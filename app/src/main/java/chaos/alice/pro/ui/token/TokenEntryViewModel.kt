package chaos.alice.pro.ui.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.TokenManager
import chaos.alice.pro.data.models.ApiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TokenEntryViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {

    // Обновляем функцию, чтобы она принимала и провайдера
    fun saveTokenAndProvider(token: String, provider: ApiProvider) {
        viewModelScope.launch {
            tokenManager.saveTokenAndProvider(token, provider)
        }
    }
}