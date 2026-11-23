package com.pinfold.app.ui.detail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.diagnostics.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

internal const val ARTWORK_LOG_TAG = "SavelinArtwork"

enum class DetailArtworkMode { ARTWORK }
enum class ArtworkSource { IMAGE, FALLBACK }

internal const val AMBIENT_IMAGE_ALPHA = 0.94f
internal const val AMBIENT_SCALE = 1.36f
internal const val AMBIENT_BLUR_DP = 52f
internal const val BLOOM_ALPHA = 0.42f
internal const val BOTTOM_DIFFUSION_ALPHA = 0.88f
internal const val BOTTOM_DIFFUSION_BLUR_DP = 72f
internal const val TOP_SCRIM_ALPHA = 0.34f
internal const val BOTTOM_FADE_ALPHA = 0.90f
internal const val CROSSFADE_MS = 380

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

internal fun artworkIssue(what: String, why: String) {
    DiagnosticsLog.issue("Artwork", what, why)
    logArtwork("Issue what=$what why=$why", LogLevel.ERROR)
}

data class ArtworkTone(
    val base: Color,
    val dominant: Color,
    val accents: List<Color>,
    val buckets: Int = 0,
)

data class ArtworkAmbience(
    val id: Long,
    val image: ImageBitmap?,
    val tone: ArtworkTone,
    val source: ArtworkSource,
)

data class ToneBloom(
    val x: Float,
    val y: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float,
)

internal fun Color.hex(): String = String.format(Locale.US, "#%08X", toArgb())

internal fun isInvisible(color: Color): Boolean =
    color.alpha < 0.12f || maxOf(color.red, color.green, color.blue) < 0.14f

/** Pure RGB to HSV so the palette maths stays testable without an Android runtime. */
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

private fun channels(color: Color): IntArray = intArrayOf(
    (color.red * 255f).toInt().coerceIn(0, 255),
    (color.green * 255f).toInt().coerceIn(0, 255),
    (color.blue * 255f).toInt().coerceIn(0, 255),
)

private fun liftAccent(color: Color, fallbackHue: Float): Color {
    if (color.alpha >= 0.95f && maxOf(color.red, color.green, color.blue) >= 0.30f) return color
    val rgb = channels(color)
    val hsv = rgbToHsv(rgb[0], rgb[1], rgb[2])
    return Color.hsv(if (hsv[1] < 0.12f) fallbackHue else hsv[0], 0.58f, 0.72f)
}

/**
 * Bucket the downsampled pixels by hue/saturation and average each bucket. Dark and neutral pixels
 * share a bucket so even a nearly black image still yields visible, image-derived accents.
 */
internal fun extractArtworkTone(pixels: IntArray): ArtworkTone {
    data class Bin(var count: Int = 0, var r: Int = 0, var g: Int = 0, var b: Int = 0)

    val bins = Array(12) { Array(2) { Bin() } }
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
        if (hsv[1] < 0.15f || hsv[2] < 0.15f || hsv[2] > 0.985f) {
            neutral.count++
            neutral.r += r
            neutral.g += g
            neutral.b += b
            continue
        }
        val bin = bins[min(11, (hsv[0] / 30f).toInt())][if (hsv[1] < 0.45f) 0 else 1]
        bin.count++
        bin.r += r
        bin.g += g
        bin.b += b
    }

    val ranked = mutableListOf<Pair<Float, Color>>()
    fun rank(bin: Bin, weight: Float) {
        if (bin.count == 0) return
        val color = Color(bin.r / bin.count, bin.g / bin.count, bin.b / bin.count)
        val hsv = rgbToHsv(bin.r / bin.count, bin.g / bin.count, bin.b / bin.count)
        ranked.add(bin.count.toFloat() * (weight + hsv[1]) to color)
    }
    bins.forEach { row -> row.forEach { rank(it, 0.40f) } }
    rank(neutral, 0.32f)
    ranked.sortByDescending { it.first }

    val dominant = ranked.firstOrNull()?.second ?: run {
        val count = max(1, opaque)
        Color(totalR / count, totalG / count, totalB / count)
    }
    val dominantRgb = channels(dominant)
    val hsv = rgbToHsv(dominantRgb[0], dominantRgb[1], dominantRgb[2])
    val hue = if (hsv[1] < 0.12f) 210f else hsv[0]
    val saturation = hsv[1]
    val value = hsv[2]
    val base = Color.hsv(
        hue,
        saturation.coerceIn(0.20f, 1f),
        (value * 0.24f).coerceIn(0.12f, 0.32f),
    )

    val accents = mutableListOf<Color>()
    ranked.take(3).forEach { accents.add(liftAccent(it.second, hue)) }
    listOf(32f, 208f, 96f, 292f).forEachIndexed { index, offset ->
        accents.add(
            Color.hsv(
                (hue + offset) % 360f,
                (saturation * (1f - index * 0.06f)).coerceIn(0.38f, 1f),
                (value * (1.10f + index * 0.07f)).coerceIn(0.55f, 1f),
            ),
        )
    }
    return ArtworkTone(base, dominant, accents.distinct().take(6), ranked.size)
}

