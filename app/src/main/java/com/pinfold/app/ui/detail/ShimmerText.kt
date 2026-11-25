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

internal const val TEXT_SHIMMER_DURATION_MS = 2200
internal const val TEXT_SHIMMER_BAND_FRACTION = 0.55f
internal const val TEXT_SHIMMER_EDGE_ALPHA = 0.75f
internal const val TEXT_SHIMMER_BAND_ALPHA = 0.95f

internal fun textShimmerColors(base: Color): List<Color> = listOf(
    base.copy(alpha = TEXT_SHIMMER_EDGE_ALPHA),
    base,
    Color.White.copy(alpha = TEXT_SHIMMER_BAND_ALPHA),
    base,
    base.copy(alpha = TEXT_SHIMMER_EDGE_ALPHA),
)

internal fun textShimmerSpan(fraction: Float, widthPx: Float): Pair<Float, Float> {
    val t = fraction.coerceIn(0f, 1f)
    val width = widthPx.coerceAtLeast(0f)
    val band = maxOf(1f, width * TEXT_SHIMMER_BAND_FRACTION)
    val start = -band + t * (width + band)
    return start to start + band
}

@Composable
internal fun rememberTextShimmerBrush(base: Color, widthPx: Float, reduceMotion: Boolean): Brush {
    val fraction = if (reduceMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(TEXT_SHIMMER_DURATION_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        ).value
    }
    val (startX, endX) = textShimmerSpan(fraction, widthPx)
    return Brush.linearGradient(
        colors = textShimmerColors(base),
        start = Offset(startX, 0f),
        end = Offset(endX, 0f),
    )
}
