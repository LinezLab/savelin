package com.pinfold.app.ui.details

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.diagnostics.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

internal const val ARTWORK_LOG_TAG = "SavelinArtwork"

enum class DetailsBackgroundMode { ARTWORK, THEME, GENERATED }

enum class ArtworkSource { IMAGE, FALLBACK }

/** One entry point for both Logcat (tag SavelinArtwork) and the redacted in-app log. */
internal fun logArtwork(message: String, level: LogLevel = LogLevel.INFO) {
    runCatching {
        when (level) {
            LogLevel.INFO -> Log.d(ARTWORK_LOG_TAG, message)
            LogLevel.WARN -> Log.w(ARTWORK_LOG_TAG, message)
            LogLevel.ERROR -> Log.e(ARTWORK_LOG_TAG, message)
        }
    }
    DiagnosticsLog.log(level, ARTWORK_LOG_TAG, message)
}

/** Failure breadcrumb in the structured issues log as well as Logcat. */
internal fun artworkIssue(what: String, why: String) {
    DiagnosticsLog.issue("Artwork", what, why)
    logArtwork("Issue what=$what why=$why", LogLevel.ERROR)
}

/**
 * ARTWORK is the unambiguous default. A missing local image is no longer a reason to fall back to
 * the old THEME page: the caller derives a visibly different artwork-like palette instead. Legacy
 * PICTURE/SOLID values from older installs still map to ARTWORK/THEME.
 */
@Suppress("UNUSED_PARAMETER")
internal fun resolveDetailsBackgroundMode(setting: String?, hasImage: Boolean): DetailsBackgroundMode {
    val normalized = setting?.trim()?.uppercase(Locale.US).orEmpty()
    return when (normalized) {
        "GENERATED" -> DetailsBackgroundMode.GENERATED
        "THEME", "SOLID" -> DetailsBackgroundMode.THEME
        else -> DetailsBackgroundMode.ARTWORK
    }
}

internal const val BLOOM_ALPHA_PRIMARY = 0.72f
internal const val BLOOM_ALPHA_SECONDARY = 0.50f
internal const val BLOOM_ALPHA_TERTIARY = 0.30f
internal const val ARTWORK_IMAGE_ALPHA = 0.84f
internal const val ARTWORK_SCALE = 1.28f
internal const val SCRIM_TOP_ALPHA = 0.30f
internal const val SCRIM_BOTTOM_ALPHA = 0.54f

internal data class AmbientBloom(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float,
)

internal fun ambientBlooms(palette: ArtworkPalette): List<AmbientBloom> {
    val accents = palette.colors
    if (accents.isEmpty()) return emptyList()
    val placements = listOf(
        0.20f to 0.18f, 0.80f to 0.22f, 0.28f to 0.70f, 0.74f to 0.72f,
        0.50f to 0.44f, 0.08f to 0.52f, 0.92f to 0.56f, 0.50f to 0.06f,
    )
    return placements.mapIndexed { index, (x, y) ->
        val source = accents[index % accents.size]
        val hsv = rgbToHsv(
            (source.red * 255f).toInt().coerceIn(0, 255),
            (source.green * 255f).toInt().coerceIn(0, 255),
            (source.blue * 255f).toInt().coerceIn(0, 255),
        )
        val saturation = (hsv[1] * (1f - (index % 3) * 0.10f)).coerceIn(0.26f, 1f)
        val value = (hsv[2] * (1f + (index % 4) * 0.05f)).coerceIn(0.52f, 1f)
        val color = lerp(
            Color.hsv(hsv[0], saturation, value),
            palette.base,
            if (index % 3 == 2) 0.16f else 0f,
        )
        val alpha = when {
            index < 2 -> BLOOM_ALPHA_PRIMARY
            index < 5 -> BLOOM_ALPHA_SECONDARY
            else -> BLOOM_ALPHA_TERTIARY
        }
        AmbientBloom(x, y, 0.50f + (index % 3) * 0.16f, color, alpha)
    }
}

data class ArtworkPalette(
    val base: Color,
    val dominant: Color,
    val colors: List<Color>,
    val bucketCount: Int = 0,
)

data class ArtworkAmbient(
    val key: String,
    val blurred: ImageBitmap?,
    val palette: ArtworkPalette,
    val source: ArtworkSource = ArtworkSource.IMAGE,
)

