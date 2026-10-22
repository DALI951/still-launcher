package com.niagaraclone.launcher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.WallpaperTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance, hardware-accelerated 60/120fps live ambient motion layer.
 * Adds silky, cinematic life to curated photo wallpapers with zero video decoding battery drain.
 */
@Composable
fun LiveAmbientWallpaper(
    settings: LauncherSettings,
    modifier: Modifier = Modifier
) {
    if (!settings.liveWallpaperEnabled) return

    val transition = rememberInfiniteTransition(label = "ambient_wallpaper_transition")

    // Slow continuous circular phase: 0..2PI over 18 seconds
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Secondary harmonic phase: 0..2PI over 26 seconds
    val secondaryPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "secondaryPhase"
    )

    // Gentle breathing pulse: 0.88..1.12 over 4.5 seconds
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Stardust static offsets for cosmic themes
    val stardustSeeds = remember {
        List(24) { index ->
            val angle = (index * 15f) * (Math.PI / 180f)
            val distRatio = 0.20f + (index % 7) * 0.11f
            val speed = 0.4f + (index % 5) * 0.25f
            val baseAlpha = 0.15f + (index % 4) * 0.18f
            Triple(angle.toFloat(), distRatio, Pair(speed, baseAlpha))
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        when (settings.wallpaperTheme) {
            WallpaperTheme.DEEP_SPACE -> {
                drawDeepSpaceBlackHole(phase, secondaryPhase, pulse, stardustSeeds)
            }
            WallpaperTheme.EMERALD_NIGHT -> {
                drawEmeraldAurora(phase, secondaryPhase, pulse)
            }
            WallpaperTheme.SOLAR_ECLIPSE -> {
                drawSolarEclipseCorona(phase, pulse)
            }
            WallpaperTheme.CYBERPUNK_DARK -> {
                drawCyberpunkNeonRain(phase, pulse)
            }
            WallpaperTheme.BLACK_DESERT, WallpaperTheme.DUNE_TWILIGHT -> {
                drawDuneTwilightAtmosphere(phase, pulse, settings.wallpaperTheme)
            }
            else -> {
                // Subtle universal ambient breathing illumination for other themes
                drawSubtleAmbientGaze(phase, pulse, settings.wallpaperTheme)
            }
        }
    }
}

/**
 * DEEP SPACE: Accretion disk swirl, event horizon glow, and drifting stardust.
 */
private fun DrawScope.drawDeepSpaceBlackHole(
    phase: Float,
    secondaryPhase: Float,
    pulse: Float,
    seeds: List<Triple<Float, Float, Pair<Float, Float>>>
) {
    val center = Offset(size.width * 0.50f, size.height * 0.40f)
    val baseRadius = size.width * 0.36f * pulse

    // Outer cosmic sapphire accretion glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x353D6EB4),
                Color(0x1F2B487A),
                Color(0x0C12203A),
                Color.Transparent
            ),
            center = center,
            radius = baseRadius * 1.55f
        ),
        center = center,
        radius = baseRadius * 1.55f
    )

    // Inner ultraviolet relativistic swirl
    val swirlOffset = Offset(
        center.x + sin(phase) * (size.width * 0.025f),
        center.y + cos(phase) * (size.height * 0.015f)
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x406C429C),
                Color(0x22355088),
                Color.Transparent
            ),
            center = swirlOffset,
            radius = baseRadius * 0.95f
        ),
        center = swirlOffset,
        radius = baseRadius * 0.95f
    )

    // Drifting cosmic stardust particles
    seeds.forEach { (baseAngle, distRatio, dynamics) ->
        val (speed, baseAlpha) = dynamics
        val currentAngle = baseAngle + phase * speed * 0.35f
        val currentDist = baseRadius * distRatio * (0.92f + 0.16f * sin(secondaryPhase + baseAngle))
        val starX = center.x + cos(currentAngle) * currentDist
        val starY = center.y + sin(currentAngle) * (currentDist * 0.65f) // Flattened disk perspective

        val alpha = (baseAlpha * (0.6f + 0.4f * sin(phase * speed * 2f + baseAngle))).coerceIn(0.05f, 0.70f)
        val starRadius = (1.5f + (baseAngle % 2f) * 1.2f)

        drawCircle(
            color = Color(0xFFB4CFFF).copy(alpha = alpha),
            radius = starRadius,
            center = Offset(starX, starY)
        )
    }
}

/**
 * EMERALD NIGHT: Sinusoidal aurora borealis curtains drifting over the arctic fjord.
 */
