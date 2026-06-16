package chaos.alice.pro.ui.theme.theming.gingerbread

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import chaos.alice.pro.data.local.MessageEntity
import chaos.alice.pro.data.local.Sender
import chaos.alice.pro.ui.chat.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun GingerbreadChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(uiState.messages.size - 1) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(GbBackground)) {
        GingerbreadTitleBar(
            title = uiState.chatTitle,
            onBack = { navController.navigateUp() }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = uiState.messages,
                key = { index, message ->
                    if (uiState.isLoading && index == uiState.messages.lastIndex) "${message.id}-s" else message.id
                }
            ) { index, message ->
                val isStreaming = uiState.isLoading && index == uiState.messages.lastIndex
                GingerbreadMessageRow(
                    message = message,
                    isStreaming = isStreaming,
                    onLongPress = { viewModel.onMessageLongPress(message) }
                )
            }
        }

        GingerbreadInput(
            isLoading = uiState.isLoading,
            isImagePickerEnabled = uiState.isImagePickerEnabled,
            selectedImageUri = uiState.selectedImageUri,
            onImageSelected = { viewModel.onImageSelected(it) },
            onSend = { text, uri -> viewModel.sendMessage(text, uri) },
            onStop = { viewModel.stopGeneration() }
        )
    }

    // Диалоги действий/редактирования переиспользуем из основной темы.
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GingerbreadMessageRow(
    message: MessageEntity,
    isStreaming: Boolean,
    onLongPress: () -> Unit
) {
    val isUser = message.sender == Sender.USER
    val bubbleColor = when {
        message.isError -> Color(0xFF5A1A1A)
        isUser -> Color(0xFF3A2A12)
        else -> GbBar
    }
    val borderColor = if (isUser) GbAccent else GbDivider

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(bubbleColor)
                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(8.dp)
        ) {
            message.imageUri?.let { uriString ->
                AsyncImage(
                    model = uriString.toUri(),
                    contentDescription = "Изображение",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(bottom = if (message.text.isNotBlank()) 6.dp else 0.dp)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
            if (message.text.isNotBlank() || (isStreaming && message.text.isEmpty())) {
                if (isStreaming) {
                    StreamingMessageContent(text = message.text, color = GbText)
                } else {
                    FinalMessageContent(message = message, color = GbText, modifier = Modifier)
                }
            }
        }
    }
}

@Composable
private fun GingerbreadInput(
    isLoading: Boolean,
    isImagePickerEnabled: Boolean,
    selectedImageUri: Uri?,
    onImageSelected: (Uri?) -> Unit,
    onSend: (String, Uri?) -> Unit,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> onImageSelected(uri) }
    )

    Column {
        // Оранжевая черта сверху панели ввода — в пару к панели заголовка.
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(GbBarBorder))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(GbBar)
                .navigationBarsPadding()
                .imePadding()
                .padding(8.dp)
        ) {
            if (selectedImageUri != null) {
                Box(modifier = Modifier.padding(bottom = 6.dp)) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Выбранное изображение",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color.Black)
                            .clickable { onImageSelected(null) }
                            .padding(2.dp)
                    ) {
                        Icon(Icons.Default.Close, "Убрать", tint = GbText, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isImagePickerEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { launcher.launch("image/*") }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, "Прикрепить фото", tint = GbAccent)
                    }
                }

                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    enabled = !isLoading,
                    textStyle = TextStyle(color = GbText, fontSize = 16.sp),
                    cursorBrush = SolidColor(GbAccent),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GbBackground)
                        .border(1.dp, GbDivider, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text("Сообщение…", color = GbTextDim, fontSize = 16.sp)
                        }
                        inner()
                    }
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(GbAccent)
                        .clickable {
                            if (isLoading) {
                                onStop()
                            } else if (text.isNotBlank() || selectedImageUri != null) {
                                onSend(text, selectedImageUri)
                                text = ""
                                onImageSelected(null)
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        if (isLoading) "Стоп" else "Отпр.",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
