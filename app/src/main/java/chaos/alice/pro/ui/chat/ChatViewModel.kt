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


    fun onImageSelected(uri: Uri?) {
        if (uri != null) {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: SecurityException) {
                Log.e("ChatViewModel", "Failed to take persistable URI permission", e)
            }
        }
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun sendMessage(text: String, imageUri: Uri?) {
        val currentChatId = chatId ?: return
        if (text.isBlank() && imageUri == null) return
        stopGeneration()
        generationJob = viewModelScope.launch {
            val userMessage = MessageEntity(
                chatId = currentChatId,
                text = text,
                sender = Sender.USER,
                timestamp = System.currentTimeMillis(),
                imageUri = imageUri?.toString() // ← ДОБАВЬТЕ ЭТУ СТРОКУ
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


    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update { it.copy(isLoading = false) }
    }
    fun onRenameRequest() { _uiState.update { it.copy(showRenameDialog = true) } }
    fun onRenameDialogDismiss() { _uiState.update { it.copy(showRenameDialog = false) } }
    fun onRenameConfirm(newTitle: String) {
        val currentChatId = chatId ?: return
        if (newTitle.isNotBlank()) {
            viewModelScope.launch {
                repository.updateChatTitle(currentChatId, newTitle)
            }
        }
        onRenameDialogDismiss()
    }
    fun onMessageLongPress(message: MessageEntity) {
        _uiState.update { it.copy(messageToAction = message) }
    }

    fun onEditRequest() {
        _uiState.update { it.copy(showEditMessageDialog = true) }
    }

    fun onDeleteRequest() {
        _uiState.update { it.copy(showDeleteMessageDialog = true) }
    }

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
    fun onConfirmDelete() {
        viewModelScope.launch {
            _uiState.value.messageToAction?.let { message ->
                repository.deleteMessage(message.id)
            }
            dismissActionDialogs()
        }
    }
    fun dismissActionDialogs() {
        _uiState.update {
            it.copy(
                messageToAction = null,
                showEditMessageDialog = false,
                showDeleteMessageDialog = false
            )
        }
    }
}