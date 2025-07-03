package chaos.alice.pro.ui.piracy

import android.content.Context
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chaos.alice.pro.R

@Composable
fun PiracyScreen(deviceId: String) { // 👈 Убираем failCount
    val context = LocalContext.current

    // Музыка теперь играет всегда при показе этого экрана
    DisposableEffect(Unit) {
        val mediaPlayer = MediaPlayer.create(context, R.raw.app_moan).apply {
            setVolume(0.5f, 0.5f)
            start()
        }
        onDispose {
            mediaPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp) // Немного уменьшим отступы для ID
    ) {
        // Основной контент по центру
        Column(
            modifier = Modifier.align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ЛОХ",
                fontSize = 48.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ПИРАТИТЬ ПРОГИ ЭТО ПЛОХО(иногда)\nа ведь кто то пытался рисовал рисунок, а ты тут типо умный\nили у тебя нет инета",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
        }

        // 👇 ID устройства внизу экрана
        Text(
            text = "ID: $deviceId",
            modifier = Modifier.align(Alignment.BottomCenter),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace, // Моноширинный шрифт для ID
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f) // Сделаем чуть прозрачнее
        )
    }
}