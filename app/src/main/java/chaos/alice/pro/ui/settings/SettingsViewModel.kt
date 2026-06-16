package chaos.alice.pro.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.TokenManager
import chaos.alice.pro.data.local.SettingsRepository
import chaos.alice.pro.data.models.ApiProvider
import chaos.alice.pro.data.models.AppTheme
import chaos.alice.pro.data.models.ResponseLength
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
                "qwen/qwen3-next-80b-a3b-instruct:free",
                "openai/gpt-oss-120b:free",
                "openai/gpt-oss-20b:free",
                "nvidia/nemotron-3-ultra-550b-a55b:free",
                "nvidia/nemotron-3-super-120b-a12b:free",
                "nvidia/nemotron-3-nano-30b-a3b:free",
                "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning:free",
                "nvidia/nemotron-nano-12b-v2-vl:free",
                "nvidia/nemotron-nano-9b-v2:free",
                "nvidia/nemotron-3.5-content-safety:free",
                "nex-agi/nex-n2-pro:free",
                "nousresearch/hermes-3-llama-3.1-405b:free",
                "google/gemma-4-31b-it:free",
                "google/gemma-4-26b-a4b-it:free",
                "meta-llama/llama-3.3-70b-instruct:free",
                "meta-llama/llama-3.2-3b-instruct:free",
                "cognitivecomputations/dolphin-mistral-24b-venice-edition:free",
                "poolside/laguna-m.1:free",
                "poolside/laguna-xs.2:free",
                "liquid/lfm-2.5-1.2b-thinking:free",
                "liquid/lfm-2.5-1.2b-instruct:free"
            ),
            ApiProvider.OPEN_AI to listOf(
                "gpt-5.5",
                "gpt-5.5-pro",
                "gpt-5.4",
                "gpt-5.4-mini",
                "gpt-5.4-nano",
                "gpt-5.3-codex",
                "gpt-5.2",
                "gpt-5.1",
                "gpt-5",
                "gpt-5-mini",
                "gpt-5-nano",
                "o3-pro",
                "o3",
                "gpt-4.1",
                "gpt-4.1-mini",
                "gpt-4o-mini"
            ),
            ApiProvider.GEMINI to listOf(
                "gemini-3.5-flash",
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-lite",
                "gemini-3-flash-preview",
                "gemini-2.5-pro",
                "gemini-2.5-flash",
                "gemini-2.5-flash-lite"
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
                settingsRepository.proxySettings,
                settingsRepository.responseLength,
                settingsRepository.appTheme
            ) { values ->
                val activeProvider = values[0] as ApiProvider
                val geminiKey = values[1] as? String
                val openAiKey = values[2] as? String
                val openRouterKey = values[3] as? String
                val modelName = values[4] as? String
                val proxySettings = values[5] as SettingsRepository.ProxySettings
                val responseLength = values[6] as ResponseLength
                val appTheme = values[7] as AppTheme

                val availableModels = ModelDatabase.models[activeProvider] ?: emptyList()

                SettingsUiState(
                    activeProvider = activeProvider,
                    geminiApiKey = geminiKey ?: "",
                    openAiApiKey = openAiKey ?: "",
                    openRouterApiKey = openRouterKey ?: "",
                    modelName = modelName.takeIf { !it.isNullOrBlank() } ?: availableModels.firstOrNull() ?: "",
                    availableModelsForProvider = availableModels,
                    responseLength = responseLength,
                    appTheme = appTheme,
                    proxyType = proxySettings.type,
                    proxyHost = proxySettings.host ?: "",
                    proxyPort = proxySettings.port?.toString() ?: "",
                    proxyUser = proxySettings.user ?: "",
                    proxyPass = proxySettings.pass ?: "",
                    isLoading = false,
                    hasUnsavedChanges = false // Начальное состояние всегда false
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
                availableModelsForProvider = availableModels,
                hasUnsavedChanges = true
            )
        }
    }

    fun onAppThemeChanged(theme: AppTheme) {
        _uiState.update { it.copy(appTheme = theme, hasUnsavedChanges = true) }
    }

    fun onResponseLengthChanged(length: ResponseLength) {
        _uiState.update { it.copy(responseLength = length, hasUnsavedChanges = true) }
    }

    fun onProxySettingsToggled() {
        _uiState.update { it.copy(isProxySettingsExpanded = !it.isProxySettingsExpanded) }
    }

    fun onGeminiKeyChanged(key: String) { _uiState.update { it.copy(geminiApiKey = key, hasUnsavedChanges = true) } }
    fun onOpenAiKeyChanged(key: String) { _uiState.update { it.copy(openAiApiKey = key, hasUnsavedChanges = true) } }
    fun onOpenRouterKeyChanged(key: String) { _uiState.update { it.copy(openRouterApiKey = key, hasUnsavedChanges = true) } }
    fun onModelNameChanged(newName: String) { _uiState.update { it.copy(modelName = newName, hasUnsavedChanges = true) } }

    fun onProxyTypeChanged(type: Proxy.Type) { _uiState.update { it.copy(proxyType = type, proxyCheckStatus = ProxyCheckStatus.IDLE, hasUnsavedChanges = true) } }
    fun onProxyHostChanged(host: String) { _uiState.update { it.copy(proxyHost = host, proxyCheckStatus = ProxyCheckStatus.IDLE, hasUnsavedChanges = true) } }
    fun onProxyPortChanged(port: String) { _uiState.update { it.copy(proxyPort = port.filter { c -> c.isDigit() }, proxyCheckStatus = ProxyCheckStatus.IDLE, hasUnsavedChanges = true) } }
    fun onProxyUserChanged(user: String) { _uiState.update { it.copy(proxyUser = user, proxyCheckStatus = ProxyCheckStatus.IDLE, hasUnsavedChanges = true) } }
    fun onProxyPassChanged(pass: String) { _uiState.update { it.copy(proxyPass = pass, proxyCheckStatus = ProxyCheckStatus.IDLE, hasUnsavedChanges = true) } }

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
            settingsRepository.saveResponseLength(currentState.responseLength)
            settingsRepository.saveAppTheme(currentState.appTheme)

            val proxySettings = SettingsRepository.ProxySettings(
                type = currentState.proxyType,
                host = currentState.proxyHost.takeIf { it.isNotBlank() },
                port = currentState.proxyPort.toIntOrNull(),
                user = currentState.proxyUser.takeIf { it.isNotBlank() },
                pass = currentState.proxyPass.takeIf { it.isNotBlank() }
            )
            settingsRepository.saveProxySettings(proxySettings)

            // Сбрасываем флаг после сохранения
            _uiState.update { it.copy(hasUnsavedChanges = false) }
        }
    }
}