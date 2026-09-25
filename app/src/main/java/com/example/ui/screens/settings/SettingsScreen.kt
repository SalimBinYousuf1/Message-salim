package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.local.BlockedContact
import com.example.data.preferences.AccentPalette
import com.example.data.preferences.AmbientBackgroundMode
import com.example.data.preferences.ThemeMode
import com.example.telephony.SmsHelper
import com.example.ui.components.AgslAmbientBackground
import com.example.ui.components.SalimDetailTopBar
import com.example.ui.components.SquircleButtonShape
import com.example.ui.components.SquircleCardShape
import com.example.ui.theme.LocalSalimColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isDefaultSms by viewModel.isDefaultSmsApp.collectAsStateWithLifecycle()
    val blockedList by viewModel.blockedContacts.collectAsStateWithLifecycle(initialValue = emptyList())
    val colors = LocalSalimColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshDefaultSmsStatus()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshDefaultSmsStatus()
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var showBlockedSheet by remember { mutableStateOf(false) }
    var showAddBlockDialog by remember { mutableStateOf(false) }
    var newBlockNumber by remember { mutableStateOf("") }
    var newBlockName by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonText by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 20 }
    }

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
                    onBackClick = onBackClick,
                    isScrolled = isScrolled,
                    glassOpacity = settings.glassOpacity,
                    reducedTransparency = settings.reducedTransparency
                )
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Telephony / Default SMS App Group
                item {
                    SettingsSectionTitle("DEFAULT SMS APPLICATION")
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
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sms,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isDefaultSms) "Salim is Default SMS App" else "Not Set as Default",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isDefaultSms) "Ready for incoming and outgoing SMS/MMS" else "Required by Android to send and delete messages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
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
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Set Default", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Active",
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Appearance & Themes Group
                item {
                    SettingsSectionTitle("APPEARANCE & THEME")
                    SettingsGroupCard {
                        // Theme Mode Segmented Picker
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Interface Theme",
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

                        // Pure Black OLED Toggle
                        SettingsToggleRow(
                            title = "Pure Black OLED Dark Mode",
                            subtitle = "Zero pixel emission (#000000) for OLED panels",
                            checked = settings.pureBlackOled,
                            onCheckedChange = { viewModel.setPureBlackOled(it) }
                        )

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Accent Color Palette Picker
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Accent Color",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Monochrome base with refined color accent",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AccentPalette.entries.forEach { palette ->
                                    val isSelected = settings.accentPalette == palette
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color(palette.hexColor))
                                            .border(
                                                width = if (isSelected) 3.dp else 0.dp,
                                                color = if (isSelected) colors.textPrimary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.setAccentPalette(palette) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
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
                                text = "Ambient AGSL Background",
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

                        if (!settings.reducedTransparency) {
                            HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Liquid Glass Opacity",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colors.textPrimary,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${(settings.glassOpacity * 100).toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.accent,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Dial the physical glass effect from translucent to near-opaque",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                                )
                                Slider(
                                    value = settings.glassOpacity,
                                    onValueChange = { viewModel.setGlassOpacity(it) },
                                    valueRange = 0.25f..0.98f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = colors.accent,
                                        activeTrackColor = colors.accent,
                                        inactiveTrackColor = colors.surfaceVariant
                                    )
                                )
                            }
                        }

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

                // Security & Biometrics Group
                item {
                    SettingsSectionTitle("SECURITY & BIOMETRICS")
                    SettingsGroupCard {
                        // Biometric Lock
                        SettingsToggleRow(
                            title = "Biometric App Lock",
                            subtitle = "Require FaceID / Fingerprint unlock when opening app",
                            checked = settings.biometricLockEnabled,
                            onCheckedChange = { viewModel.setBiometricLock(it) }
                        )

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        // Screen Privacy Shield (FLAG_SECURE)
                        SettingsToggleRow(
                            title = "Privacy Shield in App Switcher",
                            subtitle = "Hides conversation content in recent apps and blocks screenshots",
                            checked = settings.flagSecureEnabled,
                            onCheckedChange = { viewModel.setFlagSecure(it) }
                        )

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsToggleRow(
                            title = "Lock Screen Notification Privacy",
                            subtitle = "Hide message preview and body on lock screen notifications",
                            checked = settings.lockScreenPrivacy,
                            onCheckedChange = { viewModel.setLockScreenPrivacy(it) }
                        )
                    }
                }

                // Haptics & Sounds Group
                item {
                    SettingsSectionTitle("HAPTICS & SOUNDS")
                    SettingsGroupCard {
                        SettingsToggleRow(
                            title = "Haptic Feedback",
                            subtitle = "Sensory feedback when sending messages and long-pressing items",
                            checked = settings.hapticsEnabled,
                            onCheckedChange = { viewModel.setHaptics(it) }
                        )

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        SettingsToggleRow(
                            title = "Message Delivery Tone",
                            subtitle = "Play Apple-style acoustic swoosh sound on message sent",
                            checked = settings.deliverySoundsEnabled,
                            onCheckedChange = { viewModel.setDeliverySounds(it) }
                        )
                    }
                }

                // Spam & Blocked Contacts
                item {
                    SettingsSectionTitle("SPAM & BLOCKED NUMBERS")
                    SettingsGroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBlockedSheet = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Blocked Contacts",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${blockedList.size} numbers blocked",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Data Backup & Restore
                item {
                    SettingsSectionTitle("BACKUP & EXPORT")
                    SettingsGroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val backupJson = viewModel.exportBackup(context)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_TEXT, backupJson)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export Salim Messages Backup"))
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Export Settings & Pinned Metadata", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontSize = 15.sp)
                                Text(text = "Save your app preferences, drafts, and blocked list", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
                        }

                        HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRestoreDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Restore, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Restore from JSON", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontSize = 15.sp)
                                Text(text = "Import previously exported backup", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
                        }
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
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Blocked Contacts Modal Sheet
    if (showBlockedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBlockedSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Blocked Contacts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    IconButton(onClick = { showAddBlockDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Block new number", tint = colors.accent)
                    }
                }

                if (blockedList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No blocked contacts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        items(blockedList) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = item.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                                    Text(text = item.address, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                                TextButton(onClick = { viewModel.unblockContact(item.address) }) {
                                    Text("Unblock", color = colors.accent)
                                }
                            }
                            HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }

    // Add Block Number Dialog
    if (showAddBlockDialog) {
        AlertDialog(
            onDismissRequest = { showAddBlockDialog = false },
            title = { Text("Block a Number") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newBlockNumber,
                        onValueChange = { newBlockNumber = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newBlockName,
                        onValueChange = { newBlockName = it },
                        label = { Text("Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newBlockNumber.isNotBlank()) {
                            viewModel.blockNumber(newBlockNumber, newBlockName)
                            newBlockNumber = ""
                            newBlockName = ""
                            showAddBlockDialog = false
                        }
                    }
                ) {
                    Text("Block", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Backup") },
            text = {
                Column {
                    Text("Paste your backup JSON below to restore preferences and settings:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonText,
                        onValueChange = { restoreJsonText = it },
                        label = { Text("Backup JSON") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val ok = viewModel.restoreBackup(context, restoreJsonText)
                            showRestoreDialog = false
                            restoreJsonText = ""
                            Toast.makeText(context, if (ok) "Backup restored successfully" else "Invalid backup format", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Restore", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset confirmation dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Preferences") },
            text = { Text("Are you sure you want to reset all preferences to default values?") },
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
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsGroupCard(content: @Composable () -> Unit) {
    val colors = LocalSalimColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
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
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, fontSize = 15.sp)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = colors.surfaceVariant
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
            .clip(SquircleButtonShape)
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = currentMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(SquircleButtonShape)
                    .background(if (isSelected) colors.surface else Color.Transparent)
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1
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
            .background(colors.surfaceVariant.copy(alpha = 0.5f))
            .padding(2.dp)
    ) {
        AmbientBackgroundMode.entries.forEach { mode ->
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
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) colors.textPrimary else colors.textSecondary
                )
            }
        }
    }
}
