package com.example.ui.screens.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ContactItem
import com.example.ui.components.AgslAmbientBackground
import com.example.ui.components.AvatarView
import com.example.ui.components.ComposerBar
import com.example.ui.components.LiquidGlassSurface
import com.example.ui.components.SquircleCardShape
import com.example.ui.components.SquirclePillShape
import com.example.ui.components.applePressable
import com.example.ui.theme.LocalSalimColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewMessageScreen(
    viewModel: NewMessageViewModel,
    onCancel: () -> Unit,
    onMessageSent: (threadId: Long, address: String) -> Unit,
    initialBody: String = "",
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val colors = LocalSalimColors.current

    if (initialBody.isNotBlank() && uiState.composerText.isBlank()) {
        viewModel.onComposerTextChanged(initialBody)
    }

    val listState = rememberLazyListState()
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
                LiquidGlassSurface(
                    isScrolled = isScrolled,
                    glassOpacity = settings.glassOpacity,
                    reducedTransparency = settings.reducedTransparency,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onCancel) {
                                Text("Cancel", color = colors.accent, fontSize = 16.sp)
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Text(
                                text = "New Message",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Spacer(modifier = Modifier.width(60.dp))
                        }

                        // To: line with recipient chips and inline text field
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "To: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            FlowRow(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                            ) {
                                uiState.selectedRecipients.forEach { number ->
                                    RecipientChip(
                                        text = number,
                                        onRemove = { viewModel.removeRecipient(number) }
                                    )
                                }

                                BasicTextField(
                                    value = uiState.recipientInput,
                                    onValueChange = { viewModel.onRecipientInputChanged(it) },
                                    textStyle = TextStyle(
                                        color = colors.textPrimary,
                                        fontSize = 15.sp
                                    ),
                                    cursorBrush = SolidColor(colors.accent),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (uiState.recipientInput.isNotBlank()) {
                                                viewModel.addManualNumber(uiState.recipientInput)
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 4.dp)
                                        .width(if (uiState.selectedRecipients.isEmpty()) 200.dp else 120.dp)
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                val canSend = (uiState.selectedRecipients.isNotEmpty() || uiState.recipientInput.isNotBlank()) &&
                        uiState.composerText.isNotBlank()

                ComposerBar(
                    text = uiState.composerText,
                    onTextChange = { viewModel.onComposerTextChanged(it) },
                    onSendClick = {
                        viewModel.sendFirstMessage { threadId, address ->
                            onMessageSent(threadId, address)
                        }
                    },
                    onAttachClick = {},
                    glassOpacity = settings.glassOpacity,
                    reducedTransparency = settings.reducedTransparency,
                    isSendEnabled = canSend
                )
            }
        ) { innerPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (uiState.suggestedContacts.isNotEmpty()) {
                    items(uiState.suggestedContacts, key = { "${it.id}_${it.phoneNumber}" }) { contact ->
                        ContactSuggestionRow(
                            contact = contact,
                            onClick = { viewModel.selectContact(contact) }
                        )
                    }
                } else if (uiState.recipientInput.isNotBlank()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(SquircleCardShape)
                                .background(colors.surfaceVariant.copy(alpha = 0.5f))
                                .applePressable { viewModel.addManualNumber(uiState.recipientInput) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Send to \"${uiState.recipientInput}\"",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipientChip(
    text: String,
    onRemove: () -> Unit
) {
    val colors = LocalSalimColors.current
    Row(
        modifier = Modifier
            .padding(end = 6.dp, bottom = 4.dp)
            .clip(SquirclePillShape)
            .background(colors.accent.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = colors.accent,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove",
            tint = colors.accent,
            modifier = Modifier
                .size(14.dp)
                .clickable(onClick = onRemove)
        )
    }
}

@Composable
private fun ContactSuggestionRow(
    contact: ContactItem,
    onClick: () -> Unit
) {
    val colors = LocalSalimColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(SquircleCardShape)
            .applePressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarView(
            name = contact.displayName,
            photoUri = contact.photoUri,
            size = 40.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontSize = 15.sp
            )
            Text(
                text = contact.phoneNumber,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}
