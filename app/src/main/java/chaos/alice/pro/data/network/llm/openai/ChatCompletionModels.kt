package chaos.alice.pro.data.network.llm.openai

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)


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
    val content: String?,
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
    val code: JsonElement? = null
)


@Serializable
data class ChatCompletionChunkResponse(
    val id: String,
    val choices: List<ChunkChoice>,
    val created: Long,
    val model: String,
    @SerialName("system_fingerprint")
    val systemFingerprint: String? = null,
    val `object`: String
)

@Serializable
data class ChunkChoice(
    val delta: Delta,
    @SerialName("finish_reason")
    val finishReason: String? = null,
    val index: Int
)

@Serializable
data class Delta(
    val content: String? = null,
    val role: String? = null
)