package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
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
    val searchBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val bubbleIncoming: Color,
    val bubbleOutgoing: Color,
    val bubbleTextIncoming: Color,
    val bubbleTextOutgoing: Color,
    val accent: Color,
    val divider: Color,
    val isDark: Boolean
)

val LocalSalimColors = staticCompositionLocalOf {
    SalimCustomColors(
        background = LightBackground,
        surface = LightSurface,
        surfaceVariant = LightSurfaceVariant,
        surfaceTranslucent = LightSurfaceTranslucent,
        searchBackground = LightSearchBackground,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textTertiary = LightTextTertiary,
        bubbleIncoming = LightBubbleIncoming,
        bubbleOutgoing = LightBubbleOutgoing,
        bubbleTextIncoming = LightBubbleTextIncoming,
        bubbleTextOutgoing = LightBubbleTextOutgoing,
        accent = AppleBlue,
        divider = LightDivider,
        isDark = false
    )
}

val LocalSalimThemeMode = staticCompositionLocalOf {
    ThemeMode.SYSTEM
}

@OptIn(ExperimentalMaterial3Api::class)
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
        ThemeMode.SALIM -> false
    }

    val accentColor = Color(settings.accentPalette.hexColor)

    val customColors = when (settings.themeMode) {
        ThemeMode.DARK -> {
            SalimCustomColors(
                background = DarkBackground,
                surface = DarkSurface,
                surfaceVariant = DarkSurfaceVariant,
                surfaceTranslucent = DarkSurfaceTranslucent,
                searchBackground = DarkSearchBackground,
                textPrimary = DarkTextPrimary,
                textSecondary = DarkTextSecondary,
                textTertiary = DarkTextTertiary,
                bubbleIncoming = DarkBubbleIncoming,
                bubbleOutgoing = DarkBubbleOutgoing,
                bubbleTextIncoming = DarkBubbleTextIncoming,
                bubbleTextOutgoing = DarkBubbleTextOutgoing,
                accent = accentColor,
                divider = DarkDivider,
                isDark = true
            )
        }
        ThemeMode.SALIM -> {
            SalimCustomColors(
                background = Color(0xFFFAFBFC),
                surface = Color(0xF2FFFFFF),
                surfaceVariant = Color(0xE5F2F2F7),
                surfaceTranslucent = Color(0xB8FFFFFF),
                searchBackground = Color(0xD8E5E5EA),
                textPrimary = Color(0xFF1C1C1E),
                textSecondary = Color(0xFF505054),
                textTertiary = Color(0xFF8E8E93),
                bubbleIncoming = Color(0xCCF2F2F7),
                bubbleOutgoing = Color(0xEE1C1C1E),
                bubbleTextIncoming = Color(0xFF1C1C1E),
                bubbleTextOutgoing = Color.White,
                accent = accentColor,
                divider = Color(0x33000000),
                isDark = false
            )
        }
        ThemeMode.LIGHT -> {
            SalimCustomColors(
                background = LightBackground,
                surface = LightSurface,
                surfaceVariant = LightSurfaceVariant,
                surfaceTranslucent = LightSurfaceTranslucent,
                searchBackground = LightSearchBackground,
                textPrimary = LightTextPrimary,
                textSecondary = LightTextSecondary,
                textTertiary = LightTextTertiary,
                bubbleIncoming = LightBubbleIncoming,
                bubbleOutgoing = LightBubbleOutgoing,
                bubbleTextIncoming = LightBubbleTextIncoming,
                bubbleTextOutgoing = LightBubbleTextOutgoing,
                accent = accentColor,
                divider = LightDivider,
                isDark = false
            )
        }
        ThemeMode.SYSTEM -> {
            if (systemDark) {
                SalimCustomColors(
                    background = DarkBackground,
                    surface = DarkSurface,
                    surfaceVariant = DarkSurfaceVariant,
                    surfaceTranslucent = DarkSurfaceTranslucent,
                    searchBackground = DarkSearchBackground,
                    textPrimary = DarkTextPrimary,
                    textSecondary = DarkTextSecondary,
                    textTertiary = DarkTextTertiary,
                    bubbleIncoming = DarkBubbleIncoming,
                    bubbleOutgoing = DarkBubbleOutgoing,
                    bubbleTextIncoming = DarkBubbleTextIncoming,
                    bubbleTextOutgoing = DarkBubbleTextOutgoing,
                    accent = accentColor,
                    divider = DarkDivider,
                    isDark = true
                )
            } else {
                SalimCustomColors(
                    background = LightBackground,
                    surface = LightSurface,
                    surfaceVariant = LightSurfaceVariant,
                    surfaceTranslucent = LightSurfaceTranslucent,
                    searchBackground = LightSearchBackground,
                    textPrimary = LightTextPrimary,
                    textSecondary = LightTextSecondary,
                    textTertiary = LightTextTertiary,
                    bubbleIncoming = LightBubbleIncoming,
                    bubbleOutgoing = LightBubbleOutgoing,
                    bubbleTextIncoming = LightBubbleTextIncoming,
                    bubbleTextOutgoing = LightBubbleTextOutgoing,
                    accent = accentColor,
                    divider = LightDivider,
                    isDark = false
                )
            }
        }
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

    CompositionLocalProvider(
        LocalSalimColors provides customColors,
        LocalSalimThemeMode provides settings.themeMode,
        LocalRippleConfiguration provides null // Remove Android Material wave/ripple effects in favor of Apple tactile spring press
    ) {
        MaterialTheme(
            colorScheme = m3ColorScheme,
            typography = SalimTypography,
            content = content
        )
    }
}
