package com.pinfold.app.ui.detail

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pinfold.app.ui.glass.theme.Candy

internal const val TEXT_SHIMMER_DURATION_MS = 2200
internal const val TEXT_SHIMMER_BAND_FRACTION = 0.55f
internal const val TEXT_SHIMMER_EDGE_ALPHA = 0.9f
internal const val TEXT_SHIMMER_BAND_ALPHA = 1f

internal fun shimmerDurationMs(speed: String): Int = when (speed) {
    "SLOW" -> 3600
    "NORMAL" -> TEXT_SHIMMER_DURATION_MS
    "FAST" -> 1200
    else -> TEXT_SHIMMER_DURATION_MS
}

internal fun shimmerBandColors(color: String, customArgb: Long = 0xFFFFC21A): List<Color> = when (color) {
    "GREY" -> listOf(Color(0xFF9E9E9E))
    "WHITE" -> listOf(Color.White)
    "BLACK" -> listOf(Color.Black)
    "DARK_WHITE_GREY" -> listOf(Color(0xFF1A1A1A), Color.White, Color(0xFF9E9E9E))
    "CUSTOM" -> listOf(Color(customArgb.toInt()))
    else -> listOf(Candy.Lemon)
}

internal fun shimmerBandColor(color: String): Color = shimmerBandColors(color).first()

internal fun textShimmerColors(base: Color, bandColors: List<Color> = listOf(Candy.Lemon)): List<Color> {
    val band = bandColors.ifEmpty { listOf(Candy.Lemon) }
    val edges = listOf(base.copy(alpha = TEXT_SHIMMER_EDGE_ALPHA), base)
    return edges + band.map { it.copy(alpha = TEXT_SHIMMER_BAND_ALPHA) } + edges.reversed()
}

internal fun shimmerBrushColors(
    base: Color,
    enabled: Boolean,
    color: String,
    customArgb: Long = 0xFFFFC21A,
): List<Color> = if (enabled) {
    textShimmerColors(base, shimmerBandColors(color, customArgb))
} else {
    listOf(base, base)
}

internal fun textShimmerSpan(fraction: Float, widthPx: Float): Pair<Float, Float> {
    val t = fraction.coerceIn(0f, 1f)
    val width = widthPx.coerceAtLeast(0f)
    val band = maxOf(1f, width * TEXT_SHIMMER_BAND_FRACTION)
    val start = -band + t * (width + band)
    return start to start + band
}

@Composable
internal fun rememberTextShimmerBrush(
    base: Color,
    widthPx: Float,
    reduceMotion: Boolean,
    enabled: Boolean = true,
    color: String = "LEMON",
    customArgb: Long = 0xFFFFC21A,
    speed: String = "NORMAL",
): Brush {
    val colors = shimmerBrushColors(base, enabled, color, customArgb)
    if (!enabled) return Brush.linearGradient(colors)
    val fraction = if (reduceMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(shimmerDurationMs(speed), easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        ).value
    }
    val (startX, endX) = textShimmerSpan(fraction, widthPx)
    return Brush.linearGradient(
        colors = colors,
        start = Offset(startX, 0f),
        end = Offset(endX, 0f),
    )
}
