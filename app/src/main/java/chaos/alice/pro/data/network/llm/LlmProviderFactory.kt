package chaos.alice.pro.data.network.llm

import chaos.alice.pro.data.models.ApiProvider
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class LlmProviderFactory @Inject constructor(
    private val geminiProvider: Provider<GeminiLlmProvider>,
    private val openAiCompatibleProvider: Provider<OpenAiCompatibleLlmProvider>
) {
    fun getProvider(apiProvider: ApiProvider): LlmProvider {
        return when (apiProvider) {
            ApiProvider.GEMINI -> geminiProvider.get()

            ApiProvider.OPEN_AI,
            ApiProvider.OPEN_ROUTER -> openAiCompatibleProvider.get()

        }
    }
}