internal fun Color.argbHex(): String = String.format(Locale.US, "#%08X", toArgb())

internal fun isEffectivelyBlackOrTransparent(color: Color): Boolean =
    color.alpha < 0.12f || maxOf(color.red, color.green, color.blue) < 0.14f

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

private fun liftAccent(color: Color, fallbackHue: Float): Color {
    if (color.alpha >= 0.95f && maxOf(color.red, color.green, color.blue) >= 0.30f) return color
    val hsv = rgbToHsv(
        (color.red * 255f).toInt().coerceIn(0, 255),
        (color.green * 255f).toInt().coerceIn(0, 255),
        (color.blue * 255f).toInt().coerceIn(0, 255),
    )
    val hue = if (hsv[1] < 0.12f) fallbackHue else hsv[0]
    return Color.hsv(hue, 0.55f, 0.66f)
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
    val bucketCount = ranked.size

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
    ranked.take(3).forEach { accents.add(liftAccent(it.second, hue)) }
    listOf(24f, 336f, 180f, 52f, 308f).forEachIndexed { index, offset ->
        accents.add(
            Color.hsv(
                (hue + offset) % 360f,
                (saturation * (1f - index * 0.05f)).coerceIn(0.34f, 1f),
                (value * (1.12f + index * 0.09f)).coerceIn(0.52f, 1f),
            ),
        )
    }
    return ArtworkPalette(base, dominant, accents.distinct().take(6), bucketCount)
}

/**
 * Deterministic, visibly non-flat palette for bookmarks with no local preview. Derived from
 * platform/site/seed (never from a title, caption, author or URL) so the details page is still an
 * artwork background rather than the old flat theme page.
 */
internal fun fallbackPalette(platform: String, site: String, seed: Long): ArtworkPalette {
    val basis = "$platform|$site|$seed"
    var hash = 7L
    for (ch in basis) hash = hash * 31L + ch.code.toLong()
    val hue = Math.floorMod(hash, 360L).toFloat()
    val accents = listOf(
        Color.hsv(hue, 0.74f, 0.94f),
        Color.hsv((hue + 148f) % 360f, 0.66f, 0.86f),
        Color.hsv((hue + 256f) % 360f, 0.58f, 0.80f),
        Color.hsv((hue + 52f) % 360f, 0.48f, 0.72f),
    )
    return ArtworkPalette(Color.hsv(hue, 0.46f, 0.22f), accents.first(), accents, bucketCount = 0)
}

internal fun fallbackAmbient(key: Any?, platform: String, site: String): ArtworkAmbient {
    val seed = (key?.hashCode() ?: 0).toLong()
    return ArtworkAmbient("$key|fallback", null, fallbackPalette(platform, site, seed), ArtworkSource.FALLBACK)
}

/** Last decision made for the details ambient background, safe to copy out of Settings. */
data class ArtworkDecision(
    val bookmarkId: Long,
    val setting: String,
    val mode: DetailsBackgroundMode,
    val imageAvailable: Boolean,
    val imageFile: String?,
    val source: ArtworkSource?,
    val bucketCount: Int,
    val accents: List<String>,
    val anyBlackOrTransparent: Boolean,
    val composed: Boolean,
    val crossfadeChanged: Boolean,
) {
    val summary: String
        get() = "mode=${mode.name} image=$imageAvailable buckets=$bucketCount accents=${accents.size}"
}

object ArtworkDiagnostics {
    @Volatile
    private var lastDecision: ArtworkDecision? = null

    @Synchronized
    fun record(decision: ArtworkDecision) {
        lastDecision = decision
    }

    @Synchronized
    fun last(): ArtworkDecision? = lastDecision

    @Synchronized
    fun update(transform: (ArtworkDecision) -> ArtworkDecision) {
        lastDecision?.let { lastDecision = transform(it) }
    }