/** Deterministic, visibly image-like palette for bookmarks with no stored preview. */
internal fun fallbackTone(platform: String, site: String, seed: Long): ArtworkTone {
    var hash = 17L
    for (ch in "$platform|$site|$seed") hash = hash * 31L + ch.code
    val hue = Math.floorMod(hash, 360L).toFloat()
    val accents = listOf(
        Color.hsv(hue, 0.76f, 0.96f),
        Color.hsv((hue + 142f) % 360f, 0.68f, 0.88f),
        Color.hsv((hue + 248f) % 360f, 0.60f, 0.82f),
        Color.hsv((hue + 54f) % 360f, 0.52f, 0.76f),
    )
    return ArtworkTone(Color.hsv(hue, 0.48f, 0.24f), accents.first(), accents, 0)
}

internal fun fallbackAmbience(id: Long, platform: String, site: String): ArtworkAmbience =
    ArtworkAmbience(id, null, fallbackTone(platform, site, id), ArtworkSource.FALLBACK)

internal fun toneAccentHexes(tone: ArtworkTone): List<String> = tone.accents.map { it.hex() }

internal fun toneBlooms(tone: ArtworkTone): List<ToneBloom> {
    val accents = tone.accents
    if (accents.isEmpty()) return emptyList()
    val spots = listOf(
        0.18f to 0.16f, 0.84f to 0.24f, 0.30f to 0.74f,
        0.78f to 0.80f, 0.52f to 0.42f,
    )
    return spots.mapIndexed { index, (x, y) ->
        ToneBloom(
            x = x,
            y = y,
            radius = 0.46f + (index % 3) * 0.12f,
            color = accents[index % accents.size],
            alpha = if (index < 2) BLOOM_ALPHA else BLOOM_ALPHA * 0.62f,
        )
    }
}

internal fun crossfadeChanged(previousId: Long?, currentId: Long): Boolean =
    previousId != null && previousId != currentId

internal fun crossfadeDurationMs(reduceMotion: Boolean): Int = if (reduceMotion) 0 else CROSSFADE_MS

data class ArtworkSnapshot(
    val mode: DetailArtworkMode = DetailArtworkMode.ARTWORK,
    val source: ArtworkSource,
    val paletteBuckets: Int,
    val accents: List<String>,
) {
    val summary: String
        get() = "mode=${mode.name} source=${source.name} palette=$paletteBuckets accents=${accents.size}"
}

/** Mode, source, palette and alphas only — never an id, path, title or URL. */
object ArtworkDiagnostics {
    @Volatile
    private var last: ArtworkSnapshot? = null

    @Synchronized
    fun record(snapshot: ArtworkSnapshot) {
        last = snapshot
    }

    @Synchronized
    fun last(): ArtworkSnapshot? = last

    @Synchronized
    fun format(): String {
        val d = last ?: return "Artwork background diagnostics: no detail screen visited yet."
        return buildString {
            appendLine("Artwork background diagnostics (redacted)")
            appendLine("mode=${d.mode.name} source=${d.source.name}")
            appendLine("paletteBuckets=${d.paletteBuckets} accents=${d.accents.joinToString(", ")}")
            append("alphas: image=$AMBIENT_IMAGE_ALPHA bloom=$BLOOM_ALPHA ")
            append("bottom=$BOTTOM_DIFFUSION_ALPHA top=$TOP_SCRIM_ALPHA fade=$BOTTOM_FADE_ALPHA")
        }
    }
}

private data class AmbienceKey(val id: Long, val path: String)

private val ambienceCache = LinkedHashMap<AmbienceKey, ArtworkAmbience>()

