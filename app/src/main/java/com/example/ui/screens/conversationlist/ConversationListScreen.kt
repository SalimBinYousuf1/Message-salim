package com.example.ui.screens.conversationlist

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Conversation
import com.example.telephony.SmsHelper
import com.example.ui.components.AgslAmbientBackground
import com.example.ui.components.AvatarView
import com.example.ui.components.DefaultSmsPromptBanner
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SalimLargeTopBar
import com.example.ui.components.SquircleCardShape
import com.example.ui.theme.LocalSalimColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
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
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshStatus()
    }

    // Role Manager default SMS launcher
    val defaultSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.refreshStatus()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStatus()
    }

    var threadToDelete by remember { mutableStateOf<Conversation?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    AgslAmbientBackground(
        mode = settings.ambientBackground,
        reducedMotion = settings.reducedMotion,
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                SalimLargeTopBar(
                    title = if (uiState.filterArchived) "Archived" else "Messages",
                    actions = {
                        if (uiState.isSelectionMode) {
                            IconButton(onClick = { showBulkDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete selected",
                                    tint = Color(0xFFFF3B30)
                                )
                            }
                            IconButton(onClick = { viewModel.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel selection",
                                    tint = colors.textPrimary
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.toggleFilterArchived() }) {
                                Icon(
                                    imageVector = if (uiState.filterArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                    contentDescription = "Archive toggle",
                                    tint = if (uiState.filterArchived) colors.accent else colors.textPrimary
                                )
                            }
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
                if (!uiState.isSelectionMode) {
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
                        .padding(horizontal = 16.dp, vertical = 6.dp)
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

                // Permission Warning State
                if (!uiState.hasSmsPermission) {
                    EmptyStateView(
                        icon = Icons.Default.ChatBubbleOutline,
                        title = "SMS Permission Required",
                        subtitle = "Salim needs access to your SMS and Contacts to show and send messages.",
                        actionLabel = "Grant Permissions",
                        onActionClick = {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.READ_SMS,
                                    android.Manifest.permission.SEND_SMS,
                                    android.Manifest.permission.RECEIVE_SMS,
                                    android.Manifest.permission.READ_CONTACTS
                                )
                            )
                        }
                    )
                    return@Column
                }

                // Empty State
                if (uiState.conversations.isEmpty()) {
                    if (uiState.searchQuery.isNotBlank()) {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            title = "No Results",
                            subtitle = "No conversations found for \"${uiState.searchQuery}\""
                        )
                    } else if (uiState.filterArchived) {
                        EmptyStateView(
                            icon = Icons.Default.Archive,
                            title = "No Archived Messages",
                            subtitle = "Swipe left on any conversation in your inbox to archive it."
                        )
                    } else {
                        EmptyStateView(
                            icon = Icons.Default.ChatBubbleOutline,
                            title = "No Messages",
                            subtitle = "Your messages will appear here. Start a new conversation now.",
                            actionLabel = "New Message",
                            onActionClick = onNavigateToCompose
                        )
                    }
                    return@Column
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Pinned Conversations Row (if any and not searching)
                    if (uiState.pinnedConversations.isNotEmpty() && uiState.searchQuery.isBlank()) {
                        item(key = "pinned_header") {
                            Text(
                                text = "PINNED",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        item(key = "pinned_row") {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.pinnedConversations, key = { "pinned_${it.threadId}" }) { conv ->
                                    PinnedConversationItem(
                                        conversation = conv,
                                        onClick = { onNavigateToConversation(conv.threadId, conv.address) },
                                        onLongClick = { viewModel.togglePin(conv.threadId, conv.isPinned) }
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = colors.surfaceVariant.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }

                    // Main Conversations List
                    items(
                        items = if (uiState.pinnedConversations.isNotEmpty() && uiState.searchQuery.isBlank()) {
                            uiState.unpinnedConversations
                        } else {
                            uiState.conversations
                        },
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
                                viewModel.toggleSelectThread(conv.threadId)
                            },
                            onPinToggle = { viewModel.togglePin(conv.threadId, conv.isPinned) },
                            onArchiveToggle = { viewModel.toggleArchive(conv.threadId, conv.isArchived) },
                            onDelete = { threadToDelete = conv }
                        )
                    }
                }
            }
        }
    }

    // Confirmation dialog for deleting a single thread
    if (threadToDelete != null) {
        AlertDialog(
            onDismissRequest = { threadToDelete = null },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation with ${threadToDelete?.displayName}? This action cannot be undone.") },
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

@OptIn(ExperimentalFoundationApi::class)
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
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRowItem(
    conversation: Conversation,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPinToggle: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalSalimColors.current
    val isUnread = conversation.unreadCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (isSelected) colors.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    fontSize = 12.sp
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
                            .clip(CircleShape)
                            .background(colors.accent)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    HorizontalDivider(
        color = colors.surfaceVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 76.dp)
    )
}

fun formatConversationDate(dateMillis: Long): String {
    if (dateMillis <= 0L) return ""
    val now = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { timeInMillis = dateMillis }

    return when {
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == msgCal.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(dateMillis))
        }
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) &&
                now.get(Calendar.WEEK_OF_YEAR) == msgCal.get(Calendar.WEEK_OF_YEAR) -> {
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(dateMillis))
        }
        now.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dateMillis))
        }
        else -> {
            SimpleDateFormat("M/d/yy", Locale.getDefault()).format(Date(dateMillis))
        }
    }
}
