package chaos.alice.pro.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import chaos.alice.pro.ui.navigation.AppNavGraph
import chaos.alice.pro.ui.navigation.Routes
import kotlinx.coroutines.flow.first
import chaos.alice.pro.data.models.AppTheme
import chaos.alice.pro.ui.theme.theming.ThemeManager
import chaos.alice.pro.ui.theme.ChaosAliceProTheme

private data class InitialState(
    val isLoading: Boolean = true,
    val disclaimersShown: Boolean = false,
    val appTheme: AppTheme = AppTheme.DEFAULT
)

@Composable
fun MainApp(
    viewModel: MainAppViewModel = hiltViewModel()
) {
    val initialState by produceState(initialValue = InitialState()) {
        val disclaimersShown = viewModel.settingsRepository.haveDisclaimersBeenShown.first()
        val appTheme = viewModel.settingsRepository.appTheme.first()
        value = InitialState(
            isLoading = false,
            disclaimersShown = disclaimersShown,
            appTheme = appTheme

        )
    }

    ChaosAliceProTheme(appTheme = initialState.appTheme) {
        if (initialState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val startDestination = if (!initialState.disclaimersShown) {
                Routes.DISCLAIMER
            } else {
                Routes.CHAT_LIST
            }
            val themeConfig = ThemeManager.getThemeConfig(initialState.appTheme)
            AppNavGraph(startDestination = startDestination, themeConfig = themeConfig)
        }
    }
}