package chaos.alice.pro.data.network.llm.openai

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// Модель для запроса на сервер

@Serializable
data class ChatCompletionRequest(
    val model: String, // ID модели, например "gpt-4" или "openai/gpt-3.5-turbo"
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") // Можно добавить и другие параметры, например max_tokens
    val maxTokens: Int? = null,
    val stream: Boolean = false // Мы пока не будем реализовывать стриминг
)

// Модель одного сообщения в запросе
@Serializable
data class ChatMessage(
    val role: String, // "system", "user", или "assistant"
    val content: String
)

// --- Модели для ответа от сервера ---

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    @SerialName("system_fingerprint")
    val systemFingerprint: String? = null,
    val `object`: String,
    val usage: Usage
)

@Serializable
data class Choice(
    @SerialName("finish_reason")
    val finishReason: String,
    val index: Int,
    val message: ResponseMessage
)

@Serializable
data class ResponseMessage(
    val content: String?, // Ответ может быть null в редких случаях
    val role: String
)

@Serializable
data class Usage(
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

@Serializable
data class ErrorResponse(
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val message: String,
    val type: String,
    // 👇👇👇 ИСПРАВЛЕНИЕ 👇👇👇
    // Делаем поле типа JsonElement, чтобы оно принимало любой тип (строку, число, null)
    val code: JsonElement? = null
)