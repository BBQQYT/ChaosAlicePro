package chaos.alice.pro.data.network.llm

import android.util.Log
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import chaos.alice.pro.data.models.ApiProvider
import chaos.alice.pro.data.network.llm.openai.ChatCompletionRequest
import chaos.alice.pro.data.network.llm.openai.ChatMessage
import chaos.alice.pro.data.network.llm.openai.ErrorResponse
import kotlinx.serialization.json.Json
import javax.inject.Inject

class OpenAiCompatibleLlmProvider @Inject constructor(
    private val apiService: GenericLlmApiService,
    private val json: Json // Внедряем Json для ручного парсинга
) : LlmProvider {

    override suspend fun generateResponse(
        apiKey: String,
        systemPrompt: String?,
        history: List<MessageEntity>,
        userMessage: MessageEntity
    ): String {
        val url = getApiUrl(userMessage.apiProvider)
        val model = userMessage.modelName
            ?: throw IllegalStateException("Model name not provided for ${userMessage.apiProvider.name}")

        val messages = mutableListOf<ChatMessage>()
        systemPrompt?.let { messages.add(ChatMessage(role = "system", content = it)) }
        history.forEach {
            val role = if (it.sender == Sender.USER) "user" else "assistant"
            messages.add(ChatMessage(role = role, content = it.text))
        }
        messages.add(ChatMessage(role = "user", content = userMessage.text))

        val request = ChatCompletionRequest(
            model = model,
            messages = messages
        )

        try {
            val response = apiService.generateChatCompletion(
                url = url,
                apiKey = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful) {
                // Успешный ответ (код 2xx)
                val body = response.body()
                return body?.choices?.firstOrNull()?.message?.content
                    ?: throw Exception("Успешный ответ, но тело или сообщение пустое.")
            } else {
                // Ответ с ошибкой (код 4xx или 5xx)
                val errorBody = response.errorBody()?.string()
                if (errorBody != null) {
                    try {
                        // Пытаемся распарсить как нашу модель ошибки
                        val errorResponse = json.decodeFromString<ErrorResponse>(errorBody)
                        throw Exception(errorResponse.error.message) // Выбрасываем понятное сообщение
                    } catch (e: Exception) {
                        // Если не удалось распарсить JSON ошибки, показываем "сырой" ответ
                        Log.e("OpenAiProvider", "Failed to parse error JSON: $errorBody", e)
                        throw Exception("Ошибка API (код ${response.code()}): $errorBody")
                    }
                } else {
                    throw Exception("Неизвестная ошибка API (код ${response.code()})")
                }
            }
        } catch (e: Exception) {
            Log.e("OpenAiProvider", "API Error for provider ${userMessage.apiProvider.name}", e)
            // Перебрасываем исключение, чтобы ChatRepository мог его поймать
            throw e
        }
    }

    private fun getApiUrl(provider: ApiProvider): String {
        // ... (этот метод без изменений)
        return when (provider) {
            ApiProvider.OPEN_AI -> "https://api.openai.com/v1/chat/completions"
            ApiProvider.OPEN_ROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            ApiProvider.TOGETHER -> "https://api.together.xyz/v1/chat/completions"
            ApiProvider.DEEPSEEK -> "https://api.deepseek.com/v1/chat/completions"
            ApiProvider.QWEN, ApiProvider.GEMINI -> throw IllegalArgumentException(
                "Provider ${provider.name} is not OpenAI-compatible and should not be handled by this class."
            )
        }
    }
}