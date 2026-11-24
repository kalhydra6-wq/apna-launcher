package com.apnalauncher.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import com.apnalauncher.data.LauncherSettings

val LocalLauncherSettings = compositionLocalOf { LauncherSettings() }

/** Material 3 theme with dynamic color, accent, built-in presets and fonts. */
@Composable
fun ApnaTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val base = if (settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = androidx.compose.ui.platform.LocalContext.current
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (dark) darkColorScheme() else lightColorScheme()

    val scheme = when (settings.builtInTheme) {
        "amoled" -> base.copy(
            background = Color.Black, surface = Color.Black,
            surfaceVariant = Color(0xFF101010), onBackground = Color.White,
        )
        "neon" -> base.copy(primary = Color(0xFFFF2D95), secondary = Color(0xFF00E5FF))
        "glass" -> base.copy(surface = base.surface.copy(alpha = 0.55f))
        "minimal" -> base.copy(primary = Color(0xFF444444), secondary = Color(0xFF777777))
        else -> base
    }.copy(primary = Color(settings.accentColor))

    val fonts = when (settings.fontChoice) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    val typography = Typography(
        bodyLarge = Typography().bodyLarge.copy(fontFamily = fonts),
        titleLarge = Typography().titleLarge.copy(fontFamily = fonts),
        labelMedium = Typography().labelMedium.copy(fontFamily = fonts),
    )

    CompositionLocalProvider(LocalLauncherSettings provides settings) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
