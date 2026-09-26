package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        if (isScrolled) (base + 0.12f).coerceAtMost(0.96f) else 0.15f
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

/**
 * Apple-grade Liquid Glass Frost Button.
 * Features:
 * - Continuous squircle corner curvature
 * - Semi-translucent frosted glass body
 * - Specular rim highlight
 * - Instant contact-down spring physics via applePressable
 */
@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    icon: ImageVector? = null,
    text: String? = null,
    shape: Shape = SquircleButtonShape
) {
    val colors = LocalSalimColors.current
    val isDark = colors.isDark

    val containerColor = if (isPrimary) {
        colors.accent
    } else if (isDark) {
        Color(0xFF2C2C2E).copy(alpha = 0.65f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.75f)
    }

    val contentColor = if (isPrimary) {
        Color.White
    } else {
        colors.textPrimary
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp, minWidth = 44.dp)
            .clip(shape)
            .background(containerColor, shape)
            .applePressable(enabled = enabled, onClick = onClick)
            .drawWithContent {
                drawContent()
                // Top specular highlight
                val highlightColor = if (isPrimary) {
                    Color.White.copy(alpha = 0.35f)
                } else if (isDark) {
                    Color.White.copy(alpha = 0.12f)
                } else {
                    Color.White.copy(alpha = 0.85f)
                }
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, highlightColor, Color.Transparent)
                    ),
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = if (text != null) 16.dp else 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = text,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (icon != null && text != null) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * Liquid Glass Segmented Tab Row.
 * Apple-style frosted segmented control container with round squircle curvature.
 */
@Composable
fun LiquidGlassTabRow(
    modifier: Modifier = Modifier,
    shape: Shape = SquirclePillShape,
    content: @Composable RowScope.() -> Unit
) {
    val colors = LocalSalimColors.current
    val isDark = colors.isDark

    val bg = if (isDark) {
        Color(0xFF1C1C1E).copy(alpha = 0.65f)
    } else {
        Color(0xFFE5E5EA).copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bg, shape)
            .padding(3.dp)
            .drawWithContent {
                drawContent()
                // Subtle frosted container rim
                val rimColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.5f)
                drawLine(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, rimColor, Color.Transparent)),
                    start = Offset(0f, 0.5f),
                    end = Offset(size.width, 0.5f),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

/**
 * Liquid Glass Tab Item.
 */
@Composable
fun RowScope.LiquidGlassTab(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    shape: Shape = SquirclePillShape
) {
    val colors = LocalSalimColors.current
    val isDark = colors.isDark

    val tabBg = if (isSelected) {
        if (isDark) Color(0xFF2C2C2E) else Color.White
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        colors.textPrimary
    } else {
        colors.textSecondary
    }

    Box(
        modifier = modifier
            .weight(1f)
            .defaultMinSize(minHeight = 36.dp)
            .clip(shape)
            .background(tabBg, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                fontSize = 13.sp,
                maxLines = 1
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                LiquidGlassBadge(count = badgeCount)
            }
        }
    }
}

/**
 * Liquid Glass Tabular Badge / Counter Chip.
 */
@Composable
fun LiquidGlassBadge(
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalSalimColors.current.accent
) {
    Box(
        modifier = modifier
            .clip(SquirclePillShape)
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

/**
 * Liquid Glass Frost Filter Chip.
 * Apple-grade round liquid glass frost pill with specular gloss and badge support.
 */
@Composable
fun LiquidGlassChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
    shape: Shape = CircleShape
) {
    val colors = LocalSalimColors.current
    val isDark = colors.isDark

    val chipBg = if (isSelected) {
        colors.accent
    } else if (isDark) {
        Color(0xFF2C2C2E).copy(alpha = 0.60f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.85f)
    }

    val chipText = if (isSelected) {
        Color.White
    } else {
        colors.textPrimary
    }

    val rimColor = if (isSelected) {
        Color.White.copy(alpha = 0.35f)
    } else if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.White.copy(alpha = 0.95f)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .clip(shape)
            .background(chipBg, shape)
            .border(
                width = 0.8.dp,
                color = rimColor,
                shape = shape
            )
            .applePressable(onClick = onClick)
            .drawWithContent {
                drawContent()
                val specularColor = if (isSelected) {
                    Color.White.copy(alpha = 0.40f)
                } else if (isDark) {
                    Color.White.copy(alpha = 0.16f)
                } else {
                    Color.White.copy(alpha = 0.85f)
                }
                drawLine(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, specularColor, Color.Transparent)),
                    start = Offset(0f, 1f),
                    end = Offset(size.width, 1f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = chipText,
                fontSize = 13.5.sp
            )
            if (badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White.copy(alpha = 0.28f) else colors.accent)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
