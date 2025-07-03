package chaos.alice.pro.ui.settings

import chaos.alice.pro.data.models.ApiProvider

data class SettingsUiState(
    val apiKey: String = "",
    val selectedProvider: ApiProvider = ApiProvider.OPEN_ROUTER,
    val modelName: String = "",
    val availableProviders: List<ApiProvider> = ApiProvider.values().toList(),
    val isLoading: Boolean = true,
    val availableModelsForProvider: List<String> = emptyList()
)