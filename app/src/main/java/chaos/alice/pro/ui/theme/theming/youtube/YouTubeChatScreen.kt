package chaos.alice.pro.ui.theme.theming.youtube

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
            MediumTopAppBar(
                title = { Text(uiState.chatTitle, style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                }
            )
        },
        // Ввод сам обрабатывает нижние оконные отступы (навбар + клавиатура),
        // поэтому не даём Scaffold добавить их повторно в paddingValues.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
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
                        style = MaterialTheme.typography.titleLarge,
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
                    YouTubeCommentBubble(message, isStreaming, uiState.currentPersona?.icon_url)
                }
            }

            Surface(tonalElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                ) {
                    var text by remember { mutableStateOf("") }
                    val imagePickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent(),
                        onResult = { uri: Uri? -> viewModel.onImageSelected(uri) }
                    )

                    if (uiState.selectedImageUri != null) {
                        Box(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                            AsyncImage(
                                model = uiState.selectedImageUri,
                                contentDescription = "Выбранное изображение",
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(onClick = { viewModel.onImageSelected(null) }) {
                                Icon(Icons.Default.Close, "Убрать изображение")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.isImagePickerEnabled) {
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.AddPhotoAlternate, "Прикрепить фото")
                            }
                        }
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
                                viewModel.sendMessage(text, uiState.selectedImageUri)
                                text = ""
                                viewModel.onImageSelected(null)
                            },
                            enabled = (text.isNotBlank() || uiState.selectedImageUri != null) && !uiState.isLoading
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Отправить")
                        }
                    }
                }
            }
        }
    }

    if (uiState.messageToAction != null) { /* ... */ }
}

@Composable
fun YouTubeCommentBubble(message: MessageEntity, isStreaming: Boolean, personaIconUrl: String?) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (message.sender == Sender.MODEL) {
            AsyncImage(
                model = personaIconUrl,
                contentDescription = "Аватар Персонажа",
                contentScale = ContentScale.Crop,
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
            message.imageUri?.let { uriString ->
                AsyncImage(
                    model = uriString.toUri(),
                    contentDescription = "Прикреплённое изображение",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
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