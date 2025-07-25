package chaos.alice.pro.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import coil.compose.AsyncImage
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch
import chaos.alice.pro.ui.theme.ItalicMessageColor
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.core.net.toUri
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
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
                title = {
                    Text(
                        text = uiState.chatTitle,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { viewModel.onRenameRequest() }
                        )
                    )
                },
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
                            modifier = Modifier.padding(end = 8.dp).size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
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
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

                    MessageBubble(
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

@Composable
fun SimpleMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    defaultColor: Color
) {
    val codeBlockBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val markdownRegex = remember { Regex("""(\*\*.*?\*\*|\*.*?\*|`.*?`)""") }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        markdownRegex.findAll(text).forEach { match ->
            val startIndex = match.range.first
            val matchedText = match.value

            if (startIndex > lastIndex) {
                withStyle(style = SpanStyle(color = defaultColor)) {
                    append(text.substring(lastIndex, startIndex))
                }
            }

            when {
                matchedText.startsWith("**") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                        append(matchedText.removeSurrounding("**"))
                    }
                }

                matchedText.startsWith("*") -> {
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = ItalicMessageColor)) {
                        append(matchedText.removeSurrounding("*"))
                    }
                }

                matchedText.startsWith("`") -> {
                    withStyle(
                        style = SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBlockBackgroundColor,
                            color = defaultColor
                        )
                    ) {
                        append(matchedText.removeSurrounding("`"))
                    }
                }
            }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < text.length) {
            withStyle(style = SpanStyle(color = defaultColor)) {
                append(text.substring(lastIndex))
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    onLongPress: () -> Unit,
    isStreaming: Boolean
) {
    val isUser = message.sender == Sender.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when {
        message.isError -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = when {
        message.isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bubbleColor,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress
                )
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
                            .clip(RoundedCornerShape(12.dp))
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
fun StreamingMessageContent(text: String, color: Color, modifier: Modifier = Modifier) {
    var displayedText by remember { mutableStateOf("") }
    val typingChannel = remember { Channel<Char>(Channel.UNLIMITED) }
    var lastProcessedText by remember { mutableStateOf("") }

    LaunchedEffect(text) {
        if (text.length > lastProcessedText.length) {
            val newChars = text.substring(lastProcessedText.length)
            newChars.forEach { typingChannel.send(it) }
            lastProcessedText = text
        }
    }

    LaunchedEffect(Unit) {
        typingChannel.receiveAsFlow().collect { char ->
            displayedText += char
            delay(35)
        }
    }


    val infiniteTransition = rememberInfiniteTransition(label = "cursor-blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor-alpha"
    )

    val animatedText = buildAnnotatedString {
        withStyle(style = SpanStyle(color = color)) {
            append(displayedText)
        }
        withStyle(style = SpanStyle(color = color.copy(alpha = cursorAlpha))) {
            append(" █")
        }
    }

    Text(
        text = animatedText,
        modifier = modifier.padding(12.dp)
    )
}

@Composable
fun FinalMessageContent(message: MessageEntity, color: Color, modifier: Modifier = Modifier) {
    if (message.sender == Sender.MODEL && !message.isError) {
        SimpleMarkdownText(
            text = message.text,
            modifier = modifier.padding(12.dp),
            defaultColor = color
        )
    } else {
        Text(
            text = message.text,
            color = color,
            modifier = modifier.padding(12.dp)
        )
    }
}


@Composable
fun MessageInput(
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
        onResult = { uri: Uri? ->
            onImageSelected(uri)
        }
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Column {
            AnimatedVisibility(visible = selectedImageUri != null) {
                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Выбранное изображение",
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { onImageSelected(null) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, "Убрать изображение")
                    }
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isImagePickerEnabled) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.AddPhotoAlternate, "Прикрепить фото")
                    }
                }

                TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Введите сообщение...") },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ),
                enabled = !isLoading
            )

            if (isLoading) {
                IconButton(onClick = onStopClicked) {
                    Icon(Icons.Default.Stop, contentDescription = "Остановить генерацию")
                }
            } else {
                IconButton(
                    onClick = {
                        onSendMessage(text, selectedImageUri)
                        text = ""
                        onImageSelected(null)
                    },
                    enabled = text.isNotBlank() || selectedImageUri != null
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Отправить")
                }
            }
            }
        }
    }
}


@Composable
fun RenameChatDialog(
    currentTitle: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTitle by remember(currentTitle) { mutableStateOf(currentTitle) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Переименовать чат") },
        text = {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("Новое название") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(newTitle) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
@Composable
fun MessageActionDialog(
    message: MessageEntity,
    onDismiss: () -> Unit,
    onEditRequest: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Действие с сообщением") },
        text = {
            Column {
                if (message.sender == Sender.USER) {
                    TextButton(onClick = { onEditRequest() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Редактировать")
                    }
                }
                TextButton(onClick = { onDeleteRequest() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Удалить")
                }
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Копировать текст")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EditMessageDialog(
    currentText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактировать сообщение") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank() && text != currentText
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun DeleteMessageDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить сообщение?") },
        text = { Text("Это действие необратимо.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
