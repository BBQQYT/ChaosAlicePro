package chaos.alice.pro.ui.settings

import chaos.alice.pro.data.models.ApiProvider
import java.net.Proxy
import chaos.alice.pro.data.models.ResponseLength
import chaos.alice.pro.data.models.AppTheme

data class SettingsUiState(

    val proxyCheckStatus: ProxyCheckStatus = ProxyCheckStatus.IDLE,
    val activeProvider: ApiProvider = ApiProvider.GEMINI,
    val geminiApiKey: String = "",
    val openAiApiKey: String = "",
    val openRouterApiKey: String = "",
    val modelName: String = "",
    val availableProviders: List<ApiProvider> = ApiProvider.entries,
    val availableModelsForProvider: List<String> = emptyList(),
    val responseLength: ResponseLength = ResponseLength.AUTO,
    val appTheme: AppTheme = AppTheme.DEFAULT,

    val proxyType: Proxy.Type = Proxy.Type.DIRECT,
    val proxyHost: String = "",
    val proxyPort: String = "",
    val proxyUser: String = "",
    val proxyPass: String = "",
    val availableProxyTypes: List<Proxy.Type> = Proxy.Type.entries,

    val isLoading: Boolean = true,
    val isProxySettingsExpanded: Boolean = false,
    val hasUnsavedChanges: Boolean = false


)

enum class ProxyCheckStatus {
    IDLE,
    CHECKING,
    SUCCESS,
    FAILURE
}