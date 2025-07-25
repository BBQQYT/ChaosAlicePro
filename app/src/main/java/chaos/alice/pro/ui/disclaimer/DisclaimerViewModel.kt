package chaos.alice.pro.ui.disclaimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.local.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Состояния экрана
enum class DisclaimerStep {
    AGE_CONFIRMATION,
    NON_COMMERCIAL_NOTICE,
    FINISHED
}

data class DisclaimerUiState(
    val currentStep: DisclaimerStep = DisclaimerStep.AGE_CONFIRMATION
)

@HiltViewModel
class DisclaimerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DisclaimerUiState())
    val uiState = _uiState.asStateFlow()

    fun onConfirmAge() {
        _uiState.update { it.copy(currentStep = DisclaimerStep.NON_COMMERCIAL_NOTICE) }
    }

    fun onConfirmNotice() {
        viewModelScope.launch {
            settingsRepository.markDisclaimersAsShown()
            _uiState.update { it.copy(currentStep = DisclaimerStep.FINISHED) }
        }
    }
}