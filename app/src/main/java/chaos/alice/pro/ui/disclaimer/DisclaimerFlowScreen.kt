package chaos.alice.pro.ui.disclaimer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DisclaimerFlowScreen(
    onFinished: () -> Unit, // Лямбда для перехода в основное приложение
    viewModel: DisclaimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.currentStep) {
        DisclaimerStep.AGE_CONFIRMATION -> {
            DisclaimerScreen(
                title = "Возрастное ограничение",
                text = "Это приложение предназначено для лиц старше 18 лет.",
                buttonText = "Мне есть 18 лет",
                onConfirm = { viewModel.onConfirmAge() }
            )
        }
        DisclaimerStep.NON_COMMERCIAL_NOTICE -> {
            DisclaimerScreen(
                title = "Уведомление",
                text = "Chaos Alice — это некоммерческое приложение, созданное в личных целях. Оно не связано с компаниями Яндекс, Google и т.д.",
                buttonText = "Я понимаю",
                onConfirm = { viewModel.onConfirmNotice() }
            )
        }
        DisclaimerStep.FINISHED -> {
            // Когда все шаги пройдены, вызываем onFinished для навигации
            // LaunchedEffect гарантирует, что это произойдет только один раз
            LaunchedEffect(Unit) {
                onFinished()
            }
        }
    }
}