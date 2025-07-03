package chaos.alice.pro.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Авто-прокрутка к последнему сообщению
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
                        // Добавляем долгое нажатие для вызова диалога переименования
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { viewModel.onRenameRequest() }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    // Аватар персонажа в тулбаре
                    uiState.currentPersona?.let { persona ->
                        AsyncImage(
                            model = persona.icon_url,
                            contentDescription = persona.name,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            )
        },
        bottomBar = {
            MessageInput(
                onSendMessage = { text -> viewModel.sendMessage(text) },
                isLoading = uiState.isLoading
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message = message)
                }
            }

            // Показываем диалог переименования, если нужно
            if (uiState.showRenameDialog) {
                RenameChatDialog(
                    currentTitle = uiState.chatTitle,
                    onConfirm = { newTitle -> viewModel.onRenameConfirm(newTitle) },
                    onDismiss = { viewModel.onRenameDialogDismiss() }
                )
            }
        }
    }
}

@Composable
fun SimpleMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    defaultColor: Color // Этот цвет будет для обычного текста
) {
    val codeBlockBackgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val markdownRegex = remember { Regex("""(\*\*.*?\*\*|\*.*?\*|`.*?`)""") }

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        markdownRegex.findAll(text).forEach { match ->
            val startIndex = match.range.first
            val matchedText = match.value

            // Обычный текст до разметки - используем defaultColor
            if (startIndex > lastIndex) {
                withStyle(style = SpanStyle(color = defaultColor)) {
                    append(text.substring(lastIndex, startIndex))
                }
            }

            // Применяем стиль к найденному элементу
            when {
                matchedText.startsWith("**") -> {
                    // Жирный текст - используем defaultColor
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                        append(matchedText.removeSurrounding("**"))
                    }
                }

                // 👇 ВОТ ЗДЕСЬ ИЗМЕНЕНИЕ
                matchedText.startsWith("*") -> {
                    // Курсивный текст - используем ItalicMessageColor
                    withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = ItalicMessageColor)) {
                        append(matchedText.removeSurrounding("*"))
                    }
                }

                matchedText.startsWith("`") -> {
                    // Код - используем defaultColor
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

        // Оставшийся обычный текст после разметки - используем defaultColor
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

@Composable
fun MessageBubble(message: MessageEntity) {
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
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            // 👇 ГЛАВНОЕ ИЗМЕНЕНИЕ - ИСПОЛЬЗУЕМ НАШ КОМПОНЕНТ
            if (message.sender == Sender.MODEL && !message.isError) {
                SimpleMarkdownText(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    defaultColor = textColor
                )
            } else {
                Text(
                    text = message.text,
                    color = textColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun MessageInput(onSendMessage: (String) -> Unit, isLoading: Boolean) {
    var text by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = {
                    onSendMessage(text)
                    text = ""
                }, enabled = text.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
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