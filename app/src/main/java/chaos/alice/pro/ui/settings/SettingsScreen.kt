package chaos.alice.pro.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import chaos.alice.pro.data.models.ApiProvider
import java.net.Proxy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Настройки API", style = MaterialTheme.typography.titleLarge)
                ApiSettingsSection(uiState, viewModel)

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )

                ProxySettingsSection(uiState, viewModel)

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.checkProxyConnection() },
                        enabled = uiState.proxyType != Proxy.Type.DIRECT
                    ) {
                        Text("Проверить соединение")
                    }

                    ProxyCheckStatusIndicator(status = uiState.proxyCheckStatus)
                }




                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        viewModel.saveSettings()
                        Toast.makeText(context, "Настройки сохранены. Некоторые изменения (прокси) требуют перезапуска.", Toast.LENGTH_LONG).show()
                        navController.navigateUp()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.modelName.isNotBlank() && when (uiState.activeProvider) {
                        ApiProvider.GEMINI -> uiState.geminiApiKey.isNotBlank()
                        ApiProvider.OPEN_AI -> uiState.openAiApiKey.isNotBlank()
                        ApiProvider.OPEN_ROUTER -> uiState.openRouterApiKey.isNotBlank()
                    }
                ) {
                    Text("Сохранить и вернуться")
                }
            }
        }
    }
}


@Composable
private fun ProxyCheckStatusIndicator(status: ProxyCheckStatus) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (status) {
            ProxyCheckStatus.IDLE -> {
            }
            ProxyCheckStatus.CHECKING -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            ProxyCheckStatus.SUCCESS -> {
                Icon(Icons.Default.CheckCircle, "Успех", tint = Color.Green)
                Text("Успех", color = Color.Green, fontWeight = FontWeight.Bold)
            }
            ProxyCheckStatus.FAILURE -> {
                Icon(Icons.Default.Error, "Ошибка", tint = MaterialTheme.colorScheme.error)
                Text("Ошибка", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiSettingsSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Активный провайдер", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = isProviderDropdownExpanded,
            onExpandedChange = { isProviderDropdownExpanded = !isProviderDropdownExpanded }
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                readOnly = true,
                value = uiState.activeProvider.displayName,
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProviderDropdownExpanded) }
            )
            ExposedDropdownMenu(
                expanded = isProviderDropdownExpanded,
                onDismissRequest = { isProviderDropdownExpanded = false }
            ) {
                uiState.availableProviders.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider.displayName) },
                        onClick = {
                            viewModel.onProviderSelected(provider)
                            isProviderDropdownExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.geminiApiKey,
            onValueChange = { viewModel.onGeminiKeyChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API ключ для Google Gemini") },
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.openAiApiKey,
            onValueChange = { viewModel.onOpenAiKeyChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API ключ для OpenAI") },
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.openRouterApiKey,
            onValueChange = { viewModel.onOpenRouterKeyChanged(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API ключ для OpenRouter") },
            singleLine = true
        )

        Text("Модель для активного провайдера", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = isModelDropdownExpanded,
            onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded }
        ) {
            OutlinedTextField(
                value = uiState.modelName,
                onValueChange = { viewModel.onModelNameChanged(it) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                label = { Text("Выберите или введите ID модели") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = isModelDropdownExpanded,
                onDismissRequest = { isModelDropdownExpanded = false }
            ) {
                if (uiState.availableModelsForProvider.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Нет рекомендованных моделей") },
                        onClick = {},
                        enabled = false
                    )
                }
                uiState.availableModelsForProvider.forEach { modelName ->
                    DropdownMenuItem(
                        text = { Text(modelName) },
                        onClick = {
                            viewModel.onModelNameChanged(modelName)
                            isModelDropdownExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxySettingsSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (uiState.isProxySettingsExpanded) 180f else 0f,
        label = "proxy-arrow-rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { viewModel.onProxySettingsToggled() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Настройки прокси", style = MaterialTheme.typography.titleLarge)
        Icon(
            imageVector = Icons.Default.ExpandMore,
            contentDescription = if (uiState.isProxySettingsExpanded) "Свернуть" else "Развернуть",
            modifier = Modifier.rotate(rotationAngle)
        )
    }

    AnimatedVisibility(visible = uiState.isProxySettingsExpanded) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            var isProxyTypeExpanded by remember { mutableStateOf(false) }
            val isProxyEnabled = uiState.proxyType != Proxy.Type.DIRECT

            Spacer(modifier = Modifier.height(8.dp))

            Text("Тип прокси", style = MaterialTheme.typography.titleMedium)
            ExposedDropdownMenuBox(
                expanded = isProxyTypeExpanded,
                onExpandedChange = { isProxyTypeExpanded = !isProxyTypeExpanded }
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    readOnly = true,
                    value = uiState.proxyType.name,
                    onValueChange = {},
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProxyTypeExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = isProxyTypeExpanded,
                    onDismissRequest = { isProxyTypeExpanded = false }
                ) {
                    uiState.availableProxyTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.name) },
                            onClick = {
                                viewModel.onProxyTypeChanged(type)
                                isProxyTypeExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.proxyHost,
                onValueChange = { viewModel.onProxyHostChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Хост прокси") },
                singleLine = true,
                enabled = isProxyEnabled
            )

            OutlinedTextField(
                value = uiState.proxyPort,
                onValueChange = { viewModel.onProxyPortChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Порт прокси") },
                singleLine = true,
                enabled = isProxyEnabled,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = uiState.proxyUser,
                onValueChange = { viewModel.onProxyUserChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Логин прокси (если есть)") },
                singleLine = true,
                enabled = isProxyEnabled
            )

            OutlinedTextField(
                value = uiState.proxyPass,
                onValueChange = { viewModel.onProxyPassChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль прокси (если есть)") },
                singleLine = true,
                enabled = isProxyEnabled
            )
        }
    }
}