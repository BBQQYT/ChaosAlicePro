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

private val ExpressiveLightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFEF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFEF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

private val ExpressiveDarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
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

        appTheme == AppTheme.LEGACY -> {
            // Gingerbread-тема нарисована вручную в тёмных тонах; Material-диалоги
            // (выбор персонажа, подтверждения) держим тёмными независимо от системы.
            ExpressiveDarkColorScheme
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
        shapes = Shapes,
        content = content
    )
}