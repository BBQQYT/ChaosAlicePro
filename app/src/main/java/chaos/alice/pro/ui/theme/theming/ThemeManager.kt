package chaos.alice.pro.ui.theme.theming

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import chaos.alice.pro.data.models.AppTheme
import chaos.alice.pro.ui.chat.ChatScreen
import chaos.alice.pro.ui.chatlist.ChatListScreen
import chaos.alice.pro.ui.theme.theming.telegram.TelegramChatListScreen
import chaos.alice.pro.ui.theme.theming.telegram.TelegramChatScreen
import chaos.alice.pro.ui.theme.theming.youtube.YouTubeChatListScreen
import chaos.alice.pro.ui.theme.theming.youtube.YouTubeChatScreen
import chaos.alice.pro.ui.theme.theming.chatgpt.ChatGPTContainerScreen

data class ThemeConfig(
    val chatListScreen: @Composable (onChatClicked: (Long) -> Unit, onSettingsClicked: () -> Unit) -> Unit,
    val chatScreen: @Composable (navController: NavController) -> Unit
)

object ThemeManager {
    fun getThemeConfig(theme: AppTheme): ThemeConfig {
        return when (theme) {
            AppTheme.TELEGRAM -> telegramTheme
            AppTheme.CHAT_GPT -> chatGptTheme
            AppTheme.YOUTUBE -> youTubeTheme
            else -> defaultTheme
        }
    }
}

private val defaultTheme = ThemeConfig(
    chatListScreen = { onChatClicked, onSettingsClicked ->
        ChatListScreen(onChatClicked = onChatClicked, onSettingsClicked = onSettingsClicked)
    },
    chatScreen = { navController ->

        ChatScreen(viewModel = hiltViewModel(), navController = navController)
    }
)


private val telegramTheme = ThemeConfig(
    chatListScreen = { onChatClicked, onSettingsClicked ->
        TelegramChatListScreen(onChatClicked = onChatClicked, onSettingsClicked = onSettingsClicked)
    },
    chatScreen = { navController ->
        ChatScreen(viewModel = hiltViewModel(), navController = navController)
    }
)

private val chatGptTheme = ThemeConfig(
    chatListScreen = { _, onSettingsClicked ->
        ChatGPTContainerScreen(onSettingsClicked = onSettingsClicked)
    },
    chatScreen = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
            Text("Ошибка: этот экран не должен вызываться напрямую в теме ChatGPT")
        }
    }
)

private val youTubeTheme = ThemeConfig(
    chatListScreen = { onChatClicked, onSettingsClicked ->
        YouTubeChatListScreen(onChatClicked, onSettingsClicked)
    },
    chatScreen = { navController ->
        YouTubeChatScreen(navController)
    }
)