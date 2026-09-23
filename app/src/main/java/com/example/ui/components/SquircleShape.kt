package com.example.ui.components

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Continuous curvature Squircle (Superellipse) shape.
 * Emulates Apple iOS continuous corner geometry using smooth cubic Bézier curves,
 * avoiding the sharp curvature transitions of standard rounded rectangles.
 */
class SquircleShape(
    private val cornerRadius: Dp,
    private val smoothness: Float = 0.82f
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { cornerRadius.toPx().coerceAtMost(minOf(size.width, size.height) / 2f) }
        val w = size.width
        val h = size.height

        if (radiusPx <= 0f) {
            return Outline.Rectangle(Rect(0f, 0f, w, h))
        }

        val path = Path().apply {
            val s = smoothness.coerceIn(0.5f, 1.0f)
            val l = radiusPx * (1f + s * 0.28f)
            val p = radiusPx * (1f - s * 0.44f)

            // Start top edge center
            moveTo(l, 0f)

            // Top-right squircle corner
            lineTo(w - l, 0f)
            cubicTo(
                w - p, 0f,
                w, p,
                w, l
            )

            // Right edge to bottom-right corner
            lineTo(w, h - l)
            cubicTo(
                w, h - p,
                w - p, h,
                w - l, h
            )

            // Bottom edge to bottom-left corner
            lineTo(l, h)
            cubicTo(
                p, h,
                0f, h - p,
                0f, h - l
            )

            // Left edge to top-left corner
            lineTo(0f, l)
            cubicTo(
                0f, p,
                p, 0f,
                l, 0f
            )

            close()
        }

        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SquircleShape) return false
        return cornerRadius == other.cornerRadius && smoothness == other.smoothness
    }

    override fun hashCode(): Int {
        var result = cornerRadius.hashCode()
        result = 31 * result + smoothness.hashCode()
        return result
    }
}

val SquircleLargeCardShape = SquircleShape(24.dp)
val SquircleCardShape = SquircleShape(20.dp)
val SquircleBubbleShape = SquircleShape(18.dp)
val SquircleButtonShape = SquircleShape(14.dp)
val SquirclePillShape = SquircleShape(28.dp)
val SquircleAvatarShape = SquircleShape(20.dp)
