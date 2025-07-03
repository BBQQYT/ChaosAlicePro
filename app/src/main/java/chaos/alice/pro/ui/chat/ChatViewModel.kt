package chaos.alice.pro.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.ChatRepository
import chaos.alice.pro.data.PersonaRepository
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.network.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ЭТО ЕДИНСТВЕННОЕ ОПРЕДЕЛЕНИЕ ЭТОГО КЛАССА В ПРОЕКТЕ
data class ChatUiState(
    val messages: List<MessageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val currentPersona: Persona? = null,
    val chatTitle: String = "Загрузка...",
    val showRenameDialog: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val personaRepository: PersonaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onRenameRequest() {
        _uiState.update { it.copy(showRenameDialog = true) }
    }

    fun onRenameDialogDismiss() {
        _uiState.update { it.copy(showRenameDialog = false) }
    }

    fun onRenameConfirm(newTitle: String) {
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                repository.updateChatTitle(chatId, newTitle)
            }
        }
        onRenameDialogDismiss()
    }
    private val chatId: Long = checkNotNull(savedStateHandle["chatId"])

    init {
        viewModelScope.launch {
            repository.getChatHistory(chatId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        viewModelScope.launch {
            repository.getChat(chatId).filterNotNull().collect { chatEntity ->
                val persona = personaRepository.getPersonaById(chatEntity.personaId)
                _uiState.update { currentState ->
                    currentState.copy(
                        chatTitle = chatEntity.title,
                        currentPersona = persona
                    )
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.sendMessage(chatId, text)
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}