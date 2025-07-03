package chaos.alice.pro.ui.token

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import chaos.alice.pro.data.models.ApiProvider

// URL для инструкции по получению токенов
private const val HELP_URL = "https://github.com/BBQQYT/CA-promt/wiki/Get-API-Token"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenEntryScreen(
    onTokenEntered: () -> Unit,
    viewModel: TokenEntryViewModel = hiltViewModel()
) {
    var token by remember { mutableStateOf("") }
    val context = LocalContext.current // Для открытия ссылки в браузере

    // --- Состояния для выпадающего меню ---
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var selectedProvider by remember { mutableStateOf(ApiProvider.GEMINI) } // Gemini по умолчанию

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Настройка API",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        // --- ВЫПАДАЮЩЕЕ МЕНЮ ДЛЯ ВЫБОРА ПРОВАЙДЕРА ---
        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
        ) {
            // Поле, которое отображает текущий выбор и открывает меню
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(), // Важный модификатор
                readOnly = true,
                value = selectedProvider.displayName,
                onValueChange = {},
                label = { Text("Выберите провайдера") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) }
            )

            // Само выпадающее меню
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                ApiProvider.values().forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider.displayName) },
                        onClick = {
                            selectedProvider = provider
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }
        Text(
            text = "Рекомендуется использовать Google Gemini для лучшей совместимости.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- ПОЛЕ ДЛЯ ВВОДА ТОКЕНА ---
        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Введите ваш API токен") },
            singleLine = true,
            trailingIcon = {
                // Иконка-кнопка для помощи
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(HELP_URL))
                    context.startActivity(intent)
                }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Помощь по получению токена"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                // Передаем и токен, и выбранный провайдер
                viewModel.saveTokenAndProvider(token, selectedProvider)
                onTokenEntered()
            },
            enabled = token.isNotBlank()
        ) {
            Text("Сохранить и войти")
        }
    }
}