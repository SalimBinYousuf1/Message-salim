package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SimCard
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.SimCardInfo
import com.example.telephony.SmsLengthCalculator
import com.example.ui.theme.LocalSalimColors

@Composable
fun ComposerBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onScheduleClick: () -> Unit = {},
    onAttachClick: () -> Unit,
    onStartVoiceRecord: () -> Unit = {},
    onStopVoiceRecordAndSend: () -> Unit = {},
    onCancelVoiceRecord: () -> Unit = {},
    isRecordingVoice: Boolean = false,
    recordingDurationSec: Int = 0,
    recordingAmplitude: Float = 0f,
    attachedMediaUri: Uri? = null,
    onRemoveAttachment: () -> Unit = {},
    availableSims: List<SimCardInfo> = emptyList(),
    selectedSim: SimCardInfo? = null,
    onToggleSim: () -> Unit = {},
    isSendEnabled: Boolean = text.isNotBlank() || attachedMediaUri != null,
    glassOpacity: Float = 0.82f,
    reducedTransparency: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = LocalSalimColors.current
    val context = LocalContext.current

    val lengthInfo = remember(text) {
        SmsLengthCalculator.calculate(text)
    }

    LiquidGlassSurface(
        glassOpacity = glassOpacity,
        reducedTransparency = reducedTransparency,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Attachment Preview Bar
            if (attachedMediaUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(SquircleButtonShape)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(attachedMediaUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Attachment preview",
                            modifier = Modifier.size(68.dp),
                            contentScale = ContentScale.Crop
                        )

                        IconButton(
                            onClick = onRemoveAttachment,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(20.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attachment",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // SIM Selector and SMS Counter Pill Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (availableSims.size > 1 && selectedSim != null) {
                    Row(
                        modifier = Modifier
                            .clip(SquircleButtonShape)
                            .background(colors.surfaceVariant.copy(alpha = 0.6f))
                            .clickable(onClick = onToggleSim)
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = "Switch SIM",
                            tint = colors.accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${selectedSim.displayName} (${selectedSim.carrierName})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // SMS Segments and Characters remaining indicator
                AnimatedVisibility(
                    visible = text.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = if (lengthInfo.segmentCount > 1) {
                            "${lengthInfo.remainingChars} / ${lengthInfo.segmentCount}"
                        } else {
                            "${lengthInfo.remainingChars}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isRecordingVoice) {
                // Audio Recording Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onCancelVoiceRecord,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Recording",
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(SquirclePillShape)
                            .background(colors.surface)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pulsing recording indicator
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        val mins = recordingDurationSec / 60
                        val secs = recordingDurationSec % 60
                        Text(
                            text = String.format("%d:%02d", mins, secs),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Waveform amplitude visualizer
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val barCount = 12
                            for (i in 0 until barCount) {
                                val h = ((recordingAmplitude * 24.dp.value) + (i % 3) * 4).coerceIn(4f, 24f)
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(h.dp)
                                        .clip(SquircleButtonShape)
                                        .background(colors.accent)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Send voice note with instant contact feedback
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                            .applePressable(onClick = onStopVoiceRecordAndSend),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Voice Note",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            } else {
                // Standard Text Composer Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Attachment Button (+) with apple press feedback
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceVariant.copy(alpha = 0.7f))
                            .applePressable(onClick = onAttachClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Attach media",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Field Capsule with continuous squircle curvature
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 38.dp, max = 120.dp)
                            .clip(SquirclePillShape)
                            .background(colors.surface)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = "Text Message",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary.copy(alpha = 0.8f),
                                fontSize = 15.sp
                            )
                        }

                        BasicTextField(
                            value = text,
                            onValueChange = onTextChange,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(colors.accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (text.isEmpty() && attachedMediaUri == null) {
                        // Voice Note Mic Button (Apple style)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceVariant.copy(alpha = 0.7f))
                                .applePressable(onClick = onStartVoiceRecord),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Record Voice Note",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    } else {
                        // Send Arrow Button with long-press to schedule and spring press feedback
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isSendEnabled) colors.accent else colors.surfaceVariant.copy(alpha = 0.5f))
                                .applePressable(
                                    enabled = isSendEnabled,
                                    onClick = onSendClick,
                                    onLongClick = onScheduleClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (isSendEnabled) Color.White else colors.textTertiary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
