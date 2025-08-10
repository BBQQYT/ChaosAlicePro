package chaos.alice.pro.ui.theme.theming.chatgpt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chaos.alice.pro.ui.chat.ChatScreen
import chaos.alice.pro.ui.chat.ChatViewModel
import chaos.alice.pro.ui.chatlist.*
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatGPTContainerScreen(
    onSettingsClicked: () -> Unit,
    chatListViewModel: ChatListViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val chatListUiState by chatListViewModel.uiState.collectAsStateWithLifecycle()
    var selectedChatId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(selectedChatId) {
        selectedChatId?.let {
            chatViewModel.loadChatData(it)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(12.dp)
                ) {
                    Text("Чаты", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            chatListViewModel.onFabClicked()
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Новый чат")
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))


                    LazyColumn {
                        items(chatListUiState.chatItems, key = { it.chat.id }) { item ->
                            ListItem(
                                headlineContent = { Text(item.chat.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.clickable {
                                    selectedChatId = item.chat.id
                                    scope.launch { drawerState.close() }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                val chatUiState by chatViewModel.uiState.collectAsStateWithLifecycle()
                TopAppBar(
                    title = {
                        val title = if (selectedChatId != null) chatUiState.chatTitle else "Chaos Alice"
                        Text(text = title)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    actions = {
                        if (selectedChatId != null) {
                            chatUiState.currentPersona?.let { persona ->
                                AsyncImage(
                                    model = persona.icon_url,
                                    contentDescription = persona.name,
                                    modifier = Modifier.padding(end = 8.dp).size(40.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            IconButton(onClick = onSettingsClicked) {
                                Icon(Icons.Default.Settings, contentDescription = "Настройки")
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                if (selectedChatId == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Выберите чат или создайте новый", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    ChatScreen(
                        viewModel = chatViewModel,
                        navController = null,
                        showTopBar = false
                    )
                }
            }
        }
    }

    if (chatListUiState.showPersonaDialog) {
        PersonaSelectionDialog(
            officialPersonas = chatListUiState.officialPersonas,
            customPersonas = chatListUiState.customPersonas,
            onDismiss = { chatListViewModel.onDialogDismiss() },
            onSelect = { personaId ->
                chatListViewModel.onPersonaSelected(personaId) { newChatId ->
                    selectedChatId = newChatId
                }
            }
        )
    }

    chatListUiState.chatToDelete?.let { chatToDelete ->
        DeleteConfirmDialog(
            chatTitle = chatToDelete.chat.title,
            onConfirm = { chatListViewModel.onConfirmDelete() },
            onDismiss = { chatListViewModel.onDismissDeleteDialog() }
        )
    }
}