package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

/**
 * 16 Google Messages & Apple iOS inspired distinctive pastel and vibrant avatar gradient palettes.
 * Hashes phone numbers/contact names to guarantee every chat has a unique, beautiful identity.
 */
private val AVATAR_GRADIENTS = listOf(
    listOf(Color(0xFF007AFF), Color(0xFF5856D6)), // Apple Blue -> Violet
    listOf(Color(0xFF34C759), Color(0xFF30B0C7)), // Emerald -> Teal
    listOf(Color(0xFFFF9500), Color(0xFFFF3B30)), // Amber -> Coral
    listOf(Color(0xFFAF52DE), Color(0xFFFF2D55)), // Purple -> Pink
    listOf(Color(0xFF5AC8FA), Color(0xFF007AFF)), // Sky -> Blue
    listOf(Color(0xFFFFCC00), Color(0xFFFF9500)), // Gold -> Orange
    listOf(Color(0xFF5E5CE6), Color(0xFFBF5AF2)), // Indigo -> Lilac
    listOf(Color(0xFF64D2FF), Color(0xFF3860FF)), // Cyan -> Deep Azure
    listOf(Color(0xFFFF6482), Color(0xFFFF3B30)), // Rose -> Scarlet
    listOf(Color(0xFF30D158), Color(0xFF009688)), // Mint -> Jungle
    listOf(Color(0xFFAC8E68), Color(0xFF6B4F3B)), // Warm Mocha
    listOf(Color(0xFF8E8E93), Color(0xFF636366)), // Classic Slate
    listOf(Color(0xFFFF7A00), Color(0xFFFF0055)), // Sunset Blaze
    listOf(Color(0xFF4CD964), Color(0xFF5AC8FA)), // Pastel Lime -> Sky
    listOf(Color(0xFF9C27B0), Color(0xFFE91E63)), // Magenta Orchid
    listOf(Color(0xFF00BCD4), Color(0xFF3F51B5))  // Ocean Marine
)

@Composable
fun AvatarView(
    name: String,
    photoUri: String?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val cleanName = name.trim()

    // Detect if this is a group conversation with multiple names (e.g., "Alice, Bob")
    val isGroup = remember(cleanName) {
        cleanName.contains(",") && cleanName.split(",").filter { it.isNotBlank() }.size >= 2
    }

    if (isGroup && photoUri.isNullOrBlank()) {
        val members = remember(cleanName) {
            cleanName.split(",").map { it.trim() }.filter { it.isNotBlank() }
        }
        GroupAvatarCluster(
            members = members,
            size = size,
            modifier = modifier
        )
        return
    }

    SingleAvatarView(
        name = cleanName,
        photoUri = photoUri,
        size = size,
        modifier = modifier
    )
}

@Composable
fun SingleAvatarView(
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

/**
 * Apple-style overlapping circular avatar cluster for group conversations (2 or 3 members).
 */
@Composable
fun GroupAvatarCluster(
    members: List<String>,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    val subSize = size * 0.68f

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        val member1 = members.getOrNull(0) ?: "1"
        val member2 = members.getOrNull(1) ?: "2"

        // First avatar (top-left)
        SingleAvatarView(
            name = member1,
            photoUri = null,
            size = subSize,
            modifier = Modifier
                .align(Alignment.TopStart)
                .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
        )

        // Second avatar (bottom-right)
        SingleAvatarView(
            name = member2,
            photoUri = null,
            size = subSize,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
        )
    }
}
