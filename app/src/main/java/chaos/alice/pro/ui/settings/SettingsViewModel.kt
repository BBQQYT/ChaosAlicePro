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
                "google/gemini-2.0-flash-001",
                "google/gemini-2.0-flash-lite-preview-02-05:free",
                "deepseek/deepseek-r1:free",
                "deepseek/deepseek-chat:free",
                "meta-llama/llama-3.3-70b-instruct:free",
                "qwen/qwen-2.5-72b-instruct:free",
                "qwen/qwen-turbo",
                "mistralai/mistral-7b-instruct:free",
                "nvidia/llama-3.1-nemotron-70b-instruct:free",
                "microsoft/phi-3-medium-128k-instruct:free"
            ),
            ApiProvider.OPEN_AI to listOf(
                "o3-mini",
                "o1",
                "o1-mini",
                "o1-preview",
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-4-turbo",
                "gpt-3.5-turbo"
            ),
            ApiProvider.GEMINI to listOf(
                "gemini-3.1-pro-preview",
                "gemini-3.1-flash-preview",
                "gemini-3.1-flash-lite-preview",
                "gemini-2.5-pro",
                "gemini-2.5-flash",
                "gemini-2.5-flash-lite",
                "gemini-1.5-pro",
                "gemini-1.5-flash"
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