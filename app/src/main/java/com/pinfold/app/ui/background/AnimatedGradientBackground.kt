package com.pinfold.app.ui.background

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.sin

internal const val GRADIENT_LOOP_MS = 31_000L
internal const val GRADIENT_BLOB_COUNT = 5
internal const val GRADIENT_BLUR_DP = 80
internal const val GRADIENT_DRIFT = 0.14f
internal const val GRADIENT_FADE_MS = 800
internal const val DEFAULT_GRADIENT_ID = "ACID_FOREST"

private val BLOB_LOOP_MS = longArrayOf(GRADIENT_LOOP_MS, 37_000L, 43_000L, 47_000L, 53_000L)
private const val GRADIENT_OVERLAY_ALPHA = 0.25f

@Serializable
data class GradientPreset(
    val id: String,
    val name: String,
    val baseColor: Int,
    val blobColors: List<Int>,
    val seed: Long,
)

private fun builtIn(index: Int, name: String, colors: List<Int>): GradientPreset = GradientPreset(
    id = name.uppercase().replace(' ', '_'),
    name = name,
    baseColor = colors.last(),
    blobColors = colors,
    seed = 1_000L + index,
)

internal val BuiltInGradients: List<GradientPreset> = listOf(
    builtIn(0, "Sunset Plum", listOf(0xFFFCCBF0.toInt(), 0xFFFF5A57.toInt(), 0xFFE02F75.toInt(), 0xFF6700A3.toInt())),
    builtIn(1, "Ember Night", listOf(0xFFFF5A57.toInt(), 0xFFE02F75.toInt(), 0xFF6700A3.toInt(), 0xFF050C38.toInt())),
    builtIn(2, "Deep Violet", listOf(0xFF6700A3.toInt(), 0xFF1B2062.toInt(), 0xFF050C38.toInt())),
    builtIn(3, "Neon Orchid", listOf(0xFFFFE5F1.toInt(), 0xFFF042FF.toInt(), 0xFF7226FF.toInt())),
    builtIn(4, "Electric Dusk", listOf(0xFF7226FF.toInt(), 0xFF160078.toInt(), 0xFF010030.toInt())),
    builtIn(5, "Aqua Bloom", listOf(0xFF87F5F5.toInt(), 0xFFFFE5F1.toInt(), 0xFFF042FF.toInt())),
    builtIn(6, "Candy Lime", listOf(0xFFFFDEFD.toInt(), 0xFFFF4C79.toInt(), 0xFFDDFF00.toInt())),
    builtIn(7, "Ultra Night", listOf(0xFFFFDEFD.toInt(), 0xFF4712C4.toInt(), 0xFF062738.toInt())),
    builtIn(8, "Acid Forest", listOf(0xFFDDFF00.toInt(), 0xFF00484F.toInt(), 0xFF062738.toInt())),
    builtIn(9, "Deep Teal", listOf(0xFF00484F.toInt(), 0xFF062738.toInt())),
)

private data class GradientBlob(val x: Float, val y: Float, val radius: Float, val color: Int)

@Composable
fun AnimatedGradientBackground(
    preset: GradientPreset,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false,
) {
    val blobs = remember(preset) {
        gradientBlobSeeds(preset.seed, GRADIENT_BLOB_COUNT).mapIndexed { index, position ->
            GradientBlob(
                x = position.first,
                y = position.second,
                radius = 0.52f + hash01(preset.seed, 60 + index) * 0.26f,
                color = preset.blobColors[index % preset.blobColors.size],
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "gradient")
    val phases = if (reduceMotion) {
        emptyList()
    } else {
        blobs.mapIndexed { index, _ ->
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = BLOB_LOOP_MS[index % BLOB_LOOP_MS.size],
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "gradientBlob$index",
            )
        }
    }
    val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val effect = if (modern) {
        Modifier.graphicsLayer { scaleX = 1.2f; scaleY = 1.2f }.blur(GRADIENT_BLUR_DP.dp)
    } else {
        Modifier.graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }
    }
    val alphaScale = if (modern) 0.85f else 0.58f
    Box(modifier.then(effect)) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    listOf(Color(preset.baseColor), Color(darkenArgb(preset.baseColor))),
                ),
            )
            blobs.forEachIndexed { index, blob ->
                val phase = if (reduceMotion) 0.5f else phases[index].value
                val drift = phase * 2f - 1f
                val cx = (blob.x + drift * GRADIENT_DRIFT * (1f + (index % 3) * 0.25f)) * size.width
                val cy = (blob.y - drift * GRADIENT_DRIFT * (0.85f + (index % 2) * 0.35f)) * size.height
                val radius = blob.radius * maxOf(size.width, size.height)
                val center = Offset(cx, cy)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(blob.color).copy(alpha = alphaScale * (0.72f + 0.28f * phase)),
                            Color(blob.color).copy(alpha = alphaScale * 0.42f),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }
            drawRect(Color.Black.copy(alpha = GRADIENT_OVERLAY_ALPHA))
        }
    }
}