    @Synchronized
    fun format(): String {
        val d = lastDecision ?: return "Artwork background diagnostics: no details screen visited yet."
        return buildString {
            appendLine("Artwork background diagnostics (redacted)")
            appendLine("bookmarkId=${d.bookmarkId}")
            appendLine("setting=${d.setting} resolvedMode=${d.mode.name}")
            appendLine("imageAvailable=${d.imageAvailable} file=${d.imageFile ?: "none"}")
            appendLine("source=${d.source?.name ?: "none"} paletteBuckets=${d.bucketCount}")
            appendLine("accents=${d.accents.joinToString(", ")}")
            appendLine("anyBlackOrTransparent=${d.anyBlackOrTransparent} composed=${d.composed} crossfadeChanged=${d.crossfadeChanged}")
            append("layers: blur=$ARTWORK_IMAGE_ALPHA scale=$ARTWORK_SCALE bloomPrimary=$BLOOM_ALPHA_PRIMARY ")
            append("bloomSecondary=$BLOOM_ALPHA_SECONDARY bloomTertiary=$BLOOM_ALPHA_TERTIARY ")
            append("scrimTop=$SCRIM_TOP_ALPHA scrimBottom=$SCRIM_BOTTOM_ALPHA")
        }
    }
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

private fun decodeScaled(path: String, maxDimension: Int, label: String): Bitmap? {
    val name = File(path).name
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        logArtwork(
            "Bitmap decode FAIL label=$label file=$name reason=noBounds out=${bounds.outWidth}x${bounds.outHeight}",
            LogLevel.WARN,
        )
        artworkIssue("decode", "$label reason=noBounds file=$name")
        return null
    }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension * 2) sample *= 2
    val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    })
    if (decoded == null) {
        logArtwork("Bitmap decode FAIL label=$label file=$name reason=fullDecodeNull sample=$sample", LogLevel.WARN)
        artworkIssue("decode", "$label reason=fullDecodeNull file=$name")
        return null
    }
    val scale = min(1f, maxDimension.toFloat() / maxOf(decoded.width, decoded.height))
    val result = if (scale >= 1f) {
        decoded
    } else {
        val width = max(1, (decoded.width * scale).toInt())
        val height = max(1, (decoded.height * scale).toInt())
        Bitmap.createScaledBitmap(decoded, width, height, true)
    }
    logArtwork(
        "Bitmap decode OK label=$label file=$name src=${decoded.width}x${decoded.height} sample=$sample out=${result.width}x${result.height}",
    )
    return result
}

private fun loadAmbient(path: String, key: String): ArtworkAmbient? {
    val small = decodeScaled(path, 64, "palette") ?: return null
    val pixels = IntArray(small.width * small.height)
    small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
    val palette = extractArtworkPalette(pixels)
    val soft = decodeScaled(path, 112, "bloom") ?: small
    val blackOrTransparent = palette.colors.any { isEffectivelyBlackOrTransparent(it) }
    logArtwork(
        "Palette source=IMAGE buckets=${palette.bucketCount} pixels=${pixels.size} " +
            "accents=${palette.colors.joinToString(",") { it.argbHex() }} blackOrTransparent=$blackOrTransparent",
    )
    if (palette.bucketCount == 0 || palette.colors.isEmpty() || blackOrTransparent) {
        artworkIssue("palette", "buckets=${palette.bucketCount} blackOrTransparent=$blackOrTransparent file=${File(path).name}")
    }
    return ArtworkAmbient(key, soft.asImageBitmap(), palette, ArtworkSource.IMAGE)
}

/** Downsamples once off the main thread and always yields a usable (image or fallback) ambient. */
@Composable
fun rememberArtworkAmbient(
    imagePath: String?,
    key: Any?,
    platform: String = "",
    site: String = "",
): ArtworkAmbient {
    var ambient by remember(key, imagePath, platform, site) {
        mutableStateOf<ArtworkAmbient?>(fallbackAmbient(key, platform, site))
    }
    LaunchedEffect(key, imagePath, platform, site) {
        val path = imagePath
        val fromImage = if (path != null) {
            withContext(Dispatchers.IO) {
                val cacheKey = "$key|$path"
                cachedAmbient(cacheKey) { loadAmbient(path, cacheKey) }
            }
        } else null
        val result = fromImage ?: fallbackAmbient(key, platform, site)
        ambient = result
        val name = path?.let { File(it).name } ?: "none"
        logArtwork(
            "Ambient resolved id=$key file=$name pathPresent=${path != null} " +
                "fileExists=${path?.let { File(it).isFile } ?: false} source=${result.source.name} " +
                "buckets=${result.palette.bucketCount} accents=${result.palette.colors.joinToString(",") { it.argbHex() }}",
        )
        DiagnosticsLog.log(
            LogLevel.INFO,
            "Details/ambient",
            "bookmark=$key image=$name source=${result.source.name} loaded=${fromImage != null}",
        )
    }
    return ambient ?: fallbackAmbient(key, platform, site)
}

