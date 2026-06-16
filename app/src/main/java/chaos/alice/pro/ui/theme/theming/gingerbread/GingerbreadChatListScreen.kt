package chaos.alice.pro.ui.theme.theming.gingerbread

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chaos.alice.pro.ui.chatlist.ChatListViewModel
import chaos.alice.pro.ui.chatlist.ChatWithPersona
import chaos.alice.pro.ui.chatlist.DeleteConfirmDialog
import chaos.alice.pro.ui.chatlist.PersonaSelectionSheet
import coil.compose.AsyncImage

// Палитра в духе Android 2.x Gingerbread: почти чёрный фон, оранжевый акцент.
internal val GbBackground = Color(0xFF111111)
internal val GbBar = Color(0xFF1E1E1E)
internal val GbBarBorder = Color(0xFFFF8800)
internal val GbAccent = Color(0xFFFF8800)
internal val GbText = Color(0xFFEDEDED)
internal val GbTextDim = Color(0xFF9A9A9A)
internal val GbDivider = Color(0xFF333333)

@Composable
fun GingerbreadChatListScreen(
    onChatClicked: (Long) -> Unit,
    onSettingsClicked: () -> Unit,
    chatListViewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by chatListViewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(GbBackground)) {
        GingerbreadTitleBar(
            title = "Chaos Alice",
            actions = {
                GbBarButton(text = "+ Чат", onClick = { chatListViewModel.onFabClicked() })
                GbBarIcon(icon = Icons.Default.Settings, onClick = onSettingsClicked)
            }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.isLoading && uiState.chatItems.isEmpty()) {
                CircularProgressIndicator(
                    color = GbAccent,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.chatItems.isEmpty()) {
                Text(
                    "Нет чатов. Нажми «+ Чат».",
                    color = GbTextDim,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.chatItems, key = { it.chat.id }) { item ->
                        GingerbreadListItem(
                            item = item,
                            onClick = { onChatClicked(item.chat.id) },
                            onLongClick = { chatListViewModel.onChatLongPressed(item) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showPersonaDialog) {
        PersonaSelectionSheet(
            officialPersonas = uiState.officialPersonas,
            customPersonas = uiState.customPersonas,
            onDismiss = { chatListViewModel.onDialogDismiss() },
            onSelect = { personaId ->
                chatListViewModel.onPersonaSelected(personaId, onChatCreated = onChatClicked)
            }
        )
    }
    uiState.chatToDelete?.let { chatToDelete ->
        DeleteConfirmDialog(
            chatTitle = chatToDelete.chat.title,
            onConfirm = { chatListViewModel.onConfirmDelete() },
            onDismiss = { chatListViewModel.onDismissDeleteDialog() }
        )
    }
}

// --- Общие элементы Gingerbread-темы (используются и на экране чата) ---

@Composable
internal fun GingerbreadTitleBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GbBar)
                .statusBarsPadding()
                .heightIn(min = 52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                GbBarIcon(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = title,
                color = GbText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
        // Фирменная оранжевая черта снизу панели — деталь в духе Gingerbread.
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(GbBarBorder))
    }
}

@Composable
internal fun GbBarIcon(icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = GbText)
    }
}

@Composable
internal fun GbBarButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(GbAccent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GingerbreadListItem(
    item: ChatWithPersona,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.persona?.icon_url,
                contentDescription = item.persona?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.chat.title,
                    color = GbText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    item.persona?.name ?: "Неизвестно",
                    color = GbTextDim,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(GbDivider))
    }
}
