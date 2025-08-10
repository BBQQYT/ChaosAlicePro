package chaos.alice.pro.ui.theme.theming.chatgpt

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatGPTContainerViewModel : ViewModel() {
    private val _selectedChatId = MutableStateFlow<Long?>(null)
    val selectedChatId = _selectedChatId.asStateFlow()

    fun selectChat(chatId: Long?) {
        _selectedChatId.update { chatId }
    }
}