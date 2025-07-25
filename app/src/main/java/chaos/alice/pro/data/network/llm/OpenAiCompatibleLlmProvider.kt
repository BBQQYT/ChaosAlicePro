package chaos.alice.pro.data.network.llm

import android.util.Log
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import chaos.alice.pro.data.models.ApiProvider
import chaos.alice.pro.data.network.llm.openai.ChatCompletionRequest
import chaos.alice.pro.data.network.llm.openai.ChatMessage
import chaos.alice.pro.data.network.llm.openai.ChatCompletionChunkResponse
import chaos.alice.pro.data.network.llm.openai.ErrorResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import okhttp3.ResponseBody
import retrofit2.HttpException

class OpenAiCompatibleLlmProvider @Inject constructor(
    private val apiService: GenericLlmApiService,
    private val json: Json
) : LlmProvider {

    override suspend fun generateResponseStream(
        apiKey: String,
        systemPrompt: String?,
        history: List<MessageEntity>,
        userMessage: MessageEntity
    ): Flow<String> {
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
            messages = messages,
            stream = true
        )

        return flow {
            try {
                val response = apiService.generateChatCompletionStream(
                    url = url,
                    apiKey = "Bearer $apiKey",
                    request = request
                )

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    throw HttpException(response)
                }

                val body = response.body() ?: throw Exception("Response body is null")

                body.source().use { source ->
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line()
                        if (line?.startsWith("data:") == true) {
                            val jsonString = line.substring(5).trim()
                            if (jsonString == "[DONE]") {
                                break
                            }
                            try {
                                val chunk = json.decodeFromString<ChatCompletionChunkResponse>(jsonString)
                                val content = chunk.choices.firstOrNull()?.delta?.content
                                if (content != null) {
                                    emit(content)
                                }
                            } catch (e: Exception) {
                                Log.e("OpenAiProvider", "Failed to parse stream chunk: $jsonString", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OpenAiProvider", "API Stream Error for provider ${userMessage.apiProvider.name}", e)
                val errorMessage = when (e) {
                    is HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        try {
                            json.decodeFromString<ErrorResponse>(errorBody!!).error.message
                        } catch (parseEx: Exception) {
                            errorBody ?: e.message()
                        }
                    }
                    else -> e.message ?: "Unknown stream error"
                }
                throw Exception("Ошибка API: $errorMessage")
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun getApiUrl(provider: ApiProvider): String {
        return when (provider) {
            ApiProvider.OPEN_AI -> "https://api.openai.com/v1/chat/completions"
            ApiProvider.OPEN_ROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            else -> throw IllegalArgumentException("Provider ${provider.name} is not supported by this class.")
        }
    }
}