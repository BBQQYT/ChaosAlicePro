
package chaos.alice.pro.data.network.llm

import chaos.alice.pro.data.network.llm.openai.ChatCompletionRequest
import chaos.alice.pro.data.network.llm.openai.ChatCompletionResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

interface GenericLlmApiService {

    @POST
    suspend fun generateChatCompletion(
        @Url url: String,
        @Header("Authorization") apiKey: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/BBQQYT/CA-promt",
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>


    @Streaming
    @POST
    suspend fun generateChatCompletionStream(
        @Url url: String,
        @Header("Authorization") apiKey: String,
        @Header("HTTP-Referer") referer: String = "https://github.com/BBQQYT/CA-promt",
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>
}