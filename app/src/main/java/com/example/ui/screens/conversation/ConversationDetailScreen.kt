package com.example.ui.screens.conversation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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

    // Android Zero-Permission Photo Picker (Google Play policy compliant)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setAttachedMedia(uri)
        }
    }

    // Auto scroll to bottom when messages update
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
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
                    subtitle = if (uiState.displayName != uiState.address) uiState.address else null,
                    onBackClick = onBackClick,
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
                ComposerBar(
                    text = uiState.composerText,
                    onTextChange = { viewModel.onComposerTextChanged(it) },
                    onSendClick = { viewModel.sendMessage() },
                    onAttachClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    attachedMediaUri = uiState.attachedMediaUri,
                    onRemoveAttachment = { viewModel.setAttachedMedia(null) },
                    availableSims = uiState.availableSims,
                    selectedSim = uiState.selectedSim,
                    onToggleSim = { viewModel.toggleSim() }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.messages.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Person,
                        title = uiState.displayName,
                        subtitle = "Send a message to start the conversation.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.messages,
                            key = { _, msg -> msg.id }
                        ) { index, msg ->
                            val prevMsg = uiState.messages.getOrNull(index - 1)
                            val nextMsg = uiState.messages.getOrNull(index + 1)

                            // Show date separator if 1st message or day changed
                            val showDateHeader = prevMsg == null || !isSameDay(prevMsg.date, msg.date)
                            if (showDateHeader) {
                                DateHeaderView(date = msg.date)
                            }

                            val isFirstInGroup = prevMsg == null ||
                                    prevMsg.isIncoming != msg.isIncoming ||
                                    (msg.date - prevMsg.date > 120000L) // 2 minutes gap

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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header preview
                Text(
                    text = message.body.ifBlank { if (message.isMms) "MMS Attachment" else "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 2,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                HorizontalDivider(color = colors.surfaceVariant.copy(alpha = 0.5f))

                ContextActionRow(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy Text",
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("SMS", message.body))
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        activeMessageForContext = null
                    }
                )

                ContextActionRow(
                    icon = Icons.Default.Share,
                    label = "Share",
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, message.body)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Message"))
                        activeMessageForContext = null
                    }
                )

                ContextActionRow(
                    icon = Icons.Default.Forward,
                    label = "Forward",
                    onClick = {
                        onForwardMessage(message.body)
                        activeMessageForContext = null
                    }
                )

                ContextActionRow(
                    icon = Icons.Default.Info,
                    label = "View Details",
                    onClick = {
                        showDetailsSheet = message
                        activeMessageForContext = null
                    }
                )

                ContextActionRow(
                    icon = Icons.Default.Delete,
                    label = "Delete Message",
                    color = Color(0xFFFF3B30),
                    onClick = {
                        showDeleteConfirmDialog = message
                        activeMessageForContext = null
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Message Details Sheet
    if (showDetailsSheet != null) {
        val msg = showDetailsSheet!!
        AlertDialog(
            onDismissRequest = { showDetailsSheet = null },
            title = { Text("Message Details") },
            text = {
                Column {
                    DetailLine("Type", if (msg.isMms) "MMS" else "SMS")
                    DetailLine("Direction", if (msg.isIncoming) "Incoming" else "Outgoing")
                    DetailLine("Status", msg.status.name)
                    DetailLine("Date", SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.getDefault()).format(Date(msg.date)))
                    if (msg.address.isNotBlank()) {
                        DetailLine("Address", msg.address)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsSheet = null }) {
                    Text("OK", color = colors.accent)
                }
            }
        )
    }

    // Delete Single Message Confirmation
    if (showDeleteConfirmDialog != null) {
        val msg = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Message") },
            text = { Text("Are you sure you want to permanently delete this message?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessage(msg.id)
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

    // Contact Information Sheet
    if (showContactInfoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showContactInfoSheet = false },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarView(
                    name = uiState.displayName,
                    photoUri = uiState.photoUri,
                    size = 72.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = uiState.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary
                )

                if (uiState.address.isNotBlank()) {
                    Text(
                        text = uiState.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                        icon = Icons.Default.Person,
                        label = "Contacts",
                        onClick = {
                            // Open system contacts
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people/"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun ContactActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalSalimColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun ContextActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    color: Color? = null
) {
    val colors = LocalSalimColors.current
    val tintColor = color ?: colors.textPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tintColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = tintColor,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val colors = LocalSalimColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary
        )
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
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val f = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return f.format(Date(t1)) == f.format(Date(t2))
}
