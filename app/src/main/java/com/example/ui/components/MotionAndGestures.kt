package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Apple iOS Messages / Google Messages style two-way swipeable conversation item container.
 * - Swipe Left: Apple Red "Delete" + Apple Orange "Archive"
 * - Swipe Right: Apple Blue "Pin/Unpin" + Apple Green "Mark as Read/Unread"
 * - Spring physics settling with haptic tactile bumps.
 */
@Composable
fun SwipeableConversationRow(
    isPinned: Boolean,
    isArchived: Boolean,
    isUnread: Boolean,
    onPinToggle: () -> Unit,
    onReadToggle: () -> Unit,
    onArchiveToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    if (!enabled) {
        Box(modifier = modifier) { content() }
        return
    }

    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val actionWidthPx = with(density) { 156.dp.toPx() }
    val maxDragPx = with(density) { 220.dp.toPx() }
    val offsetX = remember { Animatable(0f) }

    fun performHaptic() {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (e: Exception) {
            // ignore
        }
    }

    fun close() {
        scope.launch {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Revealed Background Actions
        val currentOffset = offsetX.value

        if (currentOffset > 0f) {
            // Right-swipe actions: Apple Blue (Pin) and Apple Green (Read/Unread)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp)),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pin / Unpin Button (Apple Blue)
                SwipeActionButton(
                    icon = if (isPinned) Icons.Default.PinDrop else Icons.Default.PushPin,
                    label = if (isPinned) "Unpin" else "Pin",
                    backgroundColor = Color(0xFF007AFF),
                    onClick = {
                        performHaptic()
                        close()
                        onPinToggle()
                    }
                )

                // Read / Unread Button (Apple Green)
                SwipeActionButton(
                    icon = if (isUnread) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                    label = if (isUnread) "Read" else "Unread",
                    backgroundColor = Color(0xFF34C759),
                    onClick = {
                        performHaptic()
                        close()
                        onReadToggle()
                    }
                )
            }
        } else if (currentOffset < 0f) {
            // Left-swipe actions: Apple Orange (Archive) and Apple Red (Delete)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp)),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Archive / Unarchive Button (Apple Orange)
                SwipeActionButton(
                    icon = if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    label = if (isArchived) "Unarchive" else "Archive",
                    backgroundColor = Color(0xFFFF9500),
                    onClick = {
                        performHaptic()
                        close()
                        onArchiveToggle()
                    }
                )

                // Delete Button (Apple Red)
                SwipeActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    backgroundColor = Color(0xFFFF3B30),
                    onClick = {
                        performHaptic()
                        close()
                        onDelete()
                    }
                )
            }
        }

        // Foreground Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { performHaptic() },
                        onDragEnd = {
                            val target = when {
                                offsetX.value > actionWidthPx * 0.5f -> actionWidthPx
                                offsetX.value < -actionWidthPx * 0.5f -> -actionWidthPx
                                else -> 0f
                            }
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        },
                        onDragCancel = { close() },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetX.value + dragAmount).coerceIn(-maxDragPx, maxDragPx)
                            scope.launch { offsetX.snapTo(newOffset) }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeActionButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(76.dp)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Apple-grade instant touch feedback with interruptible spring physics:
 * - Scale-down begins the instant of contact (0 delay).
 * - Release triggers natural spring overshoot and settling.
 * - Interruptible if touched again mid-settle.
 */
fun Modifier.applePressable(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    if (!enabled) return@composed this

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(enabled) {
            detectTapGestures(
                onPress = {
                    // Instant contact response
                    val pressJob = scope.launch {
                        scale.animateTo(
                            targetValue = pressedScale,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessHigh
                            )
                        )
                    }
                    val released = tryAwaitRelease()
                    pressJob.cancel()
                    // Settle spring with gentle overshoot
                    scope.launch {
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }
                },
                onTap = {
                    onClick?.invoke()
                },
                onLongPress = {
                    onLongClick?.invoke()
                }
            )
        }
}
