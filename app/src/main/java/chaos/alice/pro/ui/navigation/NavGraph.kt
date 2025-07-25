package chaos.alice.pro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import chaos.alice.pro.ui.chat.ChatScreen
import chaos.alice.pro.ui.chatlist.ChatListScreen
import chaos.alice.pro.ui.settings.SettingsScreen
import chaos.alice.pro.ui.disclaimer.DisclaimerFlowScreen

object Routes {
    const val DISCLAIMER = "disclaimer"
    const val CHAT_LIST = "chat_list"
    const val CHAT = "chat/{chatId}"
    const val SETTINGS = "settings"

    fun chat(chatId: Long): String {
        return "chat/$chatId"
    }
}

@Composable
fun AppNavGraph(startDestination: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.DISCLAIMER) {
            DisclaimerFlowScreen(onFinished = {
                navController.navigate(Routes.CHAT_LIST) {
                    popUpTo(Routes.DISCLAIMER) { inclusive = true }
                }
            })
        }

        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onChatClicked = { chatId ->
                    navController.navigate(Routes.chat(chatId))
                },
                onSettingsClicked = {
                    navController.navigate(Routes.SETTINGS)
                }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("chatId") { type = NavType.LongType })
        ) {
            ChatScreen(navController = navController)
        }
    }
}