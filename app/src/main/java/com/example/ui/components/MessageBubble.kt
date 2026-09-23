package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import com.example.ui.theme.LocalSalimColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    onLongClick: (Message) -> Unit,
    onMediaClick: (Message) -> Unit,
    onRetryClick: (Message) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSalimColors.current
    val context = LocalContext.current
    val isOutgoing = !message.isIncoming

    val bubbleShape = RoundedCornerShape(
        topStart = if (!isOutgoing && !isFirstInGroup) 6.dp else 18.dp,
        topEnd = if (isOutgoing && !isFirstInGroup) 6.dp else 18.dp,
        bottomStart = if (!isOutgoing && !isLastInGroup) 6.dp else 18.dp,
        bottomEnd = if (isOutgoing && !isLastInGroup) 6.dp else 18.dp
    )

    val bubbleBg = if (isOutgoing) colors.bubbleOutgoing else colors.bubbleIncoming
    val textColor = if (isOutgoing) colors.bubbleTextOutgoing else colors.bubbleTextIncoming

    val timeString = formatMessageTime(message.date)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = if (isFirstInGroup) 5.dp else 1.5.dp),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(bubbleShape)
                .background(bubbleBg)
                .combinedClickable(
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
                        contentDescription = "MMS Attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (message.body.isNotBlank()) {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor,
                        fontSize = 15.5.sp,
                        lineHeight = 21.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOutgoing) Color.White.copy(alpha = 0.72f) else colors.textSecondary,
                        fontSize = 10.5.sp
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

        if (isOutgoing && message.status == MessageStatus.FAILED) {
            Text(
                text = "Not Delivered • Tap to Retry",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFF3B30),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 2.dp, end = 4.dp)
                    .combinedClickable(onClick = { onRetryClick(message) })
            )
        }
    }
}

fun formatMessageTime(dateMillis: Long): String {
    if (dateMillis <= 0L) return ""
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}

fun formatDateHeader(dateMillis: Long): String {
    if (dateMillis <= 0L) return ""
    val sdf = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
    return sdf.format(Date(dateMillis))
}
