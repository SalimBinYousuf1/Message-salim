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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import com.example.data.preferences.ThemeMode
import com.example.ui.theme.LocalSalimColors
import com.example.ui.theme.LocalSalimThemeMode
import org.intellij.lang.annotations.Language

/**
 * 4th Theme Mode: Salim
 * Exact AGSL Shader implementation for chromatic ambient fluid surface
 */
@Language("AGSL")
private const val ASGL_SHADER = """
uniform float2 resolution;
uniform float time;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);

    f = f * f * (3.0 - 2.0 * f);

    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));

    return mix(
        mix(a, b, f.x),
        mix(c, d, f.x),
        f.y
    );
}

float fbm(float2 p, float seed) {
    float value = 0.0;
    float amplitude = 0.55;

    float2 shift = float2(
        17.1 + seed * 3.7,
        9.2 + seed * 5.1
    );

    value += noise(p + shift) * amplitude;

    p = p * 2.03 + shift;
    value += noise(p) * 0.28;

    p = p * 2.07 - shift * 0.43;
    value += noise(p) * 0.12;

    p = p * 2.11 + shift * 0.27;
    value += noise(p) * 0.05;

    return value;
}

float cloudDensity(
    float2 p,
    float seed,
    float stretch,
    float phase
) {
    float2 warp = float2(
        fbm(
            p * 0.72 +
                float2(
                    phase * 0.17,
                    -phase * 0.11
                ),
            seed + 4.0
        ),
        fbm(
            p * 0.72 +
                float2(
                    -phase * 0.13,
                    phase * 0.19
                ),
            seed + 8.0
        )
    );

    float2 warped = p + (warp - 0.5) * 1.65;

    warped.x *= stretch;

    float field = fbm(
        warped +
            float2(
                phase * 0.07,
                -phase * 0.05
            ),
        seed
    );

    float detail = noise(
        warped * 5.5 +
            phase * 0.13 +
            seed
    );

    float density = smoothstep(
        0.39,
        0.70,
        field + (detail - 0.5) * 0.16
    );

    return density * (0.78 + 0.22 * detail);
}

float2 redCenter(float t) {
    return float2(
        0.50 + 0.57 * sin(t * 0.071),
        0.50 + 0.62 * cos(t * 0.053 + 1.2)
    );
}

float2 blueCenter(float t) {
    return float2(
        0.50 + 0.68 * cos(t * 0.047 + 2.4),
        0.50 + 0.58 * sin(t * 0.081)
    );
}

float2 greenCenter(float t) {
    return float2(
        0.50 + 0.62 * sin(t * 0.059 + 4.1),
        0.50 + 0.68 * sin(t * 0.039 + 0.7)
    );
}

float2 yellowCenter(float t) {
    return float2(
        0.50 + 0.72 * cos(t * 0.033 + 5.0),
        0.50 + 0.56 * cos(t * 0.067 + 2.1)
    );
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / resolution;

    float aspect = resolution.x /
        max(resolution.y, 1.0);

    float2 p = uv;
    p.x *= aspect;

    float t = time;

    float2 rc = redCenter(t);
    float2 bc = blueCenter(t);
    float2 gc = greenCenter(t);
    float2 yc = yellowCenter(t);

    rc.x *= aspect;
    bc.x *= aspect;
    gc.x *= aspect;
    yc.x *= aspect;

    float red = cloudDensity(
        (p - rc) * 2.05,
        1.7,
        0.78 + 0.12 * sin(t * 0.11),
        t * 0.31
    );

    float blue = cloudDensity(
        (p - bc) * 2.55,
        8.3,
        1.05 + 0.16 * cos(t * 0.09),
        t * 0.27
    );

    float green = cloudDensity(
        (p - gc) * 2.25,
        15.6,
        0.67 + 0.18 * sin(t * 0.13),
        t * 0.22
    );

    float yellow = cloudDensity(
        (p - yc) * 3.10,
        22.4,
        1.30 + 0.20 * cos(t * 0.07),
        t * 0.36
    );

    float3 color = float3(
        0.985,
        0.988,
        1.0
    );

    color = mix(
        color,
        float3(0.92, 0.015, 0.02),
        red * 0.24
    );

    color = mix(
        color,
        float3(0.015, 0.10, 0.95),
        blue * 0.24
    );

    color = mix(
        color,
        float3(0.03, 0.72, 0.08),
        green * 0.24
    );

    color = mix(
        color,
        float3(0.95, 0.68, 0.015),
        yellow * 0.20
    );

    float total = clamp(
        red * 0.34 +
            blue * 0.34 +
            green * 0.34 +
            yellow * 0.30,
        0.0,
        0.86
    );

    color = mix(
        color,
        color * (1.0 - total * 0.08),
        0.35
    );

    return half4(
        half3(clamp(color, 0.0, 1.0)),
        1.0
    );
}
"""

