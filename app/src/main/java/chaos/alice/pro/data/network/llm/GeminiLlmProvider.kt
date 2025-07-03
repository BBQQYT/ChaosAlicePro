package chaos.alice.pro.data.network.llm

import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.content
import javax.inject.Inject

class GeminiLlmProvider @Inject constructor() : LlmProvider {

    override suspend fun generateResponse(
        apiKey: String,
        systemPrompt: String?,
        history: List<MessageEntity>,
        userMessage: MessageEntity
    ): String {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )

        val historyForModel = mutableListOf<Content>()

        // Добавляем системный промпт, если он есть
        systemPrompt?.let {
            historyForModel.add(content(role = "user") { text(it) })
            historyForModel.add(content(role = "model") { text("Хорошо, я понял. Я буду следовать этим инструкциям.") })
        }

        // Добавляем историю чата
        history.forEach { message ->
            val role = if (message.sender == Sender.USER) "user" else "model"
            historyForModel.add(content(role = role) { text(message.text) })
        }

        val chatSession = model.startChat(history = historyForModel)
        val response = chatSession.sendMessage(userMessage.text)

        return response.text ?: throw Exception("Не удалось получить ответ от Gemini.")
    }
}