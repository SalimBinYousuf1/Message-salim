package com.example.ui.screens.conversation

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Message
import com.example.ui.components.AgslAmbientBackground
import com.example.ui.components.AvatarView
import com.example.ui.components.ComposerBar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.MessageBubble
import com.example.ui.components.SalimDetailTopBar
import com.example.ui.components.formatDateHeader
import com.example.ui.theme.LocalSalimColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDetailScreen(
    viewModel: ConversationDetailViewModel,
    onBackClick: () -> Unit,
    onMediaClick: (Uri) -> Unit,
    onForwardMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = LocalSalimColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var activeMessageForContext by remember { mutableStateOf<Message?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Message?>(null) }
    var showDetailsSheet by remember { mutableStateOf<Message?>(null) }
    var showContactInfoSheet by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }
    var hasScrolledInitially by remember { mutableStateOf(false) }

    // Android Zero-Permission Photo Picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onMediaAttached(uri)
        }
    }

    // Android Contact Picker for sending contacts from contact list
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            try {
                var contactName = ""
                var contactPhone = ""
                val cursor = context.contentResolver.query(
                    contactUri,
                    arrayOf(
                        android.provider.ContactsContract.Contacts._ID,
                        android.provider.ContactsContract.Contacts.DISPLAY_NAME,
                        android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER
                    ),
                    null, null, null
                )
                var hasPhone = 0
                var contactId = ""
                cursor?.use {
                    if (it.moveToFirst()) {
                        contactId = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts._ID))
                        contactName = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.DISPLAY_NAME)) ?: ""
                        hasPhone = it.getInt(it.getColumnIndexOrThrow(android.provider.ContactsContract.Contacts.HAS_PHONE_NUMBER))
                    }
                }

                if (hasPhone > 0 && contactId.isNotEmpty()) {
                    val phoneCursor = context.contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use {
                        if (it.moveToFirst()) {
                            contactPhone = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: ""
                        }
                    }
                }

                if (contactPhone.isNotBlank()) {
                    val vcard = com.example.telephony.VCardHelper.generateVCard(
                        name = contactName.ifBlank { "Contact" },
                        phone = contactPhone
                    )
                    viewModel.onComposerTextChanged(vcard)
                    Toast.makeText(context, "Attached contact: ${contactName.ifBlank { contactPhone }}", Toast.LENGTH_SHORT).show()
                } else if (contactName.isNotBlank()) {
                    viewModel.onComposerTextChanged("Contact: $contactName")
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not load contact info", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceRecording()
        } else {
            Toast.makeText(context, "Microphone permission required for voice messages", Toast.LENGTH_SHORT).show()
        }
    }

    val isPlayingAudio by viewModel.audioPlayer.isPlaying.collectAsStateWithLifecycle()
    val activeAudioUri by viewModel.audioPlayer.activeUri.collectAsStateWithLifecycle()
    val audioProgress by viewModel.audioPlayer.progressFraction.collectAsStateWithLifecycle()
    val audioCurrentMs by viewModel.audioPlayer.currentPositionMs.collectAsStateWithLifecycle()
    val audioDurationMs by viewModel.audioPlayer.durationMs.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.audioPlayer.playbackSpeed.collectAsStateWithLifecycle()

    var spamDismissed by remember { mutableStateOf(false) }
    val isUnknownSender = remember(uiState.displayName, uiState.address) {
        uiState.displayName == uiState.address
    }
    val suspectedSpamMessage = remember(uiState.messages, settings.spamProtectionEnabled, isUnknownSender) {
        if (!settings.spamProtectionEnabled || !isUnknownSender) null
        else uiState.messages.find { it.isIncoming && com.example.telephony.SpamDetector.isSuspectedSpam(uiState.address, it.body, false) }
    }

    // Show latest message directly on initial open without auto-scrolling down from the top
    LaunchedEffect(uiState.messages) {
        if (uiState.messages.isNotEmpty()) {
            if (!hasScrolledInitially) {
                listState.scrollToItem(uiState.messages.size - 1)
                hasScrolledInitially = true
            } else {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 10 }
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
                    title = uiState.displayName,
                    subtitle = if (uiState.isBlocked) "Blocked" else if (uiState.displayName != uiState.address) uiState.address else null,
                    onBackClick = onBackClick,
                    isScrolled = isScrolled,
                    glassOpacity = settings.glassOpacity,
                    reducedTransparency = settings.reducedTransparency,
                    avatar = {
                        AvatarView(
                            name = uiState.displayName,
                            photoUri = uiState.photoUri,
                            size = 38.dp,
                            modifier = Modifier.clickable { showContactInfoSheet = true }
                        )
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleSearch(!uiState.isSearching) }) {
                            Icon(
                                imageVector = if (uiState.isSearching) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (uiState.isSearching) "Close Search" else "Search Chat",
                                tint = colors.accent
                            )
                        }
                        if (uiState.address.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${Uri.encode(uiState.address)}")
                                    }
                                    context.startActivity(dialIntent)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Call Contact",
                                    tint = colors.accent
                                )
                            }
                        }
                        IconButton(onClick = { showContactInfoSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Contact Info",
                                tint = colors.accent
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (uiState.isBlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceTranslucent)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "You blocked this contact. Unblock to send messages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                } else {
                    ComposerBar(
                        text = uiState.composerText,
                        onTextChange = { viewModel.onComposerTextChanged(it) },
                        onSendClick = { viewModel.sendMessage() },
                        onScheduleClick = {
                            if (uiState.composerText.isNotBlank()) {
                                showScheduleSheet = true
                            }
                        },
                        onAttachClick = {
                            showAttachSheet = true
                        },
                        glassOpacity = settings.glassOpacity,
                        reducedTransparency = settings.reducedTransparency,
                        onStartVoiceRecord = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.startVoiceRecording()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onStopVoiceRecordAndSend = { viewModel.stopVoiceRecordingAndSend() },
                        onCancelVoiceRecord = { viewModel.cancelVoiceRecording() },
                        isRecordingVoice = uiState.isRecordingVoice,
                        recordingDurationSec = uiState.recordingDurationSec,
                        recordingAmplitude = uiState.recordingAmplitude,
                        attachedMediaUri = uiState.attachedMediaUri,
                        onRemoveAttachment = { viewModel.onMediaAttached(null) },
                        availableSims = uiState.availableSims,
                        selectedSim = uiState.selectedSim,
                        onToggleSim = { viewModel.toggleSim() }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Apple-style In-Chat Liquid Glass Search Bar
                    AnimatedVisibility(
                        visible = uiState.isSearching,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (colors.isDark) Color(0xFF2C2C2E).copy(alpha = 0.70f)
                                        else Color(0xFFFFFFFF).copy(alpha = 0.85f)
                                    )
                                    .border(
                                        width = 0.8.dp,
                                        color = if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                                        shape = CircleShape
                                    )
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    BasicTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = { viewModel.setSearchQuery(it) },
                                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
                                        singleLine = true,
                                        cursorBrush = SolidColor(colors.accent),
                                        decorationBox = { innerTextField ->
                                            if (uiState.searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Search in conversation...",
                                                    color = colors.textSecondary.copy(alpha = 0.6f),
                                                    fontSize = 14.sp
                                                )
                                            }
                                            innerTextField()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        Text(
                                            text = "${uiState.messages.size} found",
                                            color = colors.accent,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { viewModel.setSearchQuery("") },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cancel",
                                color = colors.accent,
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .clickable { viewModel.toggleSearch(false) }
                                    .padding(horizontal = 4.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Pending Scheduled Messages Banner
                    if (uiState.pendingScheduled.isNotEmpty()) {
                        val firstScheduled = uiState.pendingScheduled.first()
                        val formattedTime = SimpleDateFormat("EEE, MMM d 'at' h:mm a", Locale.getDefault())
                            .format(Date(firstScheduled.scheduledTimestamp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.accent.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scheduled Message",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                                Text(
                                    text = "Will send $formattedTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            TextButton(onClick = { viewModel.cancelScheduledMessage(firstScheduled.id) }) {
                                Text("Cancel", color = Color(0xFFFF3B30), fontSize = 12.sp)
                            }
                        }
                    }

                    // Suspected Spam & Phishing Warning Banner (Apple Amber Liquid Glass)
                    if (suspectedSpamMessage != null && !spamDismissed) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFF9500).copy(alpha = 0.12f))
                                .border(0.8.dp, Color(0xFFFF9500).copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Spam Warning",
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Suspected Spam / Phishing",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9500)
                                )
                                Text(
                                    text = com.example.telephony.SpamDetector.getSpamWarningMessage(suspectedSpamMessage.body),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF3B30))
                                            .clickable {
                                                viewModel.blockContact()
                                                spamDismissed = true
                                                Toast.makeText(context, "Contact blocked and reported", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text("Block Sender", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(colors.surfaceVariant)
                                            .clickable { spamDismissed = true }
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text("Dismiss", color = colors.textPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }

                    // Message List or Empty State
                    if (uiState.messages.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Person,
                            title = uiState.displayName,
                            subtitle = "Send a message to start the conversation.",
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.messages,
                                key = { _, msg -> msg.id }
                            ) { index, msg ->
                                val prevMsg = uiState.messages.getOrNull(index - 1)
                                val nextMsg = uiState.messages.getOrNull(index + 1)

                                val showDateHeader = prevMsg == null || !isSameDay(prevMsg.date, msg.date)
                                if (showDateHeader) {
                                    DateHeaderView(date = msg.date)
                                }

                                val isFirstInGroup = prevMsg == null ||
                                        prevMsg.isIncoming != msg.isIncoming ||
                                        (msg.date - prevMsg.date > 120000L)

                                val isLastInGroup = nextMsg == null ||
                                        nextMsg.isIncoming != msg.isIncoming ||
                                        (nextMsg.date - msg.date > 120000L)

                                MessageBubble(
                                    message = msg,
                                    isFirstInGroup = isFirstInGroup,
                                    isLastInGroup = isLastInGroup,
                                    reaction = uiState.reactions[msg.id],
                                    onReactionClick = { activeMessageForContext = it },
                                    onLongClick = { activeMessageForContext = it },
                                    onMediaClick = { it.mediaUri?.let { uri -> onMediaClick(uri) } },
                                    onRetryClick = { viewModel.retrySendMessage(it) },
                                    fontScale = settings.fontSizeScale,
                                    isPlayingAudio = isPlayingAudio && activeAudioUri == msg.mediaUri,
                                    audioProgress = if (activeAudioUri == msg.mediaUri) audioProgress else 0f,
                                    audioCurrentMs = if (activeAudioUri == msg.mediaUri) audioCurrentMs else 0,
                                    audioDurationMs = if (activeAudioUri == msg.mediaUri) audioDurationMs else 0,
                                    playbackSpeed = playbackSpeed,
                                    onPlayAudioClick = { viewModel.playVoiceNote(context, it.mediaUri!!) },
                                    onToggleAudioSpeed = { viewModel.audioPlayer.toggleSpeed() },
                                    onSeekAudio = { viewModel.audioPlayer.seekTo(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Schedule Message Options Sheet
    if (showScheduleSheet) {
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Schedule Message",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Tomorrow at 9:00 AM
                val calTomorrow = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                ScheduleOptionRow(
                    title = "Tomorrow Morning",
                    subtitle = "Tomorrow at 9:00 AM",
                    onClick = {
                        viewModel.scheduleMessage(calTomorrow.timeInMillis)
                        showScheduleSheet = false
                        Toast.makeText(context, "Message scheduled for tomorrow 9:00 AM", Toast.LENGTH_SHORT).show()
                    }
                )

                // This Evening at 6:00 PM (or tomorrow if past 6)
                val calEvening = Calendar.getInstance().apply {
                    if (get(Calendar.HOUR_OF_DAY) >= 18) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                    set(Calendar.HOUR_OF_DAY, 18)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                ScheduleOptionRow(
                    title = "This Evening",
                    subtitle = "Today at 6:00 PM",
                    onClick = {
                        viewModel.scheduleMessage(calEvening.timeInMillis)
                        showScheduleSheet = false
                        Toast.makeText(context, "Message scheduled for this evening 6:00 PM", Toast.LENGTH_SHORT).show()
                    }
                )

                // In 1 Hour
                ScheduleOptionRow(
                    title = "In 1 Hour",
                    subtitle = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis() + 3600000L)),
                    onClick = {
                        viewModel.scheduleMessage(System.currentTimeMillis() + 3600000L)
                        showScheduleSheet = false
                        Toast.makeText(context, "Message scheduled for 1 hour from now", Toast.LENGTH_SHORT).show()
                    }
                )

                // In 3 Hours
                ScheduleOptionRow(
                    title = "In 3 Hours",
                    subtitle = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis() + 10800000L)),
                    onClick = {
                        viewModel.scheduleMessage(System.currentTimeMillis() + 10800000L)
                        showScheduleSheet = false
                        Toast.makeText(context, "Message scheduled for 3 hours from now", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Message Long-Press Actions BottomSheet
    if (activeMessageForContext != null) {
        val message = activeMessageForContext!!
        ModalBottomSheet(
            onDismissRequest = { activeMessageForContext = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Preview header
                Text(
                    text = if (message.body.isNotBlank()) message.body else "Attachment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                // Apple Tapback Floating Reaction Bar
                val currentReaction = uiState.reactions[message.id]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (colors.isDark) Color(0xFF2C2C2E).copy(alpha = 0.75f)
                            else Color(0xFFF2F2F7)
                        )
                        .border(
                            0.8.dp,
                            if (colors.isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.8f),
                            CircleShape
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val reactions = listOf("❤️", "👍", "👎", "😂", "‼️", "❓")
                    reactions.forEach { emoji ->
                        val isSelected = currentReaction == emoji
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) colors.accent.copy(alpha = 0.25f) else Color.Transparent)
                                .clickable {
                                    viewModel.setReaction(message.id, emoji)
                                    activeMessageForContext = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                fontSize = if (isSelected) 24.sp else 20.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = colors.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                if (message.body.isNotBlank()) {
                    ContextMenuItem(
                        icon = Icons.Default.ContentCopy,
                        text = "Copy Text",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("SMS Message", message.body)
                            clipboard.setPrimaryClip(clip)
                            activeMessageForContext = null
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                ContextMenuItem(
                    icon = Icons.Default.Forward,
                    text = "Forward",
                    onClick = {
                        activeMessageForContext = null
                        onForwardMessage(message.body)
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.Share,
                    text = "Share",
                    onClick = {
                        activeMessageForContext = null
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message.body)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Message"))
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.Info,
                    text = "View Details",
                    onClick = {
                        val msg = activeMessageForContext
                        activeMessageForContext = null
                        showDetailsSheet = msg
                    }
                )

                ContextMenuItem(
                    icon = Icons.Default.Delete,
                    text = "Delete Message",
                    tint = Color(0xFFFF3B30),
                    onClick = {
                        val msg = activeMessageForContext
                        activeMessageForContext = null
                        showDeleteConfirmDialog = msg
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Message Details Sheet
    if (showDetailsSheet != null) {
        val msg = showDetailsSheet!!
        val dateFormatted = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm:ss a", Locale.getDefault()).format(Date(msg.date))
        ModalBottomSheet(
            onDismissRequest = { showDetailsSheet = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Message Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                DetailRow(label = "Direction", value = if (msg.isIncoming) "Incoming (Received)" else "Outgoing (Sent)")
                DetailRow(label = "Address", value = msg.address)
                DetailRow(label = "Timestamp", value = dateFormatted)
                DetailRow(label = "Delivery Status", value = msg.status.name)
                DetailRow(label = "Type", value = if (msg.isMms) "MMS Multimedia" else "SMS Standard")
                if (msg.subId != -1) {
                    DetailRow(label = "Subscription ID", value = msg.subId.toString())
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Contact Info & Block Sheet
    if (showContactInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showContactInfoSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarView(name = uiState.displayName, photoUri = uiState.photoUri, size = 72.dp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = uiState.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ContactActionButton(
                        icon = Icons.Default.Call,
                        label = "Call",
                        onClick = {
                            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${Uri.encode(uiState.address)}")
                            }
                            context.startActivity(dialIntent)
                        }
                    )
                    ContactActionButton(
                        icon = Icons.Default.ContentCopy,
                        label = "Copy",
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Phone Number", uiState.address))
                            Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ContactActionButton(
                        icon = Icons.Default.Block,
                        label = if (uiState.isBlocked) "Unblock" else "Block",
                        tint = if (uiState.isBlocked) colors.accent else Color(0xFFFF3B30),
                        onClick = {
                            viewModel.toggleBlockCurrentContact()
                            showContactInfoSheet = false
                            val msg = if (uiState.isBlocked) "Contact unblocked" else "Contact blocked"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Message") },
            text = { Text("Are you sure you want to delete this message? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog?.let { viewModel.deleteMessage(it.id) }
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Apple-style Frosted Attachment & Share Sheet (+ icon)
    if (showAttachSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Share & Attach",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Photos & Videos option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachSheet = false
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Photos & Videos", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("Send pictures or video clips from gallery", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }

                HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Share Contact option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachSheet = false
                            contactPickerLauncher.launch(null)
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFF34C759), modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Share Contact", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("Send a contact card from your address book", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }

                HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)

                // Schedule Send option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            showAttachSheet = false
                            if (uiState.composerText.isNotBlank()) {
                                showScheduleSheet = true
                            } else {
                                Toast.makeText(context, "Type a message first to schedule", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9500).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text("Schedule Send", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("Set a specific date and time to send", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ScheduleOptionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalSalimColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

@Composable
private fun ContactActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = LocalSalimColors.current.accent
) {
    val colors = LocalSalimColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
    }
}

@Composable
private fun DateHeaderView(date: Long) {
    val colors = LocalSalimColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formatDateHeader(date),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = LocalSalimColors.current.textPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val colors = LocalSalimColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, fontWeight = FontWeight.SemiBold)
    }
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
