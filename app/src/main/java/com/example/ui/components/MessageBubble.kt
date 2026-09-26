package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.telephony.OtpHelper
import com.example.ui.theme.LocalSalimColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: Message,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onLongClick: (Message) -> Unit,
    onMediaClick: (Message) -> Unit,
    onRetryClick: (Message) -> Unit,
    modifier: Modifier = Modifier,
    reaction: String? = null,
    onReactionClick: ((Message) -> Unit)? = null
) {
    val colors = LocalSalimColors.current
    val context = LocalContext.current
    val isOutgoing = !message.isIncoming

    // Apple-grade continuous squircle corner curvature
    val bubbleRadius = if (!isFirstInGroup && !isLastInGroup) 8.dp else 18.dp
    val bubbleShape = SquircleShape(bubbleRadius)

    val bubbleBg = if (isOutgoing) colors.bubbleOutgoing else colors.bubbleIncoming
    val textColor = if (isOutgoing) colors.bubbleTextOutgoing else colors.bubbleTextIncoming

    val timeString = formatMessageTime(message.date)

    val detectedOtp = remember(message.body) {
        if (message.isIncoming) OtpHelper.extractOtp(message.body) else null
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (isFirstInGroup) 4.dp else 1.5.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box {
            Box(
                modifier = Modifier
                    .widthIn(max = 290.dp)
                    .clip(bubbleShape)
                    .background(bubbleBg)
                    .applePressable(
                        pressedScale = 0.98f,
                        onClick = {
                            if (message.mediaUri != null) {
                                onMediaClick(message)
                            } else if (message.status == MessageStatus.FAILED && isOutgoing) {
                                onRetryClick(message)
                            }
                        },
                        onLongClick = { onLongClick(message) }
                    )
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = if (message.mediaUri != null) 6.dp else 9.dp,
                        bottom = 9.dp
                    )
            ) {
            Column {
                if (message.mediaUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(message.mediaUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .clip(SquircleButtonShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                val vcard = remember(message.body) { com.example.telephony.VCardHelper.parseVCard(message.body) }
                if (vcard != null) {
                    // Apple-style Contact Card Bubble
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SquircleButtonShape)
                            .background(if (isOutgoing) Color.White.copy(alpha = 0.22f) else colors.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isOutgoing) Color.White.copy(alpha = 0.35f) else colors.accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isOutgoing) Color.White else colors.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = vcard.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textColor
                            )
                            Text(
                                text = vcard.phone,
                                fontSize = 13.sp,
                                color = if (isOutgoing) Color.White.copy(alpha = 0.85f) else colors.textSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                try {
                                    context.startActivity(com.example.telephony.VCardHelper.createInsertContactIntent(vcard))
                                } catch (e: Exception) {
                                    val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${vcard.phone}"))
                                    context.startActivity(dial)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Save Contact",
                                tint = if (isOutgoing) Color.White else colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else if (message.body.isNotBlank()) {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOutgoing) Color.White.copy(alpha = 0.75f) else colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace // Tabular numerals
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.SENDING -> {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Sending",
                                    tint = Color.White.copy(alpha = 0.72f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.FAILED -> {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Failed",
                                    tint = Color(0xFFFFD1D1),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Apple signature floating Tapback reaction pill on bubble corner
        if (reaction != null) {
            Box(
                modifier = Modifier
                    .offset(
                        x = if (isOutgoing) (-8).dp else 8.dp,
                        y = (-10).dp
                    )
                    .zIndex(2f)
                    .clip(CircleShape)
                    .background(if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF))
                    .border(
                        width = 0.8.dp,
                        color = if (colors.isDark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .clickable { onReactionClick?.invoke(message) }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = reaction,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Quick OTP Copy Chip for incoming verification codes
    if (detectedOtp != null) {
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = if (isOutgoing) 0.dp else 16.dp, end = if (isOutgoing) 16.dp else 0.dp)
                .clip(CircleShape)
                .background(if (colors.isDark) Color(0xFF2C2C2E).copy(alpha = 0.75f) else Color(0xFFFFFFFF).copy(alpha = 0.90f))
                .border(0.8.dp, colors.accent.copy(alpha = 0.40f), CircleShape)
                .applePressable(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Verification Code", detectedOtp)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied code: $detectedOtp", Toast.LENGTH_SHORT).show()
                })
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Code",
                tint = colors.accent,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Copy Code: $detectedOtp",
                style = MaterialTheme.typography.labelMedium,
                color = colors.accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}
}

private fun formatMessageTime(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(date)
}

fun formatDateHeader(timestamp: Long): String {
    val now = java.util.Calendar.getInstance()
    val msgCal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }

    return when {
        now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
        now.get(java.util.Calendar.DAY_OF_YEAR) == msgCal.get(java.util.Calendar.DAY_OF_YEAR) -> "Today"

        now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) &&
        now.get(java.util.Calendar.DAY_OF_YEAR) - msgCal.get(java.util.Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"

        now.get(java.util.Calendar.YEAR) == msgCal.get(java.util.Calendar.YEAR) -> {
            SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(timestamp))
        }

        else -> {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
