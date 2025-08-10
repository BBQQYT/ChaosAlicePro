package chaos.alice.pro.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chaos.alice.pro.data.ChatRepository
import chaos.alice.pro.data.PersonaRepository
import chaos.alice.pro.data.TokenManager
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import chaos.alice.pro.data.local.SettingsRepository
import chaos.alice.pro.data.models.ApiProvider
import chaos.alice.pro.data.network.Persona
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<MessageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val currentPersona: Persona? = null,
    val chatTitle: String = "Загрузка...",
    val showRenameDialog: Boolean = false,
    val messageToAction: MessageEntity? = null,
    val showEditMessageDialog: Boolean = false,
    val showDeleteMessageDialog: Boolean = false,
    val selectedImageUri: Uri? = null,
    val isImagePickerEnabled: Boolean = false
)

// Снова обычный HiltViewModel
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val personaRepository: PersonaRepository,
    private val tokenManager: TokenManager,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context,
    // SavedStateHandle будет предоставлен Hilt автоматически из графа навигации
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var generationJob: Job? = null
    // chatId теперь может быть null, если мы находимся в теме ChatGPT и чат еще не выбран
    private var chatId: Long? = savedStateHandle["chatId"]

    init {
        // Мы запускаем сбор данных только если chatId был предоставлен
        chatId?.let { id ->
            loadChatData(id)
        }
    }

    // Новая функция для загрузки данных, когда chatId становится известен
    fun loadChatData(id: Long) {
        this.chatId = id
        viewModelScope.launch {
            repository.getChatHistory(id).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
        viewModelScope.launch {
            repository.getChat(id).filterNotNull().collect { chatEntity ->
                val persona = personaRepository.getPersonaById(chatEntity.personaId)
                _uiState.update { currentState ->
                    currentState.copy(
                        chatTitle = chatEntity.title,
                        currentPersona = persona
                    )
                }
            }
        }
        viewModelScope.launch {
            // Этот блок можно оставить, он не зависит от chatId
            combine(
                tokenManager.getActiveProvider(),
                settingsRepository.getModelName()
            ) { provider, modelName ->
                val model = modelName ?: ""
                provider == ApiProvider.GEMINI && !model.contains("1.5")
            }.collect { isEnabled ->
                _uiState.update { it.copy(isImagePickerEnabled = isEnabled) }
            }
        }
    }


    fun onImageSelected(uri: Uri?) { /* ... без изменений ... */ }

    fun sendMessage(text: String, imageUri: Uri?) {
        val currentChatId = chatId ?: return
        if (text.isBlank() && imageUri == null) return
        stopGeneration()
        generationJob = viewModelScope.launch {
            val userMessage = MessageEntity(
                chatId = currentChatId,
                text = text, // <--- Добавляем текст сообщения
                sender = Sender.USER, // <--- Указываем отправителя (предполагая, что это пользователь)
                timestamp = System.currentTimeMillis(), // <--- Добавляем текущее время
                // возможно, здесь также нужно будет обработать imageUri, если MessageEntity его поддерживает
                // imageUrl = imageUri?.toString() // Пример
            )
            repository.insertUserMessage(userMessage)
            _uiState.update { it.copy(isLoading = true, selectedImageUri = null) }
            try {
                repository.sendMessage(currentChatId)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }


    fun stopGeneration() { /* ... без изменений ... */ }
    fun onRenameRequest() { /* ... без изменений ... */ }
    fun onRenameDialogDismiss() { /* ... без изменений ... */ }
    fun onRenameConfirm(newTitle: String) {
        val currentChatId = chatId ?: return
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                repository.updateChatTitle(currentChatId, newTitle)
            }
        }
        onRenameDialogDismiss()
    }
    fun onMessageLongPress(message: MessageEntity) { /* ... без изменений ... */ }
    fun onEditRequest() { /* ... без изменений ... */ }
    fun onDeleteRequest() { /* ... без изменений ... */ }

    fun onConfirmEdit(newText: String) {
        val currentChatId = chatId ?: return
        stopGeneration()
        generationJob = viewModelScope.launch {
            _uiState.value.messageToAction?.let { message ->
                if (newText.isNotBlank() && newText != message.text) {
                    val forkCreated = repository.editAndFork(currentChatId, message, newText)
                    if (forkCreated) {
                        _uiState.update { it.copy(isLoading = true) }
                        try {
                            repository.sendMessage(currentChatId)
                        } finally {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                }
            }
            dismissActionDialogs()
        }
    }
    fun onConfirmDelete() { /* ... без изменений ... */ }
    fun dismissActionDialogs() { /* ... без изменений ... */ }
}