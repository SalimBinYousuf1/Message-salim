package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.abs

val SquircleCornerRadius = 18.dp
val SquircleBubbleShape = RoundedCornerShape(18.dp)
val SquircleCardShape = RoundedCornerShape(16.dp)
val SquircleButtonShape = RoundedCornerShape(14.dp)

private val AVATAR_GRADIENTS = listOf(
    listOf(Color(0xFF8E8E93), Color(0xFF636366)), // Classic slate
    listOf(Color(0xFF007AFF), Color(0xFF5856D6)), // Apple Blue -> Violet
    listOf(Color(0xFF34C759), Color(0xFF30B0C7)), // Emerald -> Teal
    listOf(Color(0xFFFF9500), Color(0xFFFF3B30)), // Amber -> Coral
    listOf(Color(0xFFAF52DE), Color(0xFFFF2D55)), // Purple -> Pink
    listOf(Color(0xFF5AC8FA), Color(0xFF007AFF)), // Sky -> Blue
    listOf(Color(0xFFFFCC00), Color(0xFFFF9500))  // Gold -> Orange
)

@Composable
fun AvatarView(
    name: String,
    photoUri: String?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val cleanName = name.trim()
    val initials = if (cleanName.isNotBlank() && cleanName.any { it.isLetter() }) {
        val parts = cleanName.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        } else {
            "${parts[0].first().uppercaseChar()}"
        }
    } else {
        "#"
    }

    val hash = abs(cleanName.hashCode())
    val gradientColors = AVATAR_GRADIENTS[hash % AVATAR_GRADIENTS.size]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photoUri)
                    .crossfade(true)
                    .build(),
                contentDescription = cleanName,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            val fontSize = (size.value * 0.42f).sp
            Text(
                text = initials,
                color = Color.White,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
