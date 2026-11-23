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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.diagnostics.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class DetailsBackgroundMode { ARTWORK, THEME, GENERATED }

/**
 * The dedicated Details background setting, resolved against whether the bookmark actually has a
 * usable local image. ARTWORK is the default (fresh install and the legacy PICTURE value); it
 * degrades to THEME only when there is no image to derive colours from. Pure and device-free.
 */
internal fun resolveDetailsBackgroundMode(setting: String?, hasImage: Boolean): DetailsBackgroundMode {
    val normalized = setting?.trim()?.uppercase().orEmpty()
    val requested = when (normalized) {
        "GENERATED" -> DetailsBackgroundMode.GENERATED
        "THEME", "SOLID" -> DetailsBackgroundMode.THEME
        else -> DetailsBackgroundMode.ARTWORK
    }
    return if (requested == DetailsBackgroundMode.ARTWORK && !hasImage) {
        DetailsBackgroundMode.THEME
    } else {
        requested
    }
}

/** Kept high on purpose: a near-zero wash was indistinguishable from the old page. */
internal const val BLOOM_ALPHA_PRIMARY = 0.78f
internal const val BLOOM_ALPHA_SECONDARY = 0.56f

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
 * Buckets the downsampled artwork by hue and saturation, then averages each bucket. Neutral and
 * very dark pixels go into their own bucket so a near-black or grayscale artwork still produces
 * at least three visible, derived accents instead of black.
 */
internal fun extractArtworkPalette(pixels: IntArray): ArtworkPalette {
    data class Bin(var count: Int = 0, var r: Int = 0, var g: Int = 0, var b: Int = 0)

    val bins = Array(12) { Array(3) { Bin() } }
    val neutral = Bin()
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
        if (hsv[1] < 0.16f || hsv[2] < 0.16f || hsv[2] > 0.985f) {
            neutral.count++
            neutral.r += r
            neutral.g += g
            neutral.b += b
            continue
        }
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
    if (neutral.count > 0) {
        val r = neutral.r / neutral.count
        val g = neutral.g / neutral.count
        val b = neutral.b / neutral.count
        ranked.add((neutral.count * 0.30f) to Color(r, g, b))
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
    val hue = if (dominantHsv[1] < 0.12f) 220f else dominantHsv[0]
    val saturation = dominantHsv[1]
    val value = dominantHsv[2]
    val base = Color.hsv(
        hue,
        saturation.coerceIn(0.18f, 1f),
        (value * 0.26f).coerceIn(0.12f, 0.34f),
    )
    val accents = mutableListOf<Color>()
    ranked.take(3).forEach { accents.add(it.second) }
    listOf(24f, 336f, 180f, 52f, 308f).forEachIndexed { index, offset ->
        accents.add(
            Color.hsv(
                (hue + offset) % 360f,
                (saturation * (1f - index * 0.05f)).coerceIn(0.34f, 1f),
                (value * (1.12f + index * 0.09f)).coerceIn(0.52f, 1f),
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
        val loaded = if (path == null) null else withContext(Dispatchers.IO) {
            val cacheKey = "$key|$path"
            cachedAmbient(cacheKey) { loadAmbient(path, cacheKey) }
        }
        ambient = loaded
        DiagnosticsLog.log(
            LogLevel.INFO,
            "Details/ambient",
            "bookmark=$key image=${path?.let { File(it).name } ?: "none"} loaded=${loaded != null}",
        )
    }
    return ambient
}

/**
 * Artwork-derived ambient presentation: base colour, genuinely blurred full-bleed artwork,
 * radial blooms from the extracted palette at clearly visible alpha, and readability scrims
 * limited to the top and bottom so the sharp artwork and the blooms stay prominent.
 */
@Composable
fun ArtworkAmbientBackground(ambient: ArtworkAmbient, modifier: Modifier = Modifier) {
    val palette = ambient.palette
    Box(modifier.fillMaxSize().background(palette.base)) {
        Image(
            bitmap = ambient.blurred,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(24.dp).alpha(0.88f),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().drawBehind {
                val w = size.width
                val h = size.height
                val radius = maxOf(w, h) * 0.9f
                palette.colors.forEachIndexed { index, source ->
                    val angle = index.toFloat() / palette.colors.size * 2f * Math.PI.toFloat()
                    val center = Offset(
                        w * (0.5f + 0.34f * cos(angle)),
                        h * (0.42f + 0.32f * sin(angle)),
                    )
                    val alpha = if (index < 2) BLOOM_ALPHA_PRIMARY else BLOOM_ALPHA_SECONDARY
                    val tint = source.copy(alpha = alpha)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(tint, tint.copy(alpha = alpha * 0.45f), tint.copy(alpha = 0f)),
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
                        0f to Color.Black.copy(alpha = 0.34f),
                        0.20f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.62f),
                    ),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().drawBehind {
                val random = java.util.Random(ambient.key.hashCode().toLong())
                repeat(360) {
                    val x = random.nextFloat() * size.width
                    val y = random.nextFloat() * size.height
                    drawCircle(
                        color = Color.White.copy(alpha = 0.02f + random.nextFloat() * 0.02f),
                        radius = 0.6f + random.nextFloat() * 0.9f,
                        center = Offset(x, y),
                    )
                }
            },
        )
    }
}
