package chaos.alice.pro.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки API") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Выбор провайдера (код без изменений)
                Text("Провайдер", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = isProviderDropdownExpanded,
                    onExpandedChange = { isProviderDropdownExpanded = !isProviderDropdownExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true,
                        value = uiState.selectedProvider.displayName,
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

                Text("API ключ", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = uiState.apiKey,
                    onValueChange = { viewModel.onApiKeyChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ваш API ключ") },
                    singleLine = true
                )

                // 👇👇👇 ОБНОВЛЕННЫЙ БЛОК ВЫБОРА МОДЕЛИ 👇👇👇
                Text("Модель", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = isModelDropdownExpanded,
                    onExpandedChange = { isModelDropdownExpanded = !isModelDropdownExpanded }
                ) {
                    // Текстовое поле, которое теперь можно редактировать
                    OutlinedTextField(
                        value = uiState.modelName,
                        onValueChange = { viewModel.onModelNameChanged(it) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Выберите или введите ID модели") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isModelDropdownExpanded) },
                        singleLine = true
                    )

                    // Выпадающее меню с рекомендованными моделями
                    ExposedDropdownMenu(
                        expanded = isModelDropdownExpanded,
                        onDismissRequest = { isModelDropdownExpanded = false }
                    ) {
                        // Если список пуст, показываем подсказку
                        if (uiState.availableModelsForProvider.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Для этого провайдера нет рекомендованных моделей") },
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

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        viewModel.saveSettings()
                        Toast.makeText(context, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.apiKey.isNotBlank() && uiState.modelName.isNotBlank()
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}