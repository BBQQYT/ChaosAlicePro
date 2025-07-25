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

private data class InitialState(
    val isLoading: Boolean = true,
    val disclaimersShown: Boolean = false
)

@Composable
fun MainApp(
    viewModel: MainAppViewModel = hiltViewModel()
) {
    val initialState by produceState(initialValue = InitialState()) {
        val disclaimersShown = viewModel.settingsRepository.haveDisclaimersBeenShown.first()
        value = InitialState(
            isLoading = false,
            disclaimersShown = disclaimersShown
        )
    }

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
        AppNavGraph(startDestination = startDestination)
    }
}