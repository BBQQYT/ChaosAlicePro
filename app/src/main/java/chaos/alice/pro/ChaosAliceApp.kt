package chaos.alice.pro

import android.app.Application
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChaosAliceApp : Application() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate() {
        super.onCreate()
        // Временный фикс для совместимости ripple с Compose BOM 2024.10.00
        ComposeFoundationFlags.isNonComposedClickableEnabled = true
    }
}