@Synchronized
private fun cachedAmbience(key: AmbienceKey): ArtworkAmbience? = ambienceCache[key]

@Synchronized
private fun cacheAmbience(key: AmbienceKey, value: ArtworkAmbience) {
    ambienceCache[key] = value
    while (ambienceCache.size > 16) ambienceCache.remove(ambienceCache.keys.first())
}

private fun decodeScaled(path: String, maxDimension: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        artworkIssue("decode", "reason=noBounds")
        return null
    }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension * 2) sample *= 2
    val decoded = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }) ?: run {
        artworkIssue("decode", "reason=fullDecodeNull")
        return null
    }
    val scale = min(1f, maxDimension.toFloat() / maxOf(decoded.width, decoded.height))
    val result = if (scale >= 1f) {
        decoded
    } else {
        Bitmap.createScaledBitmap(
            decoded,
            max(1, (decoded.width * scale).toInt()),
            max(1, (decoded.height * scale).toInt()),
            true,
        )
    }
    logArtwork("Decode ok sample=$sample out=${result.width}x${result.height}")
    return result
}

internal fun loadAmbience(id: Long, path: String, platform: String, site: String): ArtworkAmbience {
    val cacheKey = AmbienceKey(id, path)
    cachedAmbience(cacheKey)?.let { return it }
    val palettePixels = decodeScaled(path, 56) ?: return fallbackAmbience(id, platform, site)
    val soft = decodeScaled(path, 112) ?: return fallbackAmbience(id, platform, site)
    val pixels = IntArray(palettePixels.width * palettePixels.height)
    palettePixels.getPixels(pixels, 0, palettePixels.width, 0, 0, palettePixels.width, palettePixels.height)
    val tone = extractArtworkTone(pixels)
    if (tone.buckets == 0 || tone.accents.isEmpty() || tone.accents.any(::isInvisible)) {
        artworkIssue("palette", "reason=emptyOrInvisible buckets=${tone.buckets}")
        return fallbackAmbience(id, platform, site)
    }
    val result = ArtworkAmbience(id, soft.asImageBitmap(), tone, ArtworkSource.IMAGE)
    cacheAmbience(cacheKey, result)
    logArtwork("Ambient loaded source=IMAGE buckets=${tone.buckets} accents=${toneAccentHexes(tone).joinToString(",")}")
    return result
}

@Composable
internal fun rememberAmbience(
    id: Long,
    path: String?,
    platform: String,
    site: String,
): ArtworkAmbience {
    val fallback = remember(id, platform, site) { fallbackAmbience(id, platform, site) }
    var ambience by remember(id, path) {
        mutableStateOf<ArtworkAmbience>(
            if (path != null) cachedAmbience(AmbienceKey(id, path)) ?: fallback else fallback,
        )
    }
    LaunchedEffect(id, path, platform, site) {
        val resolved = if (path != null) {
            withContext(Dispatchers.IO) { loadAmbience(id, path, platform, site) }
        } else fallback
        ambience = resolved
        logArtwork("Ambient resolved source=${resolved.source.name} buckets=${resolved.tone.buckets}")
        ArtworkDiagnostics.record(
            ArtworkSnapshot(
                mode = DetailArtworkMode.ARTWORK,
                source = resolved.source,
                paletteBuckets = resolved.tone.buckets,
                accents = toneAccentHexes(resolved.tone),
            ),
        )
        DiagnosticsLog.log(
            LogLevel.INFO,
            "UI/Detail",
            "ambient source=${resolved.source.name} buckets=${resolved.tone.buckets}",
        )
    }
    return ambience
}

data class AmbienceSelection(val current: ArtworkAmbience, val settled: ArtworkAmbience?)

/** Keeps the previous ambience alive until the next bookmark's artwork has been sampled. */
@Composable
internal fun rememberAmbienceSelection(
    id: Long,
    path: String?,
    platform: String,
    site: String,
): AmbienceSelection {
    val current = rememberAmbience(id, path, platform, site)
    var settled by remember { mutableStateOf<ArtworkAmbience?>(null) }
    var lastId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(id) {
        if (crossfadeChanged(lastId, id)) logArtwork("Ambient crossfade requested")
        lastId = id
    }
    LaunchedEffect(current.id, current.source, path) {
        if (current.id == id) settled = current
    }
    return AmbienceSelection(current, settled)
}
