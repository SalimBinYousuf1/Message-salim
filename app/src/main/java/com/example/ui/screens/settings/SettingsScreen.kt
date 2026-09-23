package com.example.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.AmbientBackgroundMode
import com.example.data.preferences.ThemeMode
import com.example.telephony.SmsHelper
import com.example.ui.components.AgslAmbientBackground
import com.example.ui.components.SalimDetailTopBar
import com.example.ui.components.SquircleCardShape
import com.example.ui.theme.LocalSalimColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDefaultSms by viewModel.isDefaultSmsApp.collectAsStateWithLifecycle()
    val colors = LocalSalimColors.current
    val context = LocalContext.current

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshDefaultSmsStatus()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDefaultSmsStatus()
    }

    var showResetDialog by remember { mutableStateOf(false) }

    AgslAmbientBackground(
        mode = settings.ambientBackground,
        reducedMotion = settings.reducedMotion,
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                SalimDetailTopBar(
                    title = "Settings",
                    onBackClick = onBackClick
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                // Default SMS App Card
                item {
                    SettingsSectionTitle("SYSTEM ROLE")
                    SettingsGroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDefaultSms) colors.accent.copy(alpha = 0.15f) else Color(0xFFFF9500).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = if (isDefaultSms) colors.accent else Color(0xFFFF9500),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Default SMS App",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (isDefaultSms) "Salim is handling SMS & MMS" else "Tap to set Salim as default",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDefaultSms) Color(0xFF34C759) else Color(0xFFFF9500)
                                )
                            }

                            if (!isDefaultSms) {
                                Button(
                                    onClick = {
                                        val intent = SmsHelper.getRequestDefaultSmsAppIntent(context)
                                        if (intent != null) {
                                            roleLauncher.launch(intent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Set Default", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // Appearance Section
                item {
                    SettingsSectionTitle("APPEARANCE")
                    SettingsGroupCard {
                        // Theme Mode Selector
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            ThemeModeSegmentedControl(
                                currentMode = settings.themeMode,
                                onModeSelected = { viewModel.setThemeMode(it) }
                            )
                        }

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // True-black OLED Dark Mode toggle
                        SettingsToggleRow(
                            title = "True Black OLED",
                            subtitle = "Independently tuned pitch-black background for OLED displays",
                            checked = settings.pureBlackOled,
                            onCheckedChange = { viewModel.setPureBlackOled(it) }
                        )

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Accent Palette Swatches
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Accent Color",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AccentPalette.values().forEach { palette ->
                                    val isSelected = settings.accentPalette == palette
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(palette.hexColor))
                                            .clickable { viewModel.setAccentPalette(palette) }
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) colors.textPrimary else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = palette.title,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // AGSL Ambient Background Mode
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Ambient ASGL Background",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Real GPU RuntimeShader with organic multi-octave FBM fluid motion",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                            )
                            AmbientBgSegmentedControl(
                                currentMode = settings.ambientBackground,
                                onModeSelected = { viewModel.setAmbientBackground(it) }
                            )
                        }

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Reduced Transparency
                        SettingsToggleRow(
                            title = "Reduce Transparency",
                            subtitle = "Use solid surfaces instead of liquid translucent material",
                            checked = settings.reducedTransparency,
                            onCheckedChange = { viewModel.setReducedTransparency(it) }
                        )

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Reduced Motion
                        SettingsToggleRow(
                            title = "Reduce Motion",
                            subtitle = "Pause background GPU shader animations and soften transitions",
                            checked = settings.reducedMotion,
                            onCheckedChange = { viewModel.setReducedMotion(it) }
                        )
                    }
                }

                // Privacy & Notifications Section
                item {
                    SettingsSectionTitle("NOTIFICATIONS & PRIVACY")
                    SettingsGroupCard {
                        SettingsToggleRow(
                            title = "Lock Screen Privacy",
                            subtitle = "Hide message preview and body on lock screen notifications",
                            checked = settings.lockScreenPrivacy,
                            onCheckedChange = { viewModel.setLockScreenPrivacy(it) }
                        )

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsToggleRow(
                            title = "Haptic Feedback",
                            subtitle = "Sensory feedback when sending messages and long-pressing items",
                            checked = settings.hapticsEnabled,
                            onCheckedChange = { viewModel.setHaptics(it) }
                        )
                    }
                }

                // About Section
                item {
                    SettingsSectionTitle("ABOUT")
                    SettingsGroupCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Salim Messages",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Production Android SMS & MMS App\nArchitecture: Jetpack Compose + Room + Telephony Providers + AGSL RuntimeShader",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showResetDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reset Preferences to Default",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFF3B30),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Preferences") },
            text = { Text("Reset all theme, background, and notification preferences to their original settings? Your messages will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetPreferences()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    val colors = LocalSalimColors.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = colors.textSecondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    val colors = LocalSalimColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SquircleCardShape)
            .background(colors.surface)
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalSalimColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent
            )
        )
    }
}

@Composable
private fun ThemeModeSegmentedControl(
    currentMode: ThemeMode,
    onModeSelected: (ThemeMode) -> Unit
) {
    val colors = LocalSalimColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.6f))
            .padding(2.dp)
    ) {
        ThemeMode.values().forEach { mode ->
            val isSelected = currentMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.surface else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AmbientBgSegmentedControl(
    currentMode: AmbientBackgroundMode,
    onModeSelected: (AmbientBackgroundMode) -> Unit
) {
    val colors = LocalSalimColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.6f))
            .padding(2.dp)
    ) {
        AmbientBackgroundMode.values().forEach { mode ->
            val isSelected = currentMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.surface else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}
