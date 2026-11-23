package com.pinfold.app.ui.details

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class ArtworkPalette(
    val base: Color,
    val dominant: Color,
    val colors: List<Color>,
)

data class ArtworkAmbient(
    val key: String,
    val blurred: ImageBitmap,
    val palette: ArtworkPalette,
)

/** Pure RGB to HSV: keeps palette extraction testable without an Android runtime. */
internal fun rgbToHsv(r: Int, g: Int, b: Int): FloatArray {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val maxChannel = maxOf(rf, gf, bf)
    val minChannel = minOf(rf, gf, bf)
    val delta = maxChannel - minChannel
    val rawHue = when {
        delta == 0f -> 0f
        maxChannel == rf -> ((gf - bf) / delta) % 6f * 60f
        maxChannel == gf -> ((bf - rf) / delta + 2f) * 60f
        else -> ((rf - gf) / delta + 4f) * 60f
    }
    val hue = if (rawHue < 0f) rawHue + 360f else rawHue
    val saturation = if (maxChannel == 0f) 0f else delta / maxChannel
    return floatArrayOf(hue, saturation, maxChannel)
}

/**
 * Buckets the downsampled artwork by hue and saturation, then averages each bucket. Every returned
 * colour is derived from the pixels: an orange image yields amber/burnt orange, a blue one yields
 * deep blue/cyan, and so on. No palette is hardcoded.
 */
internal fun extractArtworkPalette(pixels: IntArray): ArtworkPalette {
    data class Bin(var count: Int = 0, var r: Int = 0, var g: Int = 0, var b: Int = 0)

    val bins = Array(12) { Array(3) { Bin() } }
    var opaque = 0
    var totalR = 0
    var totalG = 0
    var totalB = 0
    for (argb in pixels) {
        if ((argb ushr 24) < 40) continue
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        opaque++
        totalR += r
        totalG += g
        totalB += b
        val hsv = rgbToHsv(r, g, b)
        if (hsv[1] < 0.16f || hsv[2] < 0.16f || hsv[2] > 0.985f) continue
        val hueBin = min(11, (hsv[0] / 30f).toInt())
        val satBin = min(2, (hsv[1] * 3f).toInt())
        val bin = bins[hueBin][satBin]
        bin.count++
        bin.r += r
        bin.g += g
        bin.b += b
    }

    val ranked = mutableListOf<Pair<Float, Color>>()
    for (hueBin in bins.indices) {
        for (satBin in bins[hueBin].indices) {
            val bin = bins[hueBin][satBin]
            if (bin.count == 0) continue
            val r = bin.r / bin.count
            val g = bin.g / bin.count
            val b = bin.b / bin.count
            val hsv = rgbToHsv(r, g, b)
            ranked.add((bin.count.toFloat() * (0.35f + hsv[1])) to Color(r, g, b))
        }
    }
    ranked.sortByDescending { it.first }

    val dominant = ranked.firstOrNull()?.second ?: run {
        val count = max(1, opaque)
        Color(totalR / count, totalG / count, totalB / count)
    }
    val dominantHsv = rgbToHsv(
        (dominant.red * 255f).toInt().coerceIn(0, 255),
        (dominant.green * 255f).toInt().coerceIn(0, 255),
        (dominant.blue * 255f).toInt().coerceIn(0, 255),
    )
    val hue = dominantHsv[0]
    val saturation = dominantHsv[1]
    val value = dominantHsv[2]
    val base = Color.hsv(
        hue,
        (saturation * 0.85f).coerceIn(0.06f, 1f),
        (value * 0.22f).coerceIn(0.05f, 0.30f),
    )
    val accents = mutableListOf<Color>()
    ranked.take(3).forEach { accents.add(it.second) }
    listOf(24f, 336f, 52f, 308f).forEachIndexed { index, offset ->
        accents.add(
            Color.hsv(
                (hue + offset) % 360f,
                (saturation * (1f - index * 0.06f)).coerceIn(0.15f, 1f),
                (value * (1.08f + index * 0.08f)).coerceIn(0.28f, 1f),
            ),
        )
    }
    return ArtworkPalette(base, dominant, accents.distinct().take(6))
}

private val ambientCache = LinkedHashMap<String, ArtworkAmbient>()

@Synchronized
private fun cachedAmbient(cacheKey: String, loader: () -> ArtworkAmbient?): ArtworkAmbient? {
    ambientCache[cacheKey]?.let { return it }
    val loaded = loader() ?: return null
    ambientCache[cacheKey] = loaded
    if (ambientCache.size > 24) ambientCache.remove(ambientCache.keys.first())
    return loaded
}

private fun decodeScaled(path: String, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension * 2) sample *= 2
    val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }) ?: return null
    val scale = min(1f, maxDimension.toFloat() / maxOf(decoded.width, decoded.height))
    if (scale >= 1f) return decoded
    val width = max(1, (decoded.width * scale).toInt())
    val height = max(1, (decoded.height * scale).toInt())
    return Bitmap.createScaledBitmap(decoded, width, height, true)
}

private fun loadAmbient(path: String, key: String): ArtworkAmbient? {
    val small = decodeScaled(path, 64) ?: return null
    val pixels = IntArray(small.width * small.height)
    small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
    val palette = extractArtworkPalette(pixels)
    val soft = decodeScaled(path, 112) ?: small
    return ArtworkAmbient(key, soft.asImageBitmap(), palette)
}

/** Downsamples once off the main thread and caches the result per bookmark. */
@Composable
fun rememberArtworkAmbient(imagePath: String?, key: Any?): ArtworkAmbient? {
    var ambient by remember(key, imagePath) { mutableStateOf<ArtworkAmbient?>(null) }
    LaunchedEffect(key, imagePath) {
        val path = imagePath
        ambient = if (path == null) null else withContext(Dispatchers.IO) {
            val cacheKey = "$key|$path"
            cachedAmbient(cacheKey) { loadAmbient(path, cacheKey) }
        }
    }
    return ambient
}

/**
 * Apple Music-style ambient presentation: base colour, enlarged downsampled artwork, artwork-derived
 * radial blooms, a dark readability gradient and a faint deterministic grain. No player controls.
 */
@Composable
fun ArtworkAmbientBackground(ambient: ArtworkAmbient, modifier: Modifier = Modifier) {
    val palette = ambient.palette
    Box(modifier.fillMaxSize().background(palette.base)) {
        Image(
            bitmap = ambient.blurred,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().drawBehind {
                val w = size.width
                val h = size.height
                val radius = maxOf(w, h) * 0.85f
                palette.colors.forEachIndexed { index, source ->
                    val angle = index.toFloat() / palette.colors.size * 2f * Math.PI.toFloat()
                    val center = Offset(
                        w * (0.5f + 0.32f * cos(angle)),
                        h * (0.44f + 0.30f * sin(angle)),
                    )
                    val tint = source.copy(alpha = if (index == 0) 0.66f else 0.46f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(tint, tint.copy(alpha = 0f)),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                        blendMode = BlendMode.Screen,
                    )
                }
            },
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.14f),
                        Color.Black.copy(alpha = 0.06f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.34f),
                        Color.Black.copy(alpha = 0.72f),
                    ),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().drawBehind {
                val random = java.util.Random(ambient.key.hashCode().toLong())
                repeat(420) {
                    val x = random.nextFloat() * size.width
                    val y = random.nextFloat() * size.height
                    drawCircle(
                        color = Color.White.copy(alpha = 0.018f + random.nextFloat() * 0.014f),
                        radius = 0.6f + random.nextFloat() * 0.9f,
                        center = Offset(x, y),
                    )
                }
            },
        )
    }
}
