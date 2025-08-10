package chaos.alice.pro.ui.theme.theming.telegram

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import chaos.alice.pro.ui.chat.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TelegramChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.chatTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    uiState.currentPersona?.let { persona ->
                        AsyncImage(
                            model = persona.icon_url,
                            contentDescription = persona.name,
                            modifier = Modifier.padding(end = 8.dp).size(40.dp).clip(CircleShape)
                        )
                    }
                }
            )
        },
        bottomBar = {
            TelegramMessageInput(
                onSendMessage = { text, uri -> viewModel.sendMessage(text, uri) },
                isLoading = uiState.isLoading,
                onStopClicked = { viewModel.stopGeneration() },
                isImagePickerEnabled = uiState.isImagePickerEnabled,
                selectedImageUri = uiState.selectedImageUri,
                onImageSelected = { uri -> viewModel.onImageSelected(uri) }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = uiState.messages,
                    key = { index, message ->
                        if (uiState.isLoading && index == uiState.messages.lastIndex) {
                            "${message.id}-streaming"
                        } else {
                            message.id
                        }
                    }
                ) { index, message ->
                    val isStreaming = uiState.isLoading && index == uiState.messages.lastIndex
                    TelegramMessageBubble(
                        message = message,
                        onLongPress = { viewModel.onMessageLongPress(message) },
                        isStreaming = isStreaming
                    )
                }
            }
        }
    }

    if (uiState.messageToAction != null && !uiState.showEditMessageDialog && !uiState.showDeleteMessageDialog) {
        MessageActionDialog(
            message = uiState.messageToAction!!,
            onDismiss = { viewModel.dismissActionDialogs() },
            onEditRequest = { viewModel.onEditRequest() },
            onDeleteRequest = { viewModel.onDeleteRequest() }
        )
    }

    if (uiState.showEditMessageDialog && uiState.messageToAction != null) {
        EditMessageDialog(
            currentText = uiState.messageToAction!!.text,
            onDismiss = { viewModel.dismissActionDialogs() },
            onConfirm = { newText -> viewModel.onConfirmEdit(newText) }
        )
    }

    if (uiState.showDeleteMessageDialog) {
        DeleteMessageDialog(
            onDismiss = { viewModel.dismissActionDialogs() },
            onConfirm = { viewModel.onConfirmDelete() }
        )
    }

    if (uiState.showRenameDialog) {
        RenameChatDialog(
            currentTitle = uiState.chatTitle,
            onConfirm = { newTitle -> viewModel.onRenameConfirm(newTitle) },
            onDismiss = { viewModel.onRenameDialogDismiss() }
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TelegramMessageBubble(
    message: MessageEntity,
    onLongPress: () -> Unit,
    isStreaming: Boolean
) {
    val isUser = message.sender == Sender.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleShape = RoundedCornerShape(
        topStart = 12.dp,
        topEnd = 12.dp,
        bottomStart = if (isUser) 12.dp else 2.dp,
        bottomEnd = if (isUser) 2.dp else 12.dp
    )
    val bubbleColor = when {
        message.isError -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
        ) {
            Column {
                message.imageUri?.let { uriString ->
                    AsyncImage(
                        model = uriString.toUri(),
                        contentDescription = "Прикрепленное изображение",
                        modifier = Modifier
                            .padding(if (message.text.isNotBlank()) PaddingValues(4.dp) else PaddingValues(0.dp))
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    )
                }

                if (message.text.isNotBlank() || (isStreaming && message.text.isEmpty())) {
                    val textModifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp,
                        top = if (message.imageUri == null) 12.dp else 4.dp
                    )

                    if (isStreaming) {
                        StreamingMessageContent(
                            text = message.text,
                            color = textColor,
                            modifier = textModifier
                        )
                    } else {
                        FinalMessageContent(
                            message = message,
                            color = textColor,
                            modifier = textModifier
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TelegramMessageInput(
    onSendMessage: (String, Uri?) -> Unit,
    isLoading: Boolean,
    onStopClicked: () -> Unit,
    isImagePickerEnabled: Boolean,
    selectedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> onImageSelected(uri) }
    )

    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 4.dp) {
        Column {
            if (selectedImageUri != null) {
            }

            Row(
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Сообщение") },
                    shape = RoundedCornerShape(20.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    enabled = !isLoading,
                    leadingIcon = {
                        IconButton(onClick = { /* TODO: emoji picker */ }) {
                            Icon(Icons.Default.Mood, contentDescription = "Эмодзи")
                        }
                    },
                    trailingIcon = {
                        if (isImagePickerEnabled) {
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Attachment, "Прикрепить файл")
                            }
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                val fabSize = 48.dp

                FloatingActionButton(
                    onClick = {
                        if (isLoading) {
                            onStopClicked()
                        } else if (text.isNotBlank() || selectedImageUri != null) {
                            onSendMessage(text, selectedImageUri)
                            text = ""
                            onImageSelected(null)
                        }
                    },
                    modifier = Modifier.size(fabSize),
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    if (isLoading) {
                        Icon(Icons.Default.Stop, contentDescription = "Остановить")
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                    }
                }
            }
        }
    }
}