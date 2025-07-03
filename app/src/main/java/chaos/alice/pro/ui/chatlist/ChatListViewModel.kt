package chaos.alice.pro.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.ChatRepository
import chaos.alice.pro.data.PersonaRepository
import chaos.alice.pro.data.local.ChatEntity
import chaos.alice.pro.data.network.Persona
import chaos.alice.pro.di.LicenseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Шаг 1: Очищаем и упрощаем UiState
data class ChatListUiState(
    val chatItems: List<ChatWithPersona> = emptyList(),
    val officialPersonas: List<Persona> = emptyList(),
    val customPersonas: List<Persona> = emptyList(),
    val isLoading: Boolean = true,
    val showPersonaDialog: Boolean = false,
    val chatToDelete: ChatWithPersona? = null,
    val licenseState: LicenseState? = null // null - проверка не завершена или скрыта
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val personaRepository: PersonaRepository
    // УБРАЛИ LicenseViewModel ОТСЮДА
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Логика загрузки чатов и персонажей остается
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
        // Логика подписки на licenseState отсюда УДАЛЕНА
    }


    // --- Методы для управления лицензией ---
    fun updateLicenseState(newState: LicenseState) {
        // Не показываем состояние Loading, чтобы не отвлекать пользователя
        if (newState !is LicenseState.Loading) {
            _uiState.update { it.copy(licenseState = newState) }
        }
    }

    fun dismissLicenseError() {
        _uiState.update { it.copy(licenseState = null) }
    }

    // --- Методы для управления диалогами и чатами ---
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

    // --- Методы, выполняющие действия с репозиториями ---
    fun onPersonaSelected(personaId: String, onChatCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newChat = ChatEntity(
                title = "Новый чат", // Название потом сгенерирует ИИ
                personaId = personaId
            )
            val newChatId = chatRepository.createNewChat(newChat)
            _uiState.update { it.copy(showPersonaDialog = false) } // Закрываем диалог после создания
            onChatCreated(newChatId) // Передаем ID для навигации
        }
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            _uiState.value.chatToDelete?.let { itemToDelete ->
                chatRepository.deleteChat(itemToDelete.chat.id)
                // Сбрасываем чат для удаления, чтобы диалог закрылся
                _uiState.update { it.copy(chatToDelete = null) }
            }
        }
    }
}