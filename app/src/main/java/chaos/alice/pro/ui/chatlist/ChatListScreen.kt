package chaos.alice.pro.ui.chatlist

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chaos.alice.pro.data.network.Persona
import coil.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    onChatClicked: (Long) -> Unit,
    onSettingsClicked: () -> Unit,
    chatListViewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by chatListViewModel.uiState.collectAsStateWithLifecycle()

    MainChatListContent(
        uiState = uiState,
        onChatClicked = onChatClicked,
        onSettingsClicked = onSettingsClicked,
        chatListViewModel = chatListViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainChatListContent(
    uiState: ChatListUiState,
    onChatClicked: (Long) -> Unit,
    onSettingsClicked: () -> Unit,
    chatListViewModel: ChatListViewModel
) {
    val context = LocalContext.current
    val creatorUrl = "https://vercel-character-creator.vercel.app"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Чаты") },
                actions = {
                    IconButton(onClick = onSettingsClicked) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FloatingActionButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(creatorUrl))
                        context.startActivity(intent)
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Создать персонажа")
                }
                FloatingActionButton(onClick = { chatListViewModel.onFabClicked() }) {
                    Icon(Icons.Default.Add, contentDescription = "Новый чат")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading && uiState.chatItems.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.chatItems.isEmpty()) {
                Text(
                    text = "Пока нет ни одного чата.\nНажмите '+' чтобы начать.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.chatItems) { chatWithPersona ->
                        ChatCardItem(
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


@Composable
fun DeleteConfirmDialog(
    chatTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = "Внимание") },
        title = { Text("Удалить чат?") },
        text = {
            Text("Вы уверены, что хотите навсегда удалить чат \"$chatTitle\" и все его сообщения? Это действие необратимо.")
        },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatCardItem(
    item: ChatWithPersona,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.persona?.icon_url,
                contentDescription = item.persona?.name,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.chat.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Персонаж: ${item.persona?.name ?: "Неизвестно"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonaSelectionDialog(
    officialPersonas: List<Persona>,
    customPersonas: List<Persona>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите персонажа") },
        text = {
            LazyColumn {
                if (officialPersonas.isNotEmpty()) {
                    stickyHeader {
                        Surface(modifier = Modifier.fillParentMaxWidth()) {
                            Text("Официальные", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(8.dp))
                        }
                    }
                    items(officialPersonas) { persona ->
                        PersonaListItem(persona = persona, onSelect = onSelect)
                    }
                }

                if (customPersonas.isNotEmpty()) {
                    stickyHeader {
                        Surface(modifier = Modifier.fillParentMaxWidth()) {
                            Text("Пользовательские", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(8.dp))
                        }
                    }
                    items(customPersonas) { persona ->
                        PersonaListItem(persona = persona, onSelect = onSelect)
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun PersonaListItem(persona: Persona, onSelect: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(persona.name) },
        supportingContent = { Text(persona.description) },
        leadingContent = {
            AsyncImage(
                model = persona.icon_url,
                contentDescription = persona.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        modifier = Modifier.clickable { onSelect(persona.id) }
    )
}