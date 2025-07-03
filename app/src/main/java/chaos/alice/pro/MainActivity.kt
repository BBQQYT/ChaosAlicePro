package chaos.alice.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import chaos.alice.pro.ui.MainApp
import chaos.alice.pro.ui.theme.ChaosAliceProTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // LicenseViewModel больше не нужна здесь
    // private val licenseViewModel: LicenseViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Сплэш-скрин теперь будет скрываться сразу, без условий.
        // Можно оставить installSplashScreen() для красивого эффекта перехода.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContent {
            ChaosAliceProTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Больше нет when-блока для состояний лицензии.
                    // Просто показываем MainApp.
                    MainApp()
                }
            }
        }
    }
}