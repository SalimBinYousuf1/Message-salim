package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SalimApplication
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.AmbientBackgroundMode
import com.example.data.preferences.SalimSettings
import com.example.data.preferences.ThemeMode
import com.example.telephony.SmsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SalimApplication
    private val preferencesRepo = app.preferencesRepository
    private val telephonyRepo = app.telephonyRepository

    val settings: StateFlow<SalimSettings> = preferencesRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SalimSettings())

    private val _isDefaultSmsApp = MutableStateFlow(SmsHelper.isDefaultSmsApp(application))
    val isDefaultSmsApp: StateFlow<Boolean> = _isDefaultSmsApp.asStateFlow()

    fun refreshDefaultSmsStatus() {
        _isDefaultSmsApp.value = SmsHelper.isDefaultSmsApp(app)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepo.updateThemeMode(mode) }
    }

    fun setPureBlackOled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.updatePureBlackOled(enabled) }
    }

    fun setAccentPalette(palette: AccentPalette) {
        viewModelScope.launch { preferencesRepo.updateAccent(palette) }
    }

    fun setAmbientBackground(mode: AmbientBackgroundMode) {
        viewModelScope.launch { preferencesRepo.updateAmbientBg(mode) }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.updateReducedMotion(enabled) }
    }

    fun setReducedTransparency(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.updateReducedTransparency(enabled) }
    }

    fun setLockScreenPrivacy(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.updateLockScreenPrivacy(enabled) }
    }

    fun setHaptics(enabled: Boolean) {
        viewModelScope.launch { preferencesRepo.updateHaptics(enabled) }
    }

    fun setDefaultSubId(subId: Int) {
        viewModelScope.launch { preferencesRepo.updateDefaultSubId(subId) }
    }

    fun resetPreferences() {
        viewModelScope.launch {
            preferencesRepo.updateThemeMode(ThemeMode.SYSTEM)
            preferencesRepo.updatePureBlackOled(true)
            preferencesRepo.updateAccent(AccentPalette.APPLE_BLUE)
            preferencesRepo.updateAmbientBg(AmbientBackgroundMode.SUBTLE)
            preferencesRepo.updateReducedMotion(false)
            preferencesRepo.updateReducedTransparency(false)
            preferencesRepo.updateLockScreenPrivacy(false)
            preferencesRepo.updateHaptics(true)
            preferencesRepo.updateDefaultSubId(-1)
        }
    }
}
