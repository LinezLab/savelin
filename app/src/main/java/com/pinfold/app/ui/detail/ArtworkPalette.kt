package com.pinfold.app.ui.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

internal const val PALETTE_SAMPLE_LIMIT = 96
internal const val BLOOM_COUNT = 5
internal const val BLOOM_ALPHA = 0.40f
internal const val GRAIN_ALPHA = 0.035f

internal const val AMBIENT_SCALE = 1.5f
internal const val AMBIENT_BLUR_DP = 90f
internal const val AMBIENT_ALPHA = 1.0f
internal const val BOTTOM_FADE_DP = 200f
internal const val BOTTOM_FADE_ALPHA = 0.38f
internal const val SCRIM_TOP_ALPHA = 0.16f
internal const val SCRIM_BOTTOM_ALPHA = 0.14f

internal const val HALO_SCALE = 1.46f
internal const val HALO_BLUR_DP = 58f
internal const val HALO_OVERSHOOT_DP = 210f
internal const val HALO_TAIL_DP = 120f
internal const val HALO_ALPHA = 0.95f

data class ArtworkPalette(
    val base: Color,
    val dominant: Color,
    val accents: List<Color>,
    val buckets: Int,
)

internal data class ToneBloom(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float,
)

internal fun Color.hex(): String = String.format(Locale.US, "#%08X", this.toArgb())

internal fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val high = maxOf(rf, gf, bf)
    val low = minOf(rf, gf, bf)
    val delta = high - low
    val raw = when {
        delta == 0f -> 0f
        high == rf -> ((gf - bf) / delta) % 6f * 60f
        high == gf -> ((bf - rf) / delta + 2f) * 60f
        else -> ((rf - gf) / delta + 4f) * 60f
    }
    val hue = if (raw < 0f) raw + 360f else raw
    val saturation = if (high == 0f) 0f else delta / high
    return floatArrayOf(hue, saturation, high)
}

internal fun isDim(color: Color): Boolean =
    color.alpha < 0.12f || maxOf(color.red, color.green, color.blue) < 0.14f

internal fun blendColors(from: Color, to: Color, fraction: Float): Color =
    lerp(from, to, fraction.coerceIn(0f, 1f))

/** Pick a regular lattice of source pixels, capped so palette work stays cheap on a phone. */
internal fun downsampleArgb(
    pixels: IntArray,
    width: Int,
    height: Int,
    maxSide: Int = PALETTE_SAMPLE_LIMIT,
): IntArray {
    if (width < 1 || height < 1 || pixels.size < width * height) return intArrayOf()
    val stride = max(1, maxOf(width, height) / max(1, maxSide))
    val outWidth = max(1, (width + stride - 1) / stride)
    val outHeight = max(1, (height + stride - 1) / stride)
    val out = IntArray(outWidth * outHeight)
    for (y in 0 until outHeight) {
        val sourceY = min(height - 1, y * stride) * width
        for (x in 0 until outWidth) {
            out[y * outWidth + x] = pixels[sourceY + min(width - 1, x * stride)]
        }
    }
    return out
}

private fun liftColor(color: Color, fallbackHue: Float): Color {
    val r = (color.red * 255f).toInt().coerceIn(0, 255)
    val g = (color.green * 255f).toInt().coerceIn(0, 255)
    val b = (color.blue * 255f).toInt().coerceIn(0, 255)
    val hsv = rgbToHsv(r, g, b)
    if (color.alpha >= 0.95f && maxOf(r, g, b) >= 76) return color
    return Color.hsv(if (hsv[1] < 0.12f) fallbackHue else hsv[0], 0.62f, 0.78f)
}

/**
 * Generic colour quantisation: opaque pixels are sorted into hue bands and a low/high saturation
 * tier, plus one shared neutral band. Each band is averaged and bands are ranked by population with
 * a small saturation bonus, so dark or greyscale covers still produce usable accents.
 */
