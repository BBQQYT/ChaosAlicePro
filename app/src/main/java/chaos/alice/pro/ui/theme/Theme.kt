package chaos.alice.pro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import chaos.alice.pro.data.models.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val TelegramLightColorScheme = lightColorScheme(
    primary = TelegramBlueLight,
    primaryContainer = TelegramBubbleOutLight,
    onPrimaryContainer = Color.Black,
    secondaryContainer = TelegramSurfaceLight,
    onSecondaryContainer = Color.Black,
    background = TelegramBackgroundLight,
    surface = TelegramSurfaceLight,
    surfaceVariant = TelegramSurfaceLight
)

private val TelegramDarkColorScheme = darkColorScheme(
    primary = TelegramBlueDark,
    primaryContainer = TelegramBubbleOutDark,
    onPrimaryContainer = Color.White,
    secondaryContainer = TelegramSurfaceDark,
    onSecondaryContainer = Color.White,
    background = TelegramBackgroundDark,
    surface = TelegramSurfaceDark,
    surfaceVariant = TelegramSurfaceDark
)

private val YouTubeLightColorScheme = lightColorScheme(
    primary = YouTubeRedLight,
    background = YouTubeBackgroundLight,
    surface = YouTubeSurfaceLight,
    onSurface = YouTubeOnSurfaceLight,
    surfaceVariant = YouTubeBackgroundLight,
    onSurfaceVariant = YouTubeOnSurfaceVariantLight,
    primaryContainer = YouTubeSurfaceLight,
    onPrimaryContainer = YouTubeOnSurfaceLight,
    secondaryContainer = YouTubeSurfaceLight,
    onSecondaryContainer = YouTubeOnSurfaceLight
)

private val YouTubeDarkColorScheme = darkColorScheme(
    primary = YouTubeRedDark,
    background = YouTubeBackgroundDark,
    surface = YouTubeSurfaceDark,
    onSurface = YouTubeOnSurfaceDark,
    surfaceVariant = YouTubeSurfaceDark,
    onSurfaceVariant = YouTubeOnSurfaceVariantDark,
    primaryContainer = YouTubeSurfaceDark,
    onPrimaryContainer = YouTubeOnSurfaceDark,
    secondaryContainer = YouTubeSurfaceDark,
    onSecondaryContainer = YouTubeOnSurfaceDark
)

@Composable
fun ChaosAliceProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    appTheme: AppTheme = AppTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val colorScheme = when {

        appTheme == AppTheme.TELEGRAM -> {
            if (darkTheme) TelegramDarkColorScheme else TelegramLightColorScheme
        }

        appTheme == AppTheme.YOUTUBE -> {
            if (darkTheme) YouTubeDarkColorScheme else YouTubeLightColorScheme
        }

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}