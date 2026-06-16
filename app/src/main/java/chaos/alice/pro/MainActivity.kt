package chaos.alice.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import chaos.alice.pro.ui.MainApp
import chaos.alice.pro.ui.theme.ChaosAliceProTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        // Edge-to-edge: IME-инсет доставляется в Compose, чтобы imePadding() двигал
        // ввод ровно один раз (с adjustResize в манифесте), без двойного сдвига от ADJUST_PAN.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ChaosAliceProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}