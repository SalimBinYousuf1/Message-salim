package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalSalimColors

/**
 * Liquid Glass Material Surface.
 * Implements Apple's physical glass material:
 * - Dynamic scroll reactivity: shifts toward opaque when content scrolls beneath, easing back to translucent at rest.
 * - Soft top specular highlight to simulate real refractive physical thickness.
 * - Ambient subtle darkened bottom edge for depth separation instead of hard divider lines.
 * - Respects user transparency preference and reduced-transparency accessibility setting.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    isScrolled: Boolean = false,
    glassOpacity: Float = 0.82f,
    reducedTransparency: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalSalimColors.current
    val isDark = colors.isDark

    // Spring-based reactive opacity adjustment when content scrolls underneath
    val targetAlpha = if (reducedTransparency) {
        1.0f
    } else {
        val base = glassOpacity.coerceIn(0.20f, 0.98f)
        if (isScrolled) (base + 0.14f).coerceAtMost(0.98f) else base
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glass_opacity_spring"
    )

    // Dynamic base glass tint
    val glassBaseColor = if (reducedTransparency) {
        colors.surface
    } else if (isDark) {
        Color(0xFF101012).copy(alpha = animatedAlpha)
    } else {
        Color(0xFFFFFFFF).copy(alpha = animatedAlpha)
    }

    // Specular highlight and soft rim colors
    val specularTopColor = if (isDark) {
        Color.White.copy(alpha = 0.15f * animatedAlpha)
    } else {
        Color.White.copy(alpha = 0.65f * animatedAlpha)
    }

    val ambientRimColor = if (isDark) {
        Color.White.copy(alpha = 0.06f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }

    val baseModifier = if (shape != null) {
        modifier
            .clip(shape)
            .background(glassBaseColor, shape)
    } else {
        modifier.background(glassBaseColor)
    }

    Box(
        modifier = baseModifier.drawWithContent {
            // Draw underlying content first
            drawContent()

            // Draw top physical specular highlight line (0.8dp)
            if (!reducedTransparency) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        0.0f to Color.Transparent,
                        0.15f to specularTopColor,
                        0.85f to specularTopColor,
                        1.0f to Color.Transparent
                    ),
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = 1.dp.toPx()
                )

                // Soft ambient darkened edge at the bottom for physical thickness & elevation
                drawLine(
                    brush = Brush.horizontalGradient(
                        0.0f to Color.Transparent,
                        0.1f to ambientRimColor,
                        0.9f to ambientRimColor,
                        1.0f to Color.Transparent
                    ),
                    start = Offset(0f, size.height - 0.5f),
                    end = Offset(size.width, size.height - 0.5f),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
        },
        content = content
    )
}