internal fun extractPalette(pixels: IntArray): ArtworkPalette {
    data class Bin(var count: Int = 0, var red: Long = 0L, var green: Long = 0L, var blue: Long = 0L)

    val bands = 16
    val buckets = Array(bands) { arrayOf(Bin(), Bin()) }
    val neutral = Bin()
    var opaque = 0
    var totalRed = 0L
    var totalGreen = 0L
    var totalBlue = 0L

    for (argb in pixels) {
        if ((argb ushr 24) < 40) continue
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        opaque++
        totalRed += r
        totalGreen += g
        totalBlue += b
        val hsv = rgbToHsv(r, g, b)
        val target = if (hsv[1] < 0.16f || hsv[2] < 0.16f || hsv[2] > 0.985f) {
            neutral
        } else {
            val band = min(bands - 1, (hsv[0] / (360f / bands)).toInt())
            buckets[band][if (hsv[1] < 0.46f) 0 else 1]
        }
        target.count++
        target.red += r
        target.green += g
        target.blue += b
    }

    if (opaque == 0) return fallbackPalette(17L)

    val ranked = mutableListOf<Pair<Float, Color>>()
    fun rank(bin: Bin, weight: Float) {
        if (bin.count == 0) return
        val red = (bin.red / bin.count).toInt().coerceIn(0, 255)
        val green = (bin.green / bin.count).toInt().coerceIn(0, 255)
        val blue = (bin.blue / bin.count).toInt().coerceIn(0, 255)
        val hsv = rgbToHsv(red, green, blue)
        ranked.add(bin.count.toFloat() * (weight + hsv[1]) to Color(red, green, blue))
    }
    buckets.forEach { row -> row.forEach { rank(it, 0.44f) } }
    rank(neutral, 0.34f)
    ranked.sortByDescending { it.first }

    val dominant = ranked.firstOrNull()?.second ?: Color(
        (totalRed / opaque).toInt(),
        (totalGreen / opaque).toInt(),
        (totalBlue / opaque).toInt(),
    )
    val dominantHsv = rgbToHsv(
        (dominant.red * 255f).toInt(),
        (dominant.green * 255f).toInt(),
        (dominant.blue * 255f).toInt(),
    )
    val hue = if (dominantHsv[1] < 0.12f) 212f else dominantHsv[0]
    val saturation = dominantHsv[1]
    val value = dominantHsv[2]
    val base = Color.hsv(
        hue,
        saturation.coerceIn(0.30f, 0.85f),
        value.coerceIn(0.55f, 0.92f),
    )

    val accents = mutableListOf<Color>()
    ranked.take(3).forEach { accents.add(liftColor(it.second, hue)) }
    listOf(30f, 174f, 264f).forEachIndexed { index, offset ->
        accents.add(
            Color.hsv(
                (hue + offset) % 360f,
                (saturation * (1f - index * 0.05f)).coerceIn(0.40f, 1f),
                (value * (1.08f + index * 0.06f)).coerceIn(0.56f, 1f),
            ),
        )
    }
    return ArtworkPalette(base, dominant, accents.distinct().take(6), ranked.size)
}

/** Stable multi-colour ambient for a bookmark with no stored preview. */
internal fun fallbackPalette(seed: Long): ArtworkPalette {
    var hash = 17L
    for (ch in "savelin-ambient-$seed") hash = hash * 31L + ch.code
    val hue = Math.floorMod(hash, 360L).toFloat()
    val accents = listOf(
        Color.hsv(hue, 0.74f, 0.94f),
        Color.hsv((hue + 138f) % 360f, 0.66f, 0.86f),
        Color.hsv((hue + 242f) % 360f, 0.58f, 0.80f),
        Color.hsv((hue + 46f) % 360f, 0.50f, 0.74f),
    )
    return ArtworkPalette(Color.hsv(hue, 0.50f, 0.72f), accents.first(), accents, 0)
}

/** Soft, overlapping pools of light placed at fixed, visually balanced positions. */
internal fun paletteBlooms(palette: ArtworkPalette): List<ToneBloom> {
    val accents = palette.accents
    if (accents.isEmpty()) return emptyList()
    val spots = listOf(
        0.14f to 0.14f,
        0.86f to 0.22f,
        0.34f to 0.70f,
        0.76f to 0.82f,
        0.52f to 0.44f,
    )
    return spots.mapIndexed { index, (x, y) ->
        ToneBloom(
            x = x,
            y = y,
            radius = 0.44f + (index % 3) * 0.11f,
            color = accents[index % accents.size],
            alpha = if (index < 2) BLOOM_ALPHA else BLOOM_ALPHA * 0.60f,
        )
    }
}

internal fun haloTailStart(haloHeight: Float): Float =
    ((haloHeight - HALO_TAIL_DP) / max(1f, haloHeight)).coerceIn(0f, 1f)

internal fun haloAlpha(tailStart: Float, progress: Float): Float = when {
    progress <= tailStart -> 1f
    progress >= 1f -> 0f
    else -> (1f - progress) / (1f - tailStart).coerceAtLeast(0.0001f)
}

/** Smooth bottom diffusion ramp used by the image-derived fade. */
internal fun bottomFade(progress: Float): Float {
    val x = progress.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}
