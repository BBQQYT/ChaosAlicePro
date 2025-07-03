// data/network/llm/GenericLlmApiService.kt
package chaos.alice.pro.data.network.llm

import chaos.alice.pro.data.network.llm.openai.ChatCompletionRequest
import chaos.alice.pro.data.network.llm.openai.ChatCompletionResponse
import retrofit2.Response // <-- ИМПОРТИРУЕМ Response ИЗ RETROFIT
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface GenericLlmApiService {

    @POST
    suspend fun generateChatCompletion(
        @Url url: String,
        @Header("Authorization") apiKey: String,
        // Добавим еще один хэдер, который требует OpenRouter
        @Header("HTTP-Referer") referer: String = "https://github.com/BBQQYT/CA-promt",
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse> // <-- МЕНЯЕМ ТИП НА Response<T>
}