@Composable
fun GradientThumbnail(preset: GradientPreset, size: Dp, modifier: Modifier = Modifier) {
    val positions = remember(preset) { gradientBlobSeeds(preset.seed, GRADIENT_BLOB_COUNT) }
    Canvas(modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height
        drawRect(
            brush = Brush.linearGradient(
                listOf(Color(preset.baseColor), Color(darkenArgb(preset.baseColor))),
            ),
        )
        positions.forEachIndexed { index, position ->
            val color = preset.blobColors[index % preset.blobColors.size]
            val center = Offset(position.first * width, position.second * height)
            val radius = (0.52f + hash01(preset.seed, 60 + index) * 0.26f) * maxOf(width, height)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(color).copy(alpha = 0.8f),
                        Color(color).copy(alpha = 0.35f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }
        drawRect(Color.Black.copy(alpha = GRADIENT_OVERLAY_ALPHA))
    }
}

private fun hash01(seed: Long, salt: Int): Float {
    var x = seed xor (salt.toLong() * 0x9E3779B97F4A7C15L)
    x = (x xor (x ushr 30)) * 0xBF58476D1CE4E5B9L
    x = (x xor (x ushr 27)) * 0x94D049BB133111EBL
    x = x xor (x ushr 31)
    return ((x ushr 40) and 0xFFFFL).toFloat() / 0xFFFFL.toFloat()
}

private fun darkenArgb(argb: Int, factor: Float = 0.45f): Int {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return (0xFF shl 24) or
        ((r * factor).toInt().coerceIn(0, 255) shl 16) or
        ((g * factor).toInt().coerceIn(0, 255) shl 8) or
        (b * factor).toInt().coerceIn(0, 255)
}

internal fun gradientBlobSeeds(seed: Long, count: Int): List<Pair<Float, Float>> {
    if (count <= 0) return emptyList()
    return (0 until count).map { index ->
        val angle = (index + 0.5f) / count * 2f * Math.PI.toFloat() +
            (hash01(seed, index * 2 + 11) - 0.5f) * 0.9f
        val radius = 0.16f + hash01(seed, index * 2 + 12) * 0.24f
        (0.5f + cos(angle) * radius).coerceIn(0f, 1f) to
            (0.5f + sin(angle) * radius).coerceIn(0f, 1f)
    }
}

private fun hsvToArgb(hue: Float, saturation: Float, value: Float): Int {
    val sector = ((hue % 360f + 360f) % 360f) / 60f
    val s = saturation.coerceIn(0f, 1f)
    val v = value.coerceIn(0f, 1f)
    val c = v * s
    val x = c * (1f - kotlin.math.abs(sector % 2f - 1f))
    val m = v - c
    val rgb = when (sector.toInt()) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val r = ((rgb.first + m) * 255f).toInt().coerceIn(0, 255)
    val g = ((rgb.second + m) * 255f).toInt().coerceIn(0, 255)
    val b = ((rgb.third + m) * 255f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

internal fun generatePreset(seed: Long): GradientPreset {
    val baseHue = hash01(seed, 1) * 360f
    val saturation = 0.62f + hash01(seed, 2) * 0.20f
    val offsets = floatArrayOf(0f, 24f, -24f, 150f, 180f)
    val colors = (0 until 4).map { index ->
        val offset = offsets[(hash01(seed, 10 + index) * offsets.size).toInt().coerceIn(offsets.indices)] +
            (hash01(seed, 20 + index) - 0.5f) * 10f
        hsvToArgb(
            baseHue + offset,
            (saturation + (hash01(seed, 30 + index) - 0.5f) * 0.08f).coerceIn(0.4f, 0.85f),
            0.52f + hash01(seed, 40 + index) * 0.26f,
        )
    }
    val baseColor = hsvToArgb(
        baseHue,
        0.45f + hash01(seed, 5) * 0.25f,
        0.07f + hash01(seed, 6) * 0.05f,
    )
    val id = "GEN_" + (seed and 0xFFFFFFFFL).toString(16).uppercase()
    return GradientPreset(
        id = id,
        name = "Generated #${(seed and 0x7FFFL).toInt()}",
        baseColor = baseColor,
        blobColors = colors,
        seed = seed,
    )
}
