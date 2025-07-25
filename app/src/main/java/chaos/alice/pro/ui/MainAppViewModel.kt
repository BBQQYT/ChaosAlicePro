package chaos.alice.pro.ui

import androidx.lifecycle.ViewModel
import chaos.alice.pro.data.TokenManager
import chaos.alice.pro.data.local.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainAppViewModel @Inject constructor(
    val tokenManager: TokenManager,
    val settingsRepository: SettingsRepository
) : ViewModel()