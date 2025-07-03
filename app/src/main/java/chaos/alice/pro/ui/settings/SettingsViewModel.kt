package chaos.alice.pro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.TokenManager
import chaos.alice.pro.data.models.ApiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    // 👇 НАША БАЗА ДАННЫХ МОДЕЛЕЙ 👇
    private object ModelDatabase {
        val models: Map<ApiProvider, List<String>> = mapOf(
            ApiProvider.OPEN_ROUTER to listOf(
                "deepseek/deepseek-chat", // Добавим и эту популярную
                "deepseek/deepseek-r1-0528-qwen3-8b:free",
                "deepseek/deepseek-r1-0528:free",
                "tngtech/deepseek-r1t-chimera:free",
                "nvidia/llama-3.1-nemotron-ultra-253b-v1:free",
                "deepseek/deepseek-v3-base:free",
                "google/gemini-pro-1.5", // Gemini Pro 1.5 доступен через OpenRouter
                "google/gemini-flash-1.5", // И Flash тоже
                "google/gemini-2.5-pro-exp-03-25",
                "google/gemini-2.0-flash-exp:free"
            ),
            ApiProvider.OPEN_AI to listOf(
                "gpt-4o",
                "gpt-4o-mini",
                "gpt-3.5-turbo"
            ),
            ApiProvider.GEMINI to listOf(
                "gemini-1.5-pro-latest",
                "gemini-1.5-flash-latest"
            ),
            ApiProvider.DEEPSEEK to listOf(
                "deepseek-chat",
                "deepseek-coder"
            ),
            // Для остальных пока оставим пустые списки
            ApiProvider.TOGETHER to emptyList(),
            ApiProvider.QWEN to emptyList()
        )
    }

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tokenManager.getToken(),
                tokenManager.getProvider(),
                tokenManager.getModelName()
            ) { token, provider, modelName ->
                // Получаем список моделей для текущего провайдера
                val availableModels = ModelDatabase.models[provider] ?: emptyList()

                SettingsUiState(
                    apiKey = token ?: "",
                    selectedProvider = provider,
                    // Если сохраненной модели нет, берем первую из списка или пустую строку
                    modelName = modelName.takeIf { !it.isNullOrBlank() } ?: availableModels.firstOrNull() ?: "",
                    isLoading = false,
                    // Добавляем список доступных моделей в состояние
                    availableModelsForProvider = availableModels
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onProviderSelected(provider: ApiProvider) {
        val availableModels = ModelDatabase.models[provider] ?: emptyList()
        _uiState.update {
            it.copy(
                selectedProvider = provider,
                // При смене провайдера, подставляем первую модель из списка для него
                modelName = availableModels.firstOrNull() ?: "",
                availableModelsForProvider = availableModels
            )
        }
    }

    fun onModelNameChanged(newName: String) {
        _uiState.update { it.copy(modelName = newName) }
    }

    fun onApiKeyChanged(newKey: String) {
        _uiState.update { it.copy(apiKey = newKey) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            val currentState = _uiState.value
            tokenManager.saveSettings(
                token = currentState.apiKey,
                provider = currentState.selectedProvider,
                model = currentState.modelName
            )
        }
    }

    // Вспомогательная функция, чтобы предлагать пользователю популярные модели
    private fun getDefaultModelFor(provider: ApiProvider): String {
        return when (provider) {
            ApiProvider.GEMINI -> "gemini-1.5-flash"
            ApiProvider.OPEN_AI -> "gpt-4o-mini"
            ApiProvider.OPEN_ROUTER -> "openai/gpt-3.5-turbo"
            ApiProvider.TOGETHER -> "meta-llama/Llama-3-8b-chat-hf"
            ApiProvider.DEEPSEEK -> "deepseek-chat"
            ApiProvider.QWEN -> "qwen-turbo"
        }
    }
}