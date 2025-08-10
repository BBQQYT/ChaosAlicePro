package chaos.alice.pro.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.ChatRepository
import chaos.alice.pro.data.PersonaRepository
import chaos.alice.pro.data.local.ChatEntity
import chaos.alice.pro.data.network.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val chatItems: List<ChatWithPersona> = emptyList(),
    val officialPersonas: List<Persona> = emptyList(),
    val customPersonas: List<Persona> = emptyList(),
    val isLoading: Boolean = true,
    val showPersonaDialog: Boolean = false,
    val chatToDelete: ChatWithPersona? = null,
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val personaRepository: PersonaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val (official, custom) = personaRepository.getAllPersonas()
            val allPersonas = official + custom
            _uiState.update { it.copy(officialPersonas = official, customPersonas = custom) }

            chatRepository.getAllChats().collect { chats ->
                val combinedList = chats.map { chat ->
                    ChatWithPersona(
                        chat = chat,
                        persona = allPersonas.find { p -> p.id == chat.personaId }
                    )
                }
                _uiState.update {
                    it.copy(chatItems = combinedList, isLoading = false)
                }
            }
        }
    }

    fun onFabClicked() {
        _uiState.update { it.copy(showPersonaDialog = true) }
    }

    fun onDialogDismiss() {
        _uiState.update { it.copy(showPersonaDialog = false) }
    }

    fun onChatLongPressed(chatItem: ChatWithPersona) {
        _uiState.update { it.copy(chatToDelete = chatItem) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(chatToDelete = null) }
    }

    // ----- ИЗМЕНЕННАЯ ФУНКЦИЯ -----
    fun onPersonaSelected(personaId: String, onChatCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newChat = ChatEntity(
                title = "Новый чат",
                personaId = personaId
            )
            val newChatId = chatRepository.createNewChat(newChat)
            // Закрываем диалог и вызываем коллбэк с ID нового чата
            _uiState.update { it.copy(showPersonaDialog = false) }
            onChatCreated(newChatId)
        }
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            _uiState.value.chatToDelete?.let { itemToDelete ->
                chatRepository.deleteChat(itemToDelete.chat.id)
                _uiState.update { it.copy(chatToDelete = null) }
            }
        }
    }
}