package chaos.alice.pro.ui.theme.theming.telegram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chaos.alice.pro.ui.chatlist.ChatListViewModel
import chaos.alice.pro.ui.chatlist.ChatListUiState
import chaos.alice.pro.ui.chatlist.ChatCardItem
import chaos.alice.pro.ui.chatlist.DeleteConfirmDialog
import chaos.alice.pro.ui.chatlist.PersonaSelectionDialog
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramChatListScreen(
    onChatClicked: (Long) -> Unit,
    onSettingsClicked: () -> Unit,
    chatListViewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by chatListViewModel.uiState.collectAsStateWithLifecycle()



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chaos Alice") },
                actions = {
                    IconButton(onClick = onSettingsClicked) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { chatListViewModel.onFabClicked() }) {
                Icon(Icons.Default.Edit, contentDescription = "Новый чат")
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading && uiState.chatItems.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.chatItems.isEmpty()) {
                Text(
                    text = "Пока нет ни одного чата.",
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