package chaos.alice.pro.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.TokenManager
import chaos.alice.pro.data.local.SettingsRepository
import chaos.alice.pro.data.models.ApiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.Proxy
import javax.inject.Inject
import chaos.alice.pro.di.CheckerClient
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenManager: TokenManager,
    private val settingsRepository: SettingsRepository,
    @CheckerClient private val checkerHttpClient: OkHttpClient
) : ViewModel() {

    private object ModelDatabase {
        val models: Map<ApiProvider, List<String>> = mapOf(
            ApiProvider.OPEN_ROUTER to listOf(
                "qwen/qwen3-coder:free",
                "qwen/qwen3-235b-a22b-2507:free",
                "moonshotai/kimi-k2:free",
                "cognitivecomputations/venice-uncensored:free",
                "google/gemma-3n-2b-it:free",
                "tencent/hunyuan-a13b-instruct:free",
                "tngtech/deepseek-r1t2-chimera:free",
                "mistralai/mistral-small-3.2-24b-instruct:free",
                "moonshotai/kimi-dev-72b:free",
                "deepseek/deepseek-r1-0528-qwen3-8b:free",
                "deepseek/r1-0528:free",
                "sarvamai/sarvam-m:free",
                "mistralai/devstral-small-2505:free",
                "google/gemma-3n-4b:free",
                "qwen/qwen3-4b:free",
                "qwen/qwen3-30b-a3b:free",
                "qwen/qwen3-8b:free",
                "qwen/qwen3-14b:free",
                "qwen/qwen3-235b-a22b:free",
                "tngtech/deepseek-r1t-chimera:free",
                "microsoft/mai-ds-r1:free",
                "thudm/glm-z1-32b:free",
                "thudm/glm-4-32b:free",
                "shisa-ai/shisa-v2-llama-3.3-70b:free",
                "arliai/qwq-32b-rpr-v1:free",
                "agentica/deepcoder-14b-preview:free",
                "moonshotai/kimi-vl-a3b-thinking:free",
                "nvidia/llama-3.1-nemotron-ultra-253b-v1:free",
                "google/gemini-2.5-pro-experimental:free",
                "qwen/qwen2.5-vl-32b-instruct:free",
                "deepseek/deepseek-v3-0324:free",
                "featherless/qrwkv-72b:free",
                "mistralai/mistral-small-3.1-24b:free",
                "google/gemma-3-4b:free",
                "google/gemma-3-12b:free",
                "rekaai/reka-flash-3:free",
                "google/gemma-3-27b:free",
                "qwen/qwq-32b:free",
                "nousresearch/dephermes-3-llama-3-8b-preview:free",
                "google/gemini-2.0-flash-experimental:free",
                "meta-llama/llama-3.3-70b-instruct:free",
                "cognitivecomputations/dolphin3.0-r1-mistral-24b:free",
                "cognitivecomputations/dolphin3.0-mistral-24b:free",
                "deepseek/r1-distill-qwen-14b:free",
                "deepseek/r1-distill-llama-70b:free",
                "deepseek/r1:free",
                "mistralai/mistral-nemo:free",
                "google/gemma-2-9b:free",
                "meta-llama/llama-3.2-11b-vision-instruct:free",
                "meta-llama/llama-3.2-3b-instruct:free",
                "qwen/qwen2.5-72b-instruct:free",
                "mistralai/mistral-7b-instruct:free",
                "qwen/qwen2.5-coder-32b-instruct:free"
            ),
            ApiProvider.OPEN_AI to listOf("gpt-4o",
                "gpt-4o-mini",
                "gpt-4o",
                "o1-preview",
                "o1-mini",
                "gpt-4-turbo",
                "gpt-3.5-turbo-0125"

            ),
            ApiProvider.GEMINI to listOf(
                "gemini-1.5-flash",
                "gemini-1.5-pro",
                "gemini-2.0-flash-lite",
                "gemini-2.0-flash",
                "gemini-2.5-flash-lite",
                "gemini-2.5-flash",
                "gemini-2.5-pro"

            )
        )
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tokenManager.getActiveProvider(),
                tokenManager.getGeminiKey(),
                tokenManager.getOpenAiKey(),
                tokenManager.getOpenRouterKey(),
                settingsRepository.getModelName(),
                settingsRepository.proxySettings
            ) { values ->
                val activeProvider = values[0] as ApiProvider
                val geminiKey = values[1] as? String
                val openAiKey = values[2] as? String
                val openRouterKey = values[3] as? String
                val modelName = values[4] as? String
                val proxySettings = values[5] as SettingsRepository.ProxySettings

                val availableModels = ModelDatabase.models[activeProvider] ?: emptyList()

                SettingsUiState(
                    activeProvider = activeProvider,
                    geminiApiKey = geminiKey ?: "",
                    openAiApiKey = openAiKey ?: "",
                    openRouterApiKey = openRouterKey ?: "",
                    modelName = modelName.takeIf { !it.isNullOrBlank() } ?: availableModels.firstOrNull() ?: "",
                    availableModelsForProvider = availableModels,
                    proxyType = proxySettings.type,
                    proxyHost = proxySettings.host ?: "",
                    proxyPort = proxySettings.port?.toString() ?: "",
                    proxyUser = proxySettings.user ?: "", // <-- Новое
                    proxyPass = proxySettings.pass ?: "", // <-- Новое
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun checkProxyConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(proxyCheckStatus = ProxyCheckStatus.CHECKING) }
            val request = Request.Builder()
                .url("https://clients3.google.com/generate_204")
                .head()
                .build()

            val status = try {
                withContext(Dispatchers.IO) {
                    checkerHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) ProxyCheckStatus.SUCCESS else ProxyCheckStatus.FAILURE
                    }
                }
            } catch (e: Exception) {
                Log.e("ProxyChecker", "Proxy check failed", e)
                ProxyCheckStatus.FAILURE
            }

            _uiState.update { it.copy(proxyCheckStatus = status) }
        }
    }

    fun onProviderSelected(provider: ApiProvider) {
        val availableModels = ModelDatabase.models[provider] ?: emptyList()
        _uiState.update {
            it.copy(
                activeProvider = provider,
                modelName = availableModels.firstOrNull() ?: "",
                availableModelsForProvider = availableModels
            )
        }
    }

    fun onProxySettingsToggled() {
        _uiState.update { it.copy(isProxySettingsExpanded = !it.isProxySettingsExpanded) }
    }

    fun onGeminiKeyChanged(key: String) { _uiState.update { it.copy(geminiApiKey = key) } }
    fun onOpenAiKeyChanged(key: String) { _uiState.update { it.copy(openAiApiKey = key) } }
    fun onOpenRouterKeyChanged(key: String) { _uiState.update { it.copy(openRouterApiKey = key) } }
    fun onModelNameChanged(newName: String) { _uiState.update { it.copy(modelName = newName) } }

    fun onProxyTypeChanged(type: Proxy.Type) { _uiState.update { it.copy(proxyType = type, proxyCheckStatus = ProxyCheckStatus.IDLE) } }
    fun onProxyHostChanged(host: String) { _uiState.update { it.copy(proxyHost = host, proxyCheckStatus = ProxyCheckStatus.IDLE) } }
    fun onProxyPortChanged(port: String) { _uiState.update { it.copy(proxyPort = port.filter { c -> c.isDigit() }, proxyCheckStatus = ProxyCheckStatus.IDLE) } }
    fun onProxyUserChanged(user: String) { _uiState.update { it.copy(proxyUser = user, proxyCheckStatus = ProxyCheckStatus.IDLE) } }
    fun onProxyPassChanged(pass: String) { _uiState.update { it.copy(proxyPass = pass, proxyCheckStatus = ProxyCheckStatus.IDLE) } }


    fun saveSettings() {
        viewModelScope.launch {
            val currentState = _uiState.value

            tokenManager.saveSettings(
                activeProvider = currentState.activeProvider,
                geminiKey = currentState.geminiApiKey,
                openAiKey = currentState.openAiApiKey,
                openRouterKey = currentState.openRouterApiKey
            )
            settingsRepository.saveModelName(currentState.modelName)

            val proxySettings = SettingsRepository.ProxySettings(
                type = currentState.proxyType,
                host = currentState.proxyHost.takeIf { it.isNotBlank() },
                port = currentState.proxyPort.toIntOrNull(),
                user = currentState.proxyUser.takeIf { it.isNotBlank() },
                pass = currentState.proxyPass.takeIf { it.isNotBlank() }
            )
            settingsRepository.saveProxySettings(proxySettings)
        }
    }
}