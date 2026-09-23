package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.SalimSettings
import com.example.data.preferences.ThemeMode

@Immutable
data class SalimCustomColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceTranslucent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val bubbleIncoming: Color,
    val bubbleOutgoing: Color,
    val bubbleTextIncoming: Color,
    val bubbleTextOutgoing: Color,
    val accent: Color,
    val isDark: Boolean
)

val LocalSalimColors = staticCompositionLocalOf {
    SalimCustomColors(
        background = LightBackground,
        surface = LightSurface,
        surfaceVariant = LightSurfaceVariant,
        surfaceTranslucent = LightSurfaceTranslucent,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textTertiary = LightTextTertiary,
        bubbleIncoming = LightBubbleIncoming,
        bubbleOutgoing = LightBubbleOutgoing,
        bubbleTextIncoming = LightTextPrimary,
        bubbleTextOutgoing = Color.White,
        accent = AppleBlue,
        isDark = false
    )
}

@Composable
fun SalimTheme(
    settings: SalimSettings = SalimSettings(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val accentColor = Color(settings.accentPalette.hexColor)

    val customColors = if (isDark) {
        val bg = if (settings.pureBlackOled) DarkBackground else Color(0xFF0F0F12)
        SalimCustomColors(
            background = bg,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            surfaceTranslucent = if (settings.reducedTransparency) bg else DarkSurfaceTranslucent,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            textTertiary = DarkTextTertiary,
            bubbleIncoming = DarkBubbleIncoming,
            bubbleOutgoing = accentColor,
            bubbleTextIncoming = DarkTextPrimary,
            bubbleTextOutgoing = Color.White,
            accent = accentColor,
            isDark = true
        )
    } else {
        SalimCustomColors(
            background = LightBackground,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            surfaceTranslucent = if (settings.reducedTransparency) LightBackground else LightSurfaceTranslucent,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textTertiary = LightTextTertiary,
            bubbleIncoming = LightBubbleIncoming,
            bubbleOutgoing = accentColor,
            bubbleTextIncoming = LightTextPrimary,
            bubbleTextOutgoing = Color.White,
            accent = accentColor,
            isDark = false
        )
    }

    val m3ColorScheme = if (isDark) {
        darkColorScheme(
            primary = accentColor,
            background = customColors.background,
            surface = customColors.surface,
            onPrimary = Color.White,
            onBackground = customColors.textPrimary,
            onSurface = customColors.textPrimary,
            surfaceVariant = customColors.surfaceVariant,
            onSurfaceVariant = customColors.textSecondary
        )
    } else {
        lightColorScheme(
            primary = accentColor,
            background = customColors.background,
            surface = customColors.surface,
            onPrimary = Color.White,
            onBackground = customColors.textPrimary,
            onSurface = customColors.textPrimary,
            surfaceVariant = customColors.surfaceVariant,
            onSurfaceVariant = customColors.textSecondary
        )
    }

    CompositionLocalProvider(LocalSalimColors provides customColors) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = Typography,
            content = content
        )
    }
}
