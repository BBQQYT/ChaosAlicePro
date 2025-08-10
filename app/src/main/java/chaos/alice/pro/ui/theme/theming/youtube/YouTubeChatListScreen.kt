package chaos.alice.pro.ui.theme.theming.youtube

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chaos.alice.pro.ui.chatlist.*
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeChatListScreen(
    onChatClicked: (Long) -> Unit,
    onSettingsClicked: () -> Unit,
    chatListViewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by chatListViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chaos Alice") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Уже здесь */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Чаты") },
                    label = { Text("Чаты") }
                )
                NavigationBarItem(
                    selected = false,

                    onClick = { chatListViewModel.onFabClicked() },
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Новый чат") },
                    label = { Text("Новый чат") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSettingsClicked,
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                    label = { Text("Настройки") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading && uiState.chatItems.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(uiState.chatItems, key = { it.chat.id }) { chatWithPersona ->
                        YouTubeListItem(
                            item = chatWithPersona,
                            onClick = { onChatClicked(chatWithPersona.chat.id) },
                            onLongClick = { chatListViewModel.onChatLongPressed(chatWithPersona) }
                        )
                    }
                }
            }

            if (uiState.showPersonaDialog) {
                PersonaSelectionDialog(
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YouTubeListItem(item: ChatWithPersona, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        AsyncImage(
            model = item.persona?.icon_url,
            contentDescription = "Превью",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.persona?.icon_url,
                contentDescription = item.persona?.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.chat.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.persona?.name ?: "Неизвестно",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}