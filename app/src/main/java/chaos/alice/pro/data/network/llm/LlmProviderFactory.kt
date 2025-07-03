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
            // Для Gemini используем его специальную реализацию
            ApiProvider.GEMINI -> geminiProvider.get()

            // Для всех, кто совместим с OpenAI, используем нашу общую реализацию
            ApiProvider.OPEN_AI,
            ApiProvider.OPEN_ROUTER,
            ApiProvider.DEEPSEEK,
            ApiProvider.TOGETHER -> openAiCompatibleProvider.get()

            // Qwen требует своей реализации. Пока ее нет, бросаем ошибку.
            ApiProvider.QWEN -> {
                throw NotImplementedError("Provider ${apiProvider.displayName} is not implemented yet.")
            }
        }
    }
}