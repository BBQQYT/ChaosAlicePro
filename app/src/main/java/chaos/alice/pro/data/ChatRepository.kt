package chaos.alice.pro.data

import android.util.Log
import chaos.alice.pro.data.local.ChatDao
import chaos.alice.pro.data.local.ChatEntity
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import chaos.alice.pro.data.network.llm.LlmProviderFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val tokenManager: TokenManager,
    private val personaRepository: PersonaRepository,
    private val llmProviderFactory: LlmProviderFactory // 👈 Внедряем нашу фабрику
) {

    // --- Методы getAllChats, getChat, createNewChat, updateChatTitle, deleteChat остаются БЕЗ ИЗМЕНЕНИЙ ---
    fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()
    fun getChat(chatId: Long): Flow<ChatEntity?> = chatDao.getChatFlow(chatId)
    suspend fun createNewChat(chat: ChatEntity): Long = chatDao.insertChat(chat)
    suspend fun updateChatTitle(chatId: Long, newTitle: String) = chatDao.updateChatTitle(chatId, newTitle)
    suspend fun deleteChat(chatId: Long) = chatDao.deleteChatAndMessages(chatId)
    // ---

    fun getChatHistory(chatId: Long): Flow<List<MessageEntity>> {
        return chatDao.getMessagesForChat(chatId)
    }

    suspend fun sendMessage(chatId: Long, text: String) {
        val userMessage = MessageEntity(
            chatId = chatId, text = text, sender = Sender.USER, timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userMessage)

        try {
            // 1. Получаем все необходимые данные
            val chat = chatDao.getChatById(chatId) ?: throw Exception("Chat not found")
            val persona = personaRepository.getPersonaById(chat.personaId)
            val apiKey = tokenManager.getToken().first() ?: throw IllegalStateException("API Key not found!")
            val apiProviderType = tokenManager.getProvider().first()
            val modelName = tokenManager.getModelName().first() // <-- ПОЛУЧАЕМ МОДЕЛЬ
            val history = chatDao.getMessagesForChat(chatId).first().dropLast(1)

            // 2. Получаем нужный LlmProvider из фабрики
            val llmProvider = llmProviderFactory.getProvider(apiProviderType)

            // Устанавливаем провайдера и модель в userMessage
            userMessage.apiProvider = apiProviderType
            userMessage.modelName = modelName // <-- ПЕРЕДАЕМ МОДЕЛЬ

            // 3. Вызываем унифицированный метод
            val modelResponseText = llmProvider.generateResponse(
                apiKey = apiKey,
                systemPrompt = persona?.prompt,
                history = history,
                userMessage = userMessage
            )

            // 4. Сохраняем ответ модели
            val modelMessage = MessageEntity(
                chatId = chatId, text = modelResponseText, sender = Sender.MODEL, timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(modelMessage)

            // 5. Генерируем название чата, если это начало диалога
            if (chatDao.getMessagesForChat(chatId).first().size <= 2) {
                generateAndSetChatTitle(chatId) // Передаем ответ для контекста
            }

        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            val errorMessage = MessageEntity(
                chatId = chatId, text = "Ошибка: ${e.message}", sender = Sender.MODEL, timestamp = System.currentTimeMillis(), isError = true
            )
            chatDao.insertMessage(errorMessage)
        }
    }

    // --- Метод генерации заголовка тоже нужно немного адаптировать ---
    private suspend fun generateAndSetChatTitle(chatId: Long) {
        try {
            val apiKey = tokenManager.getToken().first() ?: return
            val apiProviderType = tokenManager.getProvider().first()
            val llmProvider = llmProviderFactory.getProvider(apiProviderType)

            val history = getChatHistory(chatId).first()
            val conversationText = history.joinToString("\n") { "${it.sender}: ${it.text}" }

            val summarizationPrompt = """
                Summarize the following conversation with a short, descriptive title (3-5 words). The title should be in Russian.
                Conversation:
                $conversationText
                Title:
            """.trimIndent()

            // 👇👇👇 ВОТ ИСПРАВЛЕНИЕ 👇👇👇

            // 1. Создаем сообщение с промптом как отдельную переменную
            val titlePromptMessage = MessageEntity(
                chatId = 0, // Не важен для этого запроса
                text = summarizationPrompt,
                sender = Sender.USER,
                timestamp = 0
            )

            // 2. Устанавливаем для этого сообщения тип провайдера
            titlePromptMessage.apiProvider = apiProviderType

            // 3. Передаем готовую переменную в метод
            val titleResponse = llmProvider.generateResponse(
                apiKey = apiKey,
                systemPrompt = null, // Для заголовка системный промпт не нужен
                history = emptyList(),
                userMessage = titlePromptMessage // Используем нашу переменную
            )

            val newTitle = titleResponse.trim().removeSurrounding("\"")
            if (newTitle.isNotBlank()) {
                chatDao.updateChatTitle(chatId, newTitle)
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to generate chat title", e)
        }
    }
}