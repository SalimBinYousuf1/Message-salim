package com.example.ui.screens.conversationlist

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.telephony.OtpHelper
import com.example.telephony.SecurityHelper
import com.example.telephony.SmsHelper
import com.example.ui.components.AgslAmbientBackground
import com.example.ui.components.AvatarView
import com.example.ui.components.DefaultSmsPromptBanner
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SalimLargeTopBar
import com.example.ui.components.SquircleButtonShape
import com.example.ui.components.SquircleCardShape
import com.example.ui.components.SquirclePillShape
import com.example.ui.components.applePressable
import com.example.ui.theme.LocalSalimColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel,
    onNavigateToConversation: (threadId: Long, address: String) -> Unit,
    onNavigateToCompose: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = LocalSalimColors.current
    val context = LocalContext.current

    // Request permissions launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshStatus()
    }

    // Default SMS app launcher
    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshStatus()
    }

    LaunchedEffect(Unit) {
        if (!uiState.hasSmsPermission) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_SMS,
                    android.Manifest.permission.RECEIVE_SMS,
                    android.Manifest.permission.SEND_SMS,
                    android.Manifest.permission.READ_CONTACTS
                )
            )
        }
        viewModel.refreshStatus()
    }

    // Biometric lock prompt on first launch if enabled
    LaunchedEffect(settings.biometricLockEnabled) {
        if (settings.biometricLockEnabled && !uiState.isBiometricUnlocked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && context is Activity) {
                SecurityHelper.showBiometricPrompt(
                    activity = context,
                    onSuccess = { viewModel.setBiometricUnlocked(true) },
                    onError = { /* wait for user button tap */ }
                )
            }
        }
    }

    var threadToDelete by remember { mutableStateOf<Conversation?>(null) }
    var actionSheetThread by remember { mutableStateOf<Conversation?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 20 }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // AGSL Fluid Ambient Background
        AgslAmbientBackground(
            mode = settings.ambientBackground,
            reducedMotion = settings.reducedMotion,
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                containerColor = Color.Transparent,
            topBar = {
                SalimLargeTopBar(
                    title = if (uiState.isSelectionMode) {
                        "${uiState.selectedThreadIds.size} Selected"
                    } else {
                        "Messages"
                    },
                    isScrolled = isScrolled,
                    glassOpacity = settings.glassOpacity,
                    reducedTransparency = settings.reducedTransparency,
                    isSelectionMode = uiState.isSelectionMode,
                    onCancelSelection = { viewModel.clearSelection() },
                    actions = {
                        if (uiState.isSelectionMode) {
                            IconButton(onClick = { viewModel.markSelectedAsRead() }) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = "Mark Read",
                                    tint = colors.accent
                                )
                            }
                            IconButton(onClick = { showBulkDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = Color(0xFFFF3B30)
                                )
                            }
                        } else {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = colors.textPrimary
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (!uiState.isSelectionMode && uiState.isBiometricUnlocked) {
                    FloatingActionButton(
                        onClick = onNavigateToCompose,
                        containerColor = colors.accent,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "New Message",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search Field Capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                }

                // Apple Segmented Filter Pills (All, Personal, Transactions, Unknown, Archived)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ConversationCategoryFilter.entries.toTypedArray()) { category ->
                        val isSelected = uiState.categoryFilter == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) colors.textPrimary else colors.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { viewModel.setCategoryFilter(category) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = category.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) colors.background else colors.textPrimary
                            )
                        }
                    }
                }

                // Default SMS App Banner if not default
                if (!uiState.isDefaultSmsApp) {
                    DefaultSmsPromptBanner(
                        onSetDefaultClick = {
                            val intent = SmsHelper.getRequestDefaultSmsAppIntent(context)
                            if (intent != null) {
                                defaultSmsLauncher.launch(intent)
                            }
                        }
                    )
                }

                // Pinned conversations horizontal carousel (only on 'All' tab)
                if (uiState.categoryFilter == ConversationCategoryFilter.ALL &&
                    uiState.pinnedConversations.isNotEmpty() &&
                    uiState.searchQuery.isBlank()
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = uiState.pinnedConversations,
                            key = { "pinned_${it.threadId}" }
                        ) { conv ->
                            PinnedConversationItem(
                                conversation = conv,
                                onClick = { onNavigateToConversation(conv.threadId, conv.address) },
                                onLongClick = { actionSheetThread = conv }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Conversation List or Empty State
                if (uiState.conversations.isEmpty()) {
                    val emptyTitle = when (uiState.categoryFilter) {
                        ConversationCategoryFilter.ALL -> if (uiState.searchQuery.isBlank()) "No Conversations" else "No Results Found"
                        ConversationCategoryFilter.PERSONAL -> "No Personal Messages"
                        ConversationCategoryFilter.TRANSACTIONS -> "No Transaction Alerts"
                        ConversationCategoryFilter.UNKNOWN -> "No Unknown Senders"
                        ConversationCategoryFilter.ARCHIVED -> "Archive is Empty"
                    }
                    EmptyStateView(
                        title = emptyTitle,
                        subtitle = if (uiState.searchQuery.isBlank()) {
                            "Messages received or sent will appear here."
                        } else {
                            "No messages matching \"${uiState.searchQuery}\""
                        },
                        icon = Icons.Default.ChatBubbleOutline,
                        actionLabel = if (uiState.searchQuery.isBlank()) "Start Conversation" else null,
                        onActionClick = onNavigateToCompose,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    val listToShow = if (uiState.categoryFilter == ConversationCategoryFilter.ALL && uiState.searchQuery.isBlank()) {
                        uiState.unpinnedConversations
                    } else {
                        uiState.conversations
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(
                            items = listToShow,
                            key = { it.threadId }
                        ) { conv ->
                            val isSelected = uiState.selectedThreadIds.contains(conv.threadId)
                            ConversationRowItem(
                                conversation = conv,
                                isSelected = isSelected,
                                isSelectionMode = uiState.isSelectionMode,
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleSelectThread(conv.threadId)
                                    } else {
                                        onNavigateToConversation(conv.threadId, conv.address)
                                    }
                                },
                                onLongClick = {
                                    actionSheetThread = conv
                                },
                                onCopyOtp = { code ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("OTP Code", code))
                                    Toast.makeText(context, "Code $code copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
        }

        // Biometric Lock Screen Shield Overlay
        if (settings.biometricLockEnabled && !uiState.isBiometricUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = colors.accent,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Salim Messages Locked",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Authentication is required to view your conversations.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && context is Activity) {
                                SecurityHelper.showBiometricPrompt(
                                    activity = context,
                                    onSuccess = { viewModel.setBiometricUnlocked(true) },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Action Sheet Modal BottomSheet for Conversation Options (Apple style)
    if (actionSheetThread != null) {
        val target = actionSheetThread!!
        val isUnread = target.unreadCount > 0

        ModalBottomSheet(
            onDismissRequest = { actionSheetThread = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = target.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Read/Unread
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.markThreadRead(target.threadId, isUnread)
                            actionSheetThread = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isUnread) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (isUnread) "Mark as Read" else "Mark as Unread",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }

                // Toggle Pin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.togglePin(target.threadId, target.isPinned)
                            actionSheetThread = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (target.isPinned) Icons.Default.PinDrop else Icons.Default.PushPin,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (target.isPinned) "Unpin Conversation" else "Pin Conversation",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }

                // Toggle Archive
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.toggleArchive(target.threadId, target.isArchived)
                            actionSheetThread = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (target.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (target.isArchived) "Unarchive Conversation" else "Archive Conversation",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }

                // Multi-select mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            viewModel.toggleSelectThread(target.threadId)
                            actionSheetThread = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Select Conversation",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textPrimary
                    )
                }

                // Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            threadToDelete = target
                            actionSheetThread = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Delete Conversation",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Confirmation dialog for deleting a single thread
    if (threadToDelete != null) {
        AlertDialog(
            onDismissRequest = { threadToDelete = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation with ${threadToDelete?.displayName}? All messages will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        threadToDelete?.let { viewModel.deleteThread(it.threadId) }
                        threadToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { threadToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for bulk delete
    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Delete ${uiState.selectedThreadIds.size} Conversations") },
            text = { Text("Are you sure you want to delete all selected conversations? All messages will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedThreads()
                        showBulkDeleteDialog = false
                    }
                ) {
                    Text("Delete All", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PinnedConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalSalimColors.current

    Column(
        modifier = Modifier
            .width(68.dp)
            .applePressable(
                pressedScale = 0.94f,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            AvatarView(
                name = conversation.displayName,
                photoUri = conversation.photoUri,
                size = 56.dp
            )

            if (conversation.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = conversation.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ConversationRowItem(
    conversation: Conversation,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCopyOtp: (String) -> Unit
) {
    val colors = LocalSalimColors.current
    val isUnread = conversation.unreadCount > 0
    val detectedOtp = remember(conversation.snippet) {
        OtpHelper.extractOtp(conversation.snippet)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(SquircleCardShape)
            .background(if (isSelected) colors.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
            .applePressable(
                pressedScale = 0.985f,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) colors.accent else colors.textTertiary,
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(22.dp)
            )
        }

        // Unread Blue Dot Indicator (Apple style)
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isUnread && !isSelectionMode) colors.accent else Color.Transparent)
        )

        AvatarView(
            name = conversation.displayName,
            photoUri = conversation.photoUri,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (conversation.isPinned) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = formatConversationDate(conversation.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isUnread) colors.accent else colors.textSecondary,
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val snippetText = if (!conversation.draft.isNullOrBlank()) {
                    "Draft: ${conversation.draft}"
                } else {
                    conversation.snippet
                }

                Text(
                    text = snippetText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (!conversation.draft.isNullOrBlank()) Color(0xFFFF9500) else if (isUnread) colors.textPrimary else colors.textSecondary,
                    fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(SquirclePillShape)
                            .background(colors.accent)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // Quick OTP Copy Pill if detected
            if (detectedOtp != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(SquircleButtonShape)
                        .background(colors.accent.copy(alpha = 0.12f))
                        .applePressable(onClick = { onCopyOtp(detectedOtp) })
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = colors.accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Code: $detectedOtp",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.accent
                    )
                }
            }
        }
    }
}

private fun formatConversationDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }

    return if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)
    ) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
    } else if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) == 1
    ) {
        "Yesterday"
    } else if (now.get(Calendar.WEEK_OF_YEAR) == msgTime.get(Calendar.WEEK_OF_YEAR) &&
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)
    ) {
        SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("M/d/yy", Locale.getDefault()).format(Date(timestamp))
    }
}
