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
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

    // Android Zero-Permission Photo Picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onMediaAttached(uri)
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

    // Auto scroll to bottom when messages update
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
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
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
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
                                    onLongClick = { activeMessageForContext = it },
                                    onMediaClick = { it.mediaUri?.let { uri -> onMediaClick(uri) } },
                                    onRetryClick = { viewModel.retrySendMessage(it) }
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
