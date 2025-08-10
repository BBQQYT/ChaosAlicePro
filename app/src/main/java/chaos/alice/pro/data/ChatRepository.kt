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
import chaos.alice.pro.data.TokenManager
import chaos.alice.pro.data.network.llm.GeminiLlmProvider
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


    suspend fun editMessage(messageId: Long, newText: String) {
        chatDao.updateMessageText(messageId, newText)
    }

    suspend fun deleteMessage(messageId: Long) {
        chatDao.deleteMessageById(messageId)
    }



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

    suspend fun sendMessage(chatId: Long) {
        val modelMessage = MessageEntity(
            chatId = chatId,
            text = "...",
            sender = Sender.MODEL,
            timestamp = System.currentTimeMillis() + 1
        )
        val modelMessageId = chatDao.insertMessage(modelMessage)

        try {
            val historyWithUserMessage = chatDao.getMessagesForChat(chatId).first()
            val userMessage = historyWithUserMessage.lastOrNull { it.sender == Sender.USER }
                ?: throw Exception("No user message found to respond to.")

            val chat = chatDao.getChatById(chatId) ?: throw Exception("Chat not found")
            val persona = personaRepository.getPersonaById(chat.personaId)
            val activeProvider = tokenManager.getActiveProvider().first()
            val modelName = settingsRepository.getModelName().first()
            val history = historyWithUserMessage.dropLast(1)

            val responseLength = settingsRepository.responseLength.first()
            val baseSystemPrompt = persona?.prompt ?: ""
            val lengthInstruction = when(responseLength) {
                ResponseLength.AUTO -> "\n\nТвой ответ должен адаптироваться по длине к сообщениям пользователя. Если сообщение пользователя короткое, отвечай кратко. Если длинное - отвечай развернуто."
                ResponseLength.SHORT -> "\n\nТвои ответы должны быть короткими и лаконичными."
                ResponseLength.LONG -> "\n\nТвои ответы должны быть максимально длинными, подробными и развернутыми."
            }
            val finalSystemPrompt = baseSystemPrompt + lengthInstruction

            val apiKey = when (activeProvider) {
                ApiProvider.GEMINI -> tokenManager.getGeminiKey().first()
                ApiProvider.OPEN_AI -> tokenManager.getOpenAiKey().first()
                ApiProvider.OPEN_ROUTER -> tokenManager.getOpenRouterKey().first()
            }
                ?: throw IllegalStateException("API Key for provider ${activeProvider.displayName} not found!")
            val llmProvider = llmProviderFactory.getProvider(activeProvider)

            userMessage.apiProvider = activeProvider
            userMessage.modelName = modelName

            val responseFlow = llmProvider.generateResponseStream(
                apiKey = apiKey,
                systemPrompt = finalSystemPrompt,
                history = history,
                userMessage = userMessage
            )

            var accumulatedText = ""

            responseFlow.collect { chunk ->
                accumulatedText += chunk
                chatDao.updateMessageText(modelMessageId, accumulatedText)
            }

            if (chatDao.getMessagesForChat(chatId).first().size <= 2) {
                generateAndSetChatTitle(chatId)
            }

        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message (stream)", e)
            val errorMessage = "Ошибка: ${e.message}"
            chatDao.getMessageById(modelMessageId)?.let {
                chatDao.updateMessage(
                    it.copy(
                        text = errorMessage,
                        isError = true
                    )
                )
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
                ApiProvider.GEMINI -> "gemini-1.5-flash-latest"
                ApiProvider.OPEN_AI -> "gpt-4o-mini"
                ApiProvider.OPEN_ROUTER -> "google/gemini-1.5-flash"
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