package com.example.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import com.example.data.preferences.AmbientBackgroundMode
import com.example.ui.theme.LocalSalimColors
import org.intellij.lang.annotations.Language

// AGSL Multi-Octave FBM Domain-Warping Organic Smoke/Cloud Shader
@Language("AGSL")
private const val AMBIENT_AGSL_SRC = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float4 iColor1;
    uniform float4 iColor2;
    uniform float iIntensity;

    float hash(float2 p) {
        float3 p3 = fract(float3(p.xyx) * 0.1031);
        p3 += dot(p3, p3.yzx + 33.33);
        return fract((p3.x + p3.y) * p3.z);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        return lerp(lerp(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
                    lerp(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
    }

    float fbm(float2 p) {
        float v = 0.0;
        float a = 0.5;
        float2 shift = float2(100.0);
        float2x2 rot = float2x2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
        for (int i = 0; i < 4; ++i) {
            v += a * noise(p);
            p = rot * p * 2.0 + shift;
            a *= 0.5;
        }
        return v;
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord.xy / iResolution.xy;
        
        // Domain warping with multi-octave flow
        float2 q = float2(fbm(uv + 0.08 * iTime), fbm(uv + float2(1.0, 1.0)));
        float2 r = float2(fbm(uv + 1.0 * q + float2(1.7, 9.2) + 0.12 * iTime),
                          fbm(uv + 1.0 * q + float2(8.3, 2.8) + 0.09 * iTime));
        
        float f = fbm(uv + r * 1.5);
        
        half4 col = lerp(iColor1, iColor2, clamp(f * f * 2.5, 0.0, 1.0));
        col.a *= (f * iIntensity);
        return col;
    }
"""

@Composable
fun AgslAmbientBackground(
    mode: AmbientBackgroundMode,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (mode == AmbientBackgroundMode.OFF) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val salimColors = LocalSalimColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_motion")
    
    val animProgress by if (reducedMotion) {
        remember { androidx.compose.runtime.mutableFloatStateOf(1.0f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 60f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 35000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "time"
        )
    }

    val intensity = if (mode == AmbientBackgroundMode.SUBTLE) 0.14f else 0.28f
    val accent = salimColors.accent
    val isDark = salimColors.isDark

    Box(modifier = modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val runtimeShader = remember {
                try {
                    RuntimeShader(AMBIENT_AGSL_SRC)
                } catch (e: Throwable) {
                    null
                }
            }

            if (runtimeShader != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    runtimeShader.setFloatUniform("iResolution", size.width, size.height)
                    runtimeShader.setFloatUniform("iTime", animProgress)
                    val c1 = accent.copy(alpha = if (isDark) 0.35f else 0.22f).toArgb()
                    val c2 = (if (isDark) Color(0xFF1E2235) else Color(0xFFE2E9F8)).toArgb()
                    runtimeShader.setColorUniform("iColor1", c1)
                    runtimeShader.setColorUniform("iColor2", c2)
                    runtimeShader.setFloatUniform("iIntensity", intensity)

                    drawRect(brush = ShaderBrush(runtimeShader))
                }
            } else {
                FallbackCanvasAtmosphere(isDark, accent, intensity, animProgress)
            }
        } else {
            FallbackCanvasAtmosphere(isDark, accent, intensity, animProgress)
        }

        content()
    }
}

@Composable
private fun FallbackCanvasAtmosphere(
    isDark: Boolean,
    accent: Color,
    intensity: Float,
    time: Float
) {
    val phase = (time % 10f) / 10f
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val cx1 = w * (0.3f + 0.2f * kotlin.math.sin(phase * 6.28f).toFloat())
        val cy1 = h * (0.25f + 0.15f * kotlin.math.cos(phase * 6.28f).toFloat())

        val cx2 = w * (0.7f - 0.2f * kotlin.math.cos(phase * 4.0f).toFloat())
        val cy2 = h * (0.75f - 0.15f * kotlin.math.sin(phase * 4.0f).toFloat())

        val brush1 = Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = intensity * (if (isDark) 0.35f else 0.22f)),
                Color.Transparent
            ),
            center = Offset(cx1, cy1),
            radius = w * 0.85f
        )
        drawRect(brush = brush1)

        val brush2 = Brush.radialGradient(
            colors = listOf(
                (if (isDark) Color(0xFF22283A) else Color(0xFFD6E4FF)).copy(alpha = intensity * 0.25f),
                Color.Transparent
            ),
            center = Offset(cx2, cy2),
            radius = w * 0.9f
        )
        drawRect(brush = brush2)
    }
}