data class AmbientSelection(val current: ArtworkAmbient, val settled: ArtworkAmbient?)

/**
 * Holds the previous composed ambient until the next bookmark's artwork has actually been sampled, so
 * a switch crossfades two real palettes instead of flashing a placeholder gradient.
 */
@Composable
fun rememberDetailsAmbient(
    imagePath: String?,
    key: Any?,
    platform: String = "",
    site: String = "",
): AmbientSelection {
    val current = rememberArtworkAmbient(imagePath, key, platform, site)
    var settled by remember { mutableStateOf<ArtworkAmbient?>(null) }
    LaunchedEffect(current.key, imagePath, platform, site) {
        if (imagePath == null || current.source == ArtworkSource.IMAGE) settled = current
    }
    return AmbientSelection(current, settled)
}

/**
 * Full-screen artwork environment: base, enlarged blurred artwork, palette-derived radial blooms,
 * a light readability gradient and a quiet grain layer. The sharp hero is drawn by the caller above
 * all of these layers.
 */
@Composable
fun ArtworkAmbientBackground(
    ambient: ArtworkAmbient,
    modifier: Modifier = Modifier,
    seed: Long = ambient.key.hashCode().toLong(),
) {
    val palette = ambient.palette
    val blooms = ambientBlooms(palette)
    val layers = "blur=$ARTWORK_IMAGE_ALPHA scale=$ARTWORK_SCALE " +
        "bloomPrimary=$BLOOM_ALPHA_PRIMARY bloomSecondary=$BLOOM_ALPHA_SECONDARY " +
        "bloomTertiary=$BLOOM_ALPHA_TERTIARY scrimTop=$SCRIM_TOP_ALPHA scrimBottom=$SCRIM_BOTTOM_ALPHA"
    DisposableEffect(ambient.key, ambient.source) {
        logArtwork("Compose ENTER key=${ambient.key} source=${ambient.source.name} $layers")
        ArtworkDiagnostics.update { it.copy(composed = true) }
        onDispose {
            logArtwork("Compose DISPOSE key=${ambient.key} source=${ambient.source.name}")
            ArtworkDiagnostics.update { it.copy(composed = false) }
        }
    }
    Box(modifier.fillMaxSize().background(palette.base)) {
        if (ambient.blurred != null) {
            Image(
                bitmap = ambient.blurred,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().scale(ARTWORK_SCALE).blur(36.dp).alpha(ARTWORK_IMAGE_ALPHA),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                Modifier.fillMaxSize().scale(ARTWORK_SCALE).blur(24.dp).background(
                    Brush.linearGradient(palette.colors.map { it.copy(alpha = ARTWORK_IMAGE_ALPHA) } + palette.base),
                ),
            )
        }
        Box(
            Modifier.fillMaxSize().drawBehind {
                val w = size.width
                val h = size.height
                blooms.forEach { bloom ->
                    val center = Offset(w * bloom.x, h * bloom.y)
                    val radius = maxOf(w, h) * bloom.radius
                    val tint = bloom.color.copy(alpha = bloom.alpha)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(tint, tint.copy(alpha = bloom.alpha * 0.45f), tint.copy(alpha = 0f)),
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
                    0f to palette.base.copy(alpha = SCRIM_TOP_ALPHA),
                    0.24f to Color.Transparent,
                    0.58f to Color.Transparent,
                    1f to palette.base.copy(alpha = SCRIM_BOTTOM_ALPHA),
                ),
            ),
        )
        Box(
            Modifier.fillMaxSize().drawBehind {
                val random = java.util.Random(seed)
                repeat(360) {
                    val x = random.nextFloat() * size.width
                    val y = random.nextFloat() * size.height
                    drawCircle(
                        color = Color.White.copy(alpha = 0.016f + random.nextFloat() * 0.018f),
                        radius = 0.6f + random.nextFloat() * 0.9f,
                        center = Offset(x, y),
                    )
                }
            },
        )
    }
}