@Composable
fun SalimShaderBackground(
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "salim_shader_time")
    val rawTime by if (reducedMotion) {
        remember { androidx.compose.runtime.mutableFloatStateOf(10f) }
    } else {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 600f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 180000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "time"
        )
    }

    // Gentle, calm time scaling so the chromatic clouds glide soothingly
    val time = rawTime * 0.12f

    Box(modifier = modifier.fillMaxSize()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val runtimeShader = remember {
                try {
                    RuntimeShader(ASGL_SHADER)
                } catch (e: Throwable) {
                    null
                }
            }

            if (runtimeShader != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = Color(0xFFFAFBFC))
                    runtimeShader.setFloatUniform("resolution", size.width, size.height)
                    runtimeShader.setFloatUniform("time", time)
                    drawRect(brush = ShaderBrush(runtimeShader))
                }
            } else {
                FallbackSalimAtmosphere(time)
            }
        } else {
            FallbackSalimAtmosphere(time)
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

@Composable
private fun FallbackSalimAtmosphere(time: Float) {
    val phase = (time % 20f) / 20f
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base pristine luminous white
        drawRect(color = Color(0xFFFAFBFC))

        val cxRed = w * (0.50f + 0.25f * kotlin.math.sin(phase * 6.28f).toFloat())
        val cyRed = h * (0.50f + 0.25f * kotlin.math.cos(phase * 5.10f + 1.2f).toFloat())

        val cxBlue = w * (0.50f + 0.30f * kotlin.math.cos(phase * 4.20f + 2.4f).toFloat())
        val cyBlue = h * (0.50f + 0.25f * kotlin.math.sin(phase * 7.10f).toFloat())

        val cxGreen = w * (0.50f + 0.25f * kotlin.math.sin(phase * 5.40f + 4.1f).toFloat())
        val cyGreen = h * (0.50f + 0.30f * kotlin.math.sin(phase * 3.80f + 0.7f).toFloat())

        val cxYellow = w * (0.50f + 0.32f * kotlin.math.cos(phase * 3.20f + 5.0f).toFloat())
        val cyYellow = h * (0.50f + 0.24f * kotlin.math.cos(phase * 6.00f + 2.1f).toFloat())

        // Red cloud
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFE81515).copy(alpha = 0.12f), Color.Transparent),
                center = Offset(cxRed, cyRed),
                radius = w * 0.85f
            )
        )
        // Blue cloud
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF1540EB).copy(alpha = 0.12f), Color.Transparent),
                center = Offset(cxBlue, cyBlue),
                radius = w * 0.90f
            )
        )
        // Green cloud
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF0CB824).copy(alpha = 0.12f), Color.Transparent),
                center = Offset(cxGreen, cyGreen),
                radius = w * 0.80f
            )
        )
        // Yellow cloud
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF2AD14).copy(alpha = 0.10f), Color.Transparent),
                center = Offset(cxYellow, cyYellow),
                radius = w * 0.95f
            )
        )
    }
}

// AGSL Multi-Octave FBM Domain-Warping Organic Smoke/Cloud Shader for other themes
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
    themeMode: ThemeMode = LocalSalimThemeMode.current,
    content: @Composable BoxScope.() -> Unit
) {
    if (themeMode == ThemeMode.SALIM) {
        SalimShaderBackground(
            modifier = modifier,
            reducedMotion = reducedMotion,
            content = content
        )
        return
    }

    val salimColors = LocalSalimColors.current

    if (mode == AmbientBackgroundMode.OFF) {
        Box(modifier = modifier.fillMaxSize().background(salimColors.background)) {
            content()
        }
        return
    }

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
                    drawRect(color = salimColors.background)
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
                FallbackCanvasAtmosphere(isDark, accent, intensity, animProgress, salimColors.background)
            }
        } else {
            FallbackCanvasAtmosphere(isDark, accent, intensity, animProgress, salimColors.background)
        }

        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun FallbackCanvasAtmosphere(
    isDark: Boolean,
    accent: Color,
    intensity: Float,
    time: Float,
    backgroundColor: Color
) {
    val phase = (time % 10f) / 10f
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = backgroundColor)
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
