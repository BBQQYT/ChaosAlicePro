package chaos.alice.pro.data

import android.util.Log
import chaos.alice.pro.data.local.ChatDao
import chaos.alice.pro.data.local.ChatEntity
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import chaos.alice.pro.data.local.SettingsRepository
import chaos.alice.pro.data.models.ApiProvider
import chaos.alice.pro.data.network.llm.LlmProviderFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import chaos.alice.pro.data.models.ResponseLength

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao,

    private val tokenManager: TokenManager,
    private val personaRepository: PersonaRepository,
    private val settingsRepository: SettingsRepository,
    private val llmProviderFactory: LlmProviderFactory
) {

    fun getAllChats(): Flow<List<ChatEntity>> = chatDao.getAllChats()
    fun getChat(chatId: Long): Flow<ChatEntity?> = chatDao.getChatFlow(chatId)
    suspend fun createNewChat(chat: ChatEntity): Long = chatDao.insertChat(chat)
    suspend fun updateChatTitle(chatId: Long, newTitle: String) = chatDao.updateChatTitle(chatId, newTitle)
    suspend fun deleteChat(chatId: Long) = chatDao.deleteChatAndMessages(chatId)
    fun getChatHistory(chatId: Long): Flow<List<MessageEntity>> = chatDao.getMessagesForChat(chatId)




    suspend fun editAndFork(chatId: Long, message: MessageEntity, newText: String): Boolean {
        val lastMessage = chatDao.getMessagesForChat(chatId).first().lastOrNull()

        if (lastMessage != null && lastMessage.id == message.id) {
            chatDao.updateMessageText(message.id, newText)
            return false
        } else {
            chatDao.deleteMessagesAfter(chatId, message.timestamp)
            chatDao.updateMessageText(message.id, newText)
            return true
        }
    }

    suspend fun insertUserMessage(message: MessageEntity) {
        chatDao.insertMessage(message)
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteMessageById(messageId)
    }

    suspend fun sendMessage(chatId: Long, userMessageText: String, userMessageImageUri: String?) {
        // 1. Создаем "пустое" сообщение от модели, чтобы показать индикатор загрузки
        val modelMessage = MessageEntity(
            chatId = chatId,
            text = "...",
            sender = Sender.MODEL,
            timestamp = System.currentTimeMillis() + 1 // Убедимся, что оно всегда после сообщения юзера
        )
        val modelMessageId = chatDao.insertMessage(modelMessage)

        try {
            // 2. Получаем все нужные настройки
            val chat = chatDao.getChatById(chatId) ?: throw Exception("Chat not found")
            val persona = personaRepository.getPersonaById(chat.personaId)
            val activeProvider = tokenManager.getActiveProvider().first()
            val modelName = settingsRepository.getModelName().first()
            val apiKey = when (activeProvider) {
                ApiProvider.GEMINI -> tokenManager.getGeminiKey().first()
                ApiProvider.OPEN_AI -> tokenManager.getOpenAiKey().first()
                ApiProvider.OPEN_ROUTER -> tokenManager.getOpenRouterKey().first()
            } ?: throw IllegalStateException("API Key for provider ${activeProvider.displayName} not found!")

            // 3. Формируем историю, ИСКЛЮЧАЯ последнее сообщение пользователя, которое мы передадим отдельно
            val history = chatDao.getMessagesForChat(chatId).first().dropLast(2)

            // 4. Создаем объект сообщения пользователя "на лету" со всеми данными
            val userMessage = MessageEntity(
                chatId = chatId,
                text = userMessageText,
                imageUri = userMessageImageUri,
                sender = Sender.USER,
                timestamp = System.currentTimeMillis()
            ).apply {
                this.apiProvider = activeProvider
                this.modelName = modelName
            }

            // 5. Формируем системный промпт
            val responseLength = settingsRepository.responseLength.first()
            val baseSystemPrompt = persona?.prompt ?: ""
            val lengthInstruction = when(responseLength) {
                ResponseLength.AUTO -> "\n\nТвой ответ должен адаптироваться по длине к сообщениям пользователя. Если сообщение пользователя короткое, отвечай кратко. Если длинное - отвечай развернуто."
                ResponseLength.SHORT -> "\n\nТвои ответы должны быть короткими и лаконичными."
                ResponseLength.LONG -> "\n\nТвои ответы должны быть максимально длинными, подробными и развернутыми."
            }
            val finalSystemPrompt = baseSystemPrompt + lengthInstruction

            // 6. Вызываем провайдер
            val llmProvider = llmProviderFactory.getProvider(activeProvider)
            val responseFlow = llmProvider.generateResponseStream(
                apiKey = apiKey,
                systemPrompt = finalSystemPrompt,
                history = history,
                userMessage = userMessage // Передаем наше свежесозданное сообщение
            )

            // 7. Собираем ответ
            var accumulatedText = ""
            responseFlow.collect { chunk ->
                accumulatedText += chunk
                chatDao.updateMessageText(modelMessageId, accumulatedText)
            }

            // 8. Генерируем заголовок, если это начало чата
            if (chatDao.getMessagesForChat(chatId).first().size <= 2) {
                generateAndSetChatTitle(chatId)
            }

        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message", e)
            val errorMessage = "Ошибка: ${e.message}"
            chatDao.getMessageById(modelMessageId)?.let {
                chatDao.updateMessage(it.copy(text = errorMessage, isError = true))
            }
        }
    }


    private suspend fun generateAndSetChatTitle(chatId: Long) {
        try {
            val activeProvider = tokenManager.getActiveProvider().first()
            val apiKey = when(activeProvider) {
                ApiProvider.GEMINI -> tokenManager.getGeminiKey().first()
                ApiProvider.OPEN_AI -> tokenManager.getOpenAiKey().first()
                ApiProvider.OPEN_ROUTER -> tokenManager.getOpenRouterKey().first()
            } ?: return

            val llmProvider = llmProviderFactory.getProvider(activeProvider)
            val history = getChatHistory(chatId).first()
            val conversationText = history.joinToString("\n") { "${it.sender}: ${it.text}" }

            val summarizationPrompt = """
                Summarize the following conversation with a short, descriptive title (3-5 words). The title should be in Russian.
                Conversation:
                $conversationText
                Title:
            """.trimIndent()

            val titlePromptMessage = MessageEntity(
                chatId = 0, text = summarizationPrompt, sender = Sender.USER, timestamp = 0
            )
            titlePromptMessage.apiProvider = activeProvider
            titlePromptMessage.modelName = when(activeProvider) {
                ApiProvider.GEMINI -> "gemini-2.5-flash-lite"
                ApiProvider.OPEN_AI -> "gpt-4o-mini"
                ApiProvider.OPEN_ROUTER -> "nvidia/nemotron-nano-12b-v2-vl:free" // Изменено на nvidia/nemotron-nano-12b-v2-vl:free
            }

            var titleResponse = ""
            llmProvider.generateResponseStream(
                apiKey = apiKey,
                systemPrompt = null,
                history = emptyList(),
                userMessage = titlePromptMessage
            ).collect { chunk -> titleResponse += chunk }


            val newTitle = titleResponse.trim().removeSurrounding("\"")
            if (newTitle.isNotBlank()) {
                chatDao.updateChatTitle(chatId, newTitle)
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to generate chat title", e)
        }
    }
}