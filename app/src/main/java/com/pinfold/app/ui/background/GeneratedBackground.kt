package com.pinfold.app.ui.background

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.pinfold.app.ui.glass.theme.clear
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val CREAM = Color(0xFFFFF4DF)

internal fun effectiveBackgroundFps(fps: Int): Int = if (fps <= 0) 30 else fps.coerceIn(1, 60)

@Composable
private fun rememberBackgroundProgress(
    loop: Int,
    running: Boolean,
    style: String,
    hue: Float,
): androidx.compose.runtime.State<Float> = if (running) {
    key(loop, running, style, hue) {
        val transition = rememberInfiniteTransition(label = "background")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(loop, easing = LinearEasing), RepeatMode.Restart),
            label = "loop",
        )
    }
} else {
    remember { mutableStateOf(0f) }
}

@Composable
fun GeneratedBackground(
    modifier: Modifier = Modifier,
    loopMs: Int = 40_000,
    animate: Boolean = true,
    style: String = "PETALS",
    hue: Float = 220f,
    dim: Float = 0.2f,
    shape: String = "SOFT",
    source: String = "PALETTE",
    fps: Int = 30,
    paused: Boolean = false,
) {
    val running = animate && !paused
    val loop = loopMs.coerceIn(1000, 3_600_000)
    val rate = rememberUpdatedState(effectiveBackgroundFps(fps))
    val progressKey = rememberUpdatedState("$loop|$running|$style|$hue")
    val raw by rememberBackgroundProgress(loop, running, style, hue)
    val t by remember {
        derivedStateOf {
            progressKey.value
            val r = rate.value
            (raw * r).roundToInt().toFloat() / r
        }
    }
    val palette = if (source == "WALLPAPER") {
        (clear.wall + clear.blooms.map { it.first }).distinct().ifEmpty { listOf(CREAM, CREAM, CREAM, CREAM) }
    } else {
        val h = hue.coerceIn(0f, 360f)
        listOf(
            Color.hsv(h, 0.64f, 0.98f),
            Color.hsv((h + 28f) % 360f, 0.72f, 0.84f),
            Color.hsv((h + 312f) % 360f, 0.74f, 0.52f),
            Color.hsv((h + 186f) % 360f, 0.62f, 0.25f),
        )
    }
    val soft = shape != "SHARP"
    val wobble = if (shape == "ORGANIC") 1.6f else 1f
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (!w.isFinite() || !h.isFinite() || w <= 0f || h <= 0f) return@Canvas
        drawRect(Brush.linearGradient(listOf(palette.last(), palette[palette.size / 2]), start = Offset(w, 0f), end = Offset(0f, h)))
        when (style) {
            "AURORA" -> aurora(t, palette, soft, wobble)
            "MESH" -> mesh(t, palette, soft, wobble)
            "WAVES" -> waves(t, palette, soft, wobble)
            else -> petals(t, palette, soft, wobble)
        }
        drawRect(Color.Black.copy(alpha = dim.coerceIn(0f, 0.85f)))
    }
}

private fun DrawScope.petals(t: Float, palette: List<Color>, soft: Boolean, wobble: Float) {
    val cx = size.width / 2f
    val cy = size.height * 0.46f
    val reach = maxOf(size.width, size.height).coerceAtLeast(0f)
    for (i in 0 until 7) {
        val phase = i.toFloat() / 7f
        val angle = 2 * Math.PI.toFloat() * (phase + t * 0.18f * wobble)
        val pulse = 1f + 0.14f * sin(2 * Math.PI * (t + phase)).toFloat() * wobble
        val radius = (reach * (0.36f + 0.16f * phase) * pulse).coerceAtLeast(0f)
        val centre = Offset(cx + cos(angle) * radius * 0.42f, cy + sin(angle) * radius * 0.42f)
        val c = palette[i % palette.size]
        val colours = if (soft) listOf(c.copy(alpha = 0.86f), c.copy(alpha = 0.34f), Color.Transparent)
        else listOf(c, c.copy(alpha = 0.8f), Color.Transparent)
        drawCircle(Brush.radialGradient(colours, center = centre, radius = radius), radius = radius, center = centre, blendMode = BlendMode.SrcOver)
    }
}

private fun DrawScope.aurora(t: Float, palette: List<Color>, soft: Boolean, wobble: Float) {
    val origin = Offset(0f, size.height)
    val reach = maxOf(size.width, size.height).coerceAtLeast(0f)
    for (i in 0 until 5) {
        val phase = i.toFloat() / 5f
        val wave = sin(2 * Math.PI * (t + phase)).toFloat() * wobble
        val angle = Math.PI / 4f * (phase * 2f) + wave * 0.12f
        val radius = (reach * (0.65f + 0.28f * phase + 0.06f * cos(2 * Math.PI * (t + phase)).toFloat())).coerceAtLeast(0f)
        val centre = origin + Offset(cos(angle).toFloat() * radius * 0.28f, -sin(angle).toFloat() * radius * 0.28f)
        val c = palette[i % palette.size]
        val ramp = if (soft) listOf(c.copy(alpha = 0.82f), c.copy(alpha = 0.5f), Color.Transparent)
        else listOf(c, c.copy(alpha = 0.72f), Color.Transparent)
        drawRect(Brush.radialGradient(ramp, center = centre, radius = radius), blendMode = BlendMode.SrcOver)
    }
}

private fun DrawScope.mesh(t: Float, palette: List<Color>, soft: Boolean, wobble: Float) {
    val reach = maxOf(size.width, size.height).coerceAtLeast(0f)
    for (row in 0..2) {
        for (col in 0..2) {
            val i = row * 3 + col
            val driftX = 0.06f * sin(2 * Math.PI * (t + i * 0.11f)).toFloat() * wobble
            val driftY = 0.06f * cos(2 * Math.PI * (t + i * 0.17f)).toFloat() * wobble
            val centre = Offset(size.width * ((col + 0.5f) / 3f + driftX), size.height * ((row + 0.5f) / 3f + driftY))
            val c = palette[i % palette.size]
            val radius = (reach * 0.5f).coerceAtLeast(0f)
            drawCircle(
                Brush.radialGradient(
                    if (soft) listOf(c.copy(alpha = 0.78f), c.copy(alpha = 0.28f), Color.Transparent)
                    else listOf(c, c.copy(alpha = 0.7f), Color.Transparent),
                    center = centre,
                    radius = radius,
                ),
                radius = radius,
                center = centre,
                blendMode = BlendMode.SrcOver,
            )
        }
    }
}

private fun DrawScope.waves(t: Float, palette: List<Color>, soft: Boolean, wobble: Float) {
    val bands = 6
    for (i in 0 until bands) {
        val phase = i.toFloat() / bands
        val offset = sin(2 * PI.toFloat() * (t + phase)).toFloat() * size.height * 0.05f * wobble
        val y = size.height * (i + 0.5f) / bands + offset
        val band = (size.height / bands * 1.4f).coerceAtLeast(0f)
        val c = palette[i % palette.size]
        drawRect(
            Brush.verticalGradient(
                if (soft) listOf(Color.Transparent, c.copy(alpha = 0.72f), Color.Transparent)
                else listOf(Color.Transparent, c, Color.Transparent),
                startY = y - band,
                endY = y + band,
            ),
            topLeft = Offset(0f, y - band),
            size = Size(size.width, band * 2f),
            blendMode = BlendMode.SrcOver,
        )
    }
    drawCircle(
        Brush.radialGradient(listOf(CREAM.copy(alpha = 0.4f), Color.Transparent)),
        radius = maxOf(size.width, size.height).coerceAtLeast(0f) * 0.6f,
        center = Offset(size.width * 0.8f, size.height * 0.15f),
        blendMode = BlendMode.Plus,
    )
}