private fun DrawScope.drawEmeraldAurora(
    phase: Float,
    secondaryPhase: Float,
    pulse: Float
) {
    val w = size.width
    val h = size.height

    // Curtain 1: Emerald glow undulating in upper atmospheric sky
    val path1 = Path()
    val baseY1 = h * 0.16f
    path1.moveTo(0f, 0f)
    path1.lineTo(0f, baseY1)

    val steps = 24
    for (i in 0..steps) {
        val x = (w / steps) * i
        val wave = sin((x / w) * 4f + phase) * 28.dp.toPx() +
                cos((x / w) * 2.5f - secondaryPhase) * 18.dp.toPx()
        path1.lineTo(x, (baseY1 + wave) * pulse)
    }
    path1.lineTo(w, 0f)
    path1.close()

    drawPath(
        path = path1,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x352ECC71),
                Color(0x2000E5FF),
                Color.Transparent
            ),
            startY = 0f,
            endY = h * 0.38f
        ),
        style = Fill
    )

    // Curtain 2: Secondary mist-cyan curtain weaving beneath
    val path2 = Path()
    val baseY2 = h * 0.24f
    path2.moveTo(0f, h * 0.06f)

    for (i in 0..steps) {
        val x = (w / steps) * i
        val wave = sin((x / w) * 5.2f - phase * 0.8f) * 22.dp.toPx() +
                sin((x / w) * 2f + secondaryPhase) * 14.dp.toPx()
        path2.lineTo(x, (baseY2 + wave))
    }
    path2.lineTo(w, h * 0.06f)
    path2.close()

    drawPath(
        path = path2,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color(0x2800BCD4),
                Color(0x182ECC71),
                Color.Transparent
            ),
            startY = h * 0.08f,
            endY = h * 0.44f
        ),
        style = Fill
    )

    // Fjord horizon water reflection shimmer
    val waterY = h * 0.72f
    val shimmerAlpha = (0.08f + 0.05f * sin(phase * 1.5f)).coerceIn(0.04f, 0.16f)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF2ECC71).copy(alpha = shimmerAlpha),
                Color.Transparent
            ),
            startY = waterY,
            endY = h * 0.92f
        ),
        topLeft = Offset(0f, waterY),
        size = androidx.compose.ui.geometry.Size(w, h * 0.20f)
    )
}

/**
 * SOLAR ECLIPSE: Coronal flare filaments undulating around the dark moon.
 */
private fun DrawScope.drawSolarEclipseCorona(
    phase: Float,
    pulse: Float
) {
    val center = Offset(size.width * 0.50f, size.height * 0.38f)
    val moonRadius = size.width * 0.24f

    // Outer coronal shimmer ring
    val outerRadius = moonRadius * (1.35f * pulse)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x40FFFFFF),
                Color(0x28D0D6E4),
                Color(0x0E8A98B0),
                Color.Transparent
            ),
            center = center,
            radius = outerRadius
        ),
        center = center,
        radius = outerRadius
    )

    // Inner bright coronal fringe
    val fringeRadius = moonRadius * (1.14f + 0.04f * sin(phase * 3f))
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x55FFFFFF),
                Color(0x15FFFFFF),
                Color.Transparent
            ),
            center = center,
            radius = fringeRadius
        ),
        center = center,
        radius = fringeRadius
    )
}

/**
 * CYBERPUNK DARK: Ambient neon pulse on the rainy horizon.
 */
private fun DrawScope.drawCyberpunkNeonRain(
    phase: Float,
    pulse: Float
) {
    val w = size.width
    val h = size.height

    // Electric cyan horizon glow pulse
    val horizonY = h * 0.55f
    val glowAlpha = (0.16f + 0.08f * sin(phase * 1.8f)).coerceIn(0.08f, 0.26f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF00E5FF).copy(alpha = glowAlpha),
                Color(0xFF9B59B6).copy(alpha = glowAlpha * 0.55f),
                Color.Transparent
            ),
            center = Offset(w * 0.35f, horizonY),
            radius = w * 0.70f * pulse
        ),
        center = Offset(w * 0.35f, horizonY),
        radius = w * 0.70f * pulse
    )
}

/**
 * DUNE & BLACK DESERT: Ethereal twilight haze drifting softly across the sand ridges.
 */
private fun DrawScope.drawDuneTwilightAtmosphere(
    phase: Float,
    pulse: Float,
    theme: WallpaperTheme
) {
    val w = size.width
    val h = size.height

    val hazeY = h * 0.48f + sin(phase) * 14.dp.toPx()
    val hazeColor = if (theme == WallpaperTheme.DUNE_TWILIGHT) {
        Color(0xFFD49B6A)
    } else {
        Color(0xFFB0B3BA)
    }
    val hazeAlpha = (0.10f + 0.05f * cos(phase)).coerceIn(0.05f, 0.18f)

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                hazeColor.copy(alpha = hazeAlpha),
                Color.Transparent
            ),
            startY = hazeY - 80.dp.toPx(),
            endY = hazeY + 120.dp.toPx()
        ),
        topLeft = Offset(0f, hazeY - 80.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(w, 200.dp.toPx())
    )
}

/**
 * Fallback gentle ambient breathing illumination for other themes.
 */
private fun DrawScope.drawSubtleAmbientGaze(
    phase: Float,
    pulse: Float,
    theme: WallpaperTheme
) {
    val center = Offset(size.width * 0.50f, size.height * 0.30f)
    val accentColor = Color(theme.accentHex)
    val alpha = (0.09f + 0.04f * sin(phase)).coerceIn(0.04f, 0.14f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accentColor.copy(alpha = alpha),
                Color.Transparent
            ),
            center = center,
            radius = size.width * 0.85f * pulse
        ),
        center = center,
        radius = size.width * 0.85f * pulse
    )
}
