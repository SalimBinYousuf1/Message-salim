package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "salim_settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK, SALIM
}

enum class AccentPalette(val hexColor: Long, val title: String) {
    APPLE_BLUE(0xFF007AFF, "Ocean Blue"),
    GRAPHITE_SLATE(0xFF3A3D40, "Graphite Slate"),
    EMERALD(0xFF34C759, "Emerald"),
    VIOLET(0xFF5856D6, "Deep Violet"),
    AMBER(0xFFFF9500, "Warm Amber"),
    ROSE(0xFFFF2D55, "Crimson Rose")
}

enum class AmbientBackgroundMode {
    OFF, SUBTLE, DYNAMIC
}

enum class ConversationSortOrder(val title: String) {
    RECENT("Most Recent"),
    UNREAD_FIRST("Unread First"),
    NAME_AZ("Contact Name (A–Z)")
}

data class SalimSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pureBlackOled: Boolean = true,
    val accentPalette: AccentPalette = AccentPalette.APPLE_BLUE,
    val ambientBackground: AmbientBackgroundMode = AmbientBackgroundMode.SUBTLE,
    val reducedMotion: Boolean = false,
    val reducedTransparency: Boolean = false,
    val glassOpacity: Float = 0.82f,
    val lockScreenPrivacy: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val flagSecureEnabled: Boolean = false,
    val deliverySoundsEnabled: Boolean = true,
    val defaultSubId: Int = -1,
    val sortOrder: ConversationSortOrder = ConversationSortOrder.RECENT,
    val vibrationEnabled: Boolean = true,
    val fontSizeScale: Float = 1.0f,
    val spamProtectionEnabled: Boolean = true
)

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PURE_BLACK_OLED = booleanPreferencesKey("pure_black_oled")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val AMBIENT_BG = stringPreferencesKey("ambient_bg")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val REDUCED_TRANSPARENCY = booleanPreferencesKey("reduced_transparency")
        val GLASS_OPACITY = androidx.datastore.preferences.core.floatPreferencesKey("glass_opacity")
        val LOCK_SCREEN_PRIVACY = booleanPreferencesKey("lock_screen_privacy")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val FLAG_SECURE = booleanPreferencesKey("flag_secure")
        val DELIVERY_SOUNDS = booleanPreferencesKey("delivery_sounds")
        val DEFAULT_SUB_ID = intPreferencesKey("default_sub_id")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val FONT_SIZE_SCALE = androidx.datastore.preferences.core.floatPreferencesKey("font_size_scale")
        val SPAM_PROTECTION = booleanPreferencesKey("spam_protection")
    }

    val settingsFlow: Flow<SalimSettings> = context.dataStore.data.map { preferences ->
        val themeModeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM)

        val accentStr = preferences[PreferencesKeys.ACCENT_PALETTE] ?: AccentPalette.APPLE_BLUE.name
        val accent = runCatching { AccentPalette.valueOf(accentStr) }.getOrDefault(AccentPalette.APPLE_BLUE)

        val ambientStr = preferences[PreferencesKeys.AMBIENT_BG] ?: AmbientBackgroundMode.SUBTLE.name
        val ambient = runCatching { AmbientBackgroundMode.valueOf(ambientStr) }.getOrDefault(AmbientBackgroundMode.SUBTLE)

        val sortStr = preferences[PreferencesKeys.SORT_ORDER] ?: ConversationSortOrder.RECENT.name
        val sortOrder = runCatching { ConversationSortOrder.valueOf(sortStr) }.getOrDefault(ConversationSortOrder.RECENT)

        SalimSettings(
            themeMode = themeMode,
            pureBlackOled = preferences[PreferencesKeys.PURE_BLACK_OLED] ?: true,
            accentPalette = accent,
            ambientBackground = ambient,
            reducedMotion = preferences[PreferencesKeys.REDUCED_MOTION] ?: false,
            reducedTransparency = preferences[PreferencesKeys.REDUCED_TRANSPARENCY] ?: false,
            glassOpacity = preferences[PreferencesKeys.GLASS_OPACITY] ?: 0.82f,
            lockScreenPrivacy = preferences[PreferencesKeys.LOCK_SCREEN_PRIVACY] ?: false,
            hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
            biometricLockEnabled = preferences[PreferencesKeys.BIOMETRIC_LOCK] ?: false,
            flagSecureEnabled = preferences[PreferencesKeys.FLAG_SECURE] ?: false,
            deliverySoundsEnabled = preferences[PreferencesKeys.DELIVERY_SOUNDS] ?: true,
            defaultSubId = preferences[PreferencesKeys.DEFAULT_SUB_ID] ?: -1,
            sortOrder = sortOrder,
            vibrationEnabled = preferences[PreferencesKeys.VIBRATION_ENABLED] ?: true,
            fontSizeScale = preferences[PreferencesKeys.FONT_SIZE_SCALE] ?: 1.0f,
            spamProtectionEnabled = preferences[PreferencesKeys.SPAM_PROTECTION] ?: true
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun updatePureBlackOled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PURE_BLACK_OLED] = enabled }
    }

    suspend fun updateAccent(accent: AccentPalette) {
        context.dataStore.edit { it[PreferencesKeys.ACCENT_PALETTE] = accent.name }
    }

    suspend fun updateAmbientBg(mode: AmbientBackgroundMode) {
        context.dataStore.edit { it[PreferencesKeys.AMBIENT_BG] = mode.name }
    }

    suspend fun updateReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.REDUCED_MOTION] = enabled }
    }

    suspend fun updateReducedTransparency(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.REDUCED_TRANSPARENCY] = enabled }
    }

    suspend fun updateGlassOpacity(opacity: Float) {
        context.dataStore.edit { it[PreferencesKeys.GLASS_OPACITY] = opacity.coerceIn(0.20f, 0.98f) }
    }

    suspend fun updateLockScreenPrivacy(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.LOCK_SCREEN_PRIVACY] = enabled }
    }

    suspend fun updateHaptics(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun updateBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun updateFlagSecure(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FLAG_SECURE] = enabled }
    }

    suspend fun updateDeliverySounds(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DELIVERY_SOUNDS] = enabled }
    }

    suspend fun updateSortOrder(order: ConversationSortOrder) {
        context.dataStore.edit { it[PreferencesKeys.SORT_ORDER] = order.name }
    }

    suspend fun updateVibration(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun updateDefaultSubId(subId: Int) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_SUB_ID] = subId }
    }

    suspend fun updateFontSizeScale(scale: Float) {
        context.dataStore.edit { it[PreferencesKeys.FONT_SIZE_SCALE] = scale.coerceIn(0.80f, 1.40f) }
    }

    suspend fun updateSpamProtection(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SPAM_PROTECTION] = enabled }
    }
}
