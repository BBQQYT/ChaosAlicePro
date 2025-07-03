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
import kotlinx.coroutines.flow.firstOrNull
import chaos.alice.pro.data.local.SettingsRepository
import kotlinx.coroutines.flow.first

// Определим состояние загрузки
private sealed interface TokenState {
    object Loading : TokenState
    data class Loaded(val token: String?) : TokenState
}

private data class InitialState(
    val isLoading: Boolean = true,
    val disclaimersShown: Boolean = false,
    val tokenExists: Boolean = false
)

@Composable
fun MainApp(
    viewModel: MainAppViewModel = hiltViewModel()
) {
    // Загружаем все необходимые для старта данные
    val initialState by produceState(initialValue = InitialState()) {
        val disclaimersShown = viewModel.settingsRepository.haveDisclaimersBeenShown.first()
        val token = viewModel.tokenManager.getToken().first()
        value = InitialState(
            isLoading = false,
            disclaimersShown = disclaimersShown,
            tokenExists = !token.isNullOrBlank()
        )
    }

    if (initialState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Определяем, какой экран будет первым в графе навигации
        val startDestination = when {
            !initialState.disclaimersShown -> Routes.DISCLAIMER
            !initialState.tokenExists -> Routes.TOKEN_ENTRY
            else -> Routes.CHAT_LIST
        }
        AppNavGraph(startDestination = startDestination)
    }
}