package chaos.alice.pro.ui.theme.theming.youtube

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import chaos.alice.pro.ui.chat.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.messages.size) { /* ... без изменений ... */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.chatTitle, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Комментарии",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                itemsIndexed(
                    items = uiState.messages,
                    key = { index, message ->
                        if (uiState.isLoading && index == uiState.messages.lastIndex) "${message.id}-s" else message.id
                    }
                ) { index, message ->
                    val isStreaming = uiState.isLoading && index == uiState.messages.lastIndex
                    YouTubeCommentBubble(message, isStreaming)
                }
            }

            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var text by remember { mutableStateOf("") }
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Оставьте комментарий...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(text, null)
                            text = ""
                        },
                        enabled = text.isNotBlank() && !uiState.isLoading
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Отправить")
                    }
                }
            }
        }
    }

    if (uiState.messageToAction != null) { /* ... */ }
}

@Composable
fun YouTubeCommentBubble(message: MessageEntity, isStreaming: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (message.sender == Sender.MODEL) {
            AsyncImage(
                model = "https://yt3.googleusercontent.com/ytc/AIdro_k-3852uA1b-iCgc2h2I16b1MkP_8D5IDdeqTcS1g=s900-c-k-c0x00ffffff-no-rj",
                contentDescription = "Аватар Персонажа",
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
        } else {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Аватар Пользователя",
                modifier = Modifier.size(40.dp)
            )
        }

        Column {
            Text(
                text = if (message.sender == Sender.MODEL) "Персонаж" else "Вы",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isStreaming) {
                StreamingMessageContent(
                    text = message.text,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                FinalMessageContent(
                    message = message,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                )
            }
        }
    }
}