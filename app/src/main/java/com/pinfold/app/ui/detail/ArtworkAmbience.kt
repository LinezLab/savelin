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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.diagnostics.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

internal const val ARTWORK_LOG_TAG = "SavelinArtwork"
internal const val CROSSFADE_MS = 520

enum class DetailArtworkMode { ARTWORK }
enum class ArtworkSource { IMAGE, FALLBACK }

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

data class ArtworkAmbience(
    val id: Long,
    val image: ImageBitmap?,
    val palette: ArtworkPalette,
    val source: ArtworkSource,
)

internal fun paletteHexes(palette: ArtworkPalette): List<String> = palette.accents.map { it.hex() }

internal fun crossfadeChanged(previousId: Long?, currentId: Long): Boolean =
    previousId != null && previousId != currentId

internal fun crossfadeDurationMs(reduceMotion: Boolean): Int = if (reduceMotion) 0 else CROSSFADE_MS

data class ArtworkSnapshot(
    val source: ArtworkSource,
    val buckets: Int,
    val accents: List<String>,
    val blooms: Int = BLOOM_COUNT,
    val fadeMs: Int = CROSSFADE_MS,
) {
    val summary: String
        get() = "source=${source.name} palette=$buckets accents=${accents.size} " +
            "blooms=$blooms fade=${fadeMs}ms"
}

/** Redacted shape/amount information only — never an id, path, title or URL. */
object ArtworkDiagnostics {
    @Volatile
    private var lastSnapshot: ArtworkSnapshot? = null

    @Synchronized
    fun record(snapshot: ArtworkSnapshot) {
        lastSnapshot = snapshot
    }

    @Synchronized
    fun last(): ArtworkSnapshot? = lastSnapshot

    @Synchronized
    fun format(): String {
        val snapshot = lastSnapshot
            ?: return "Artwork background diagnostics: no detail screen visited yet."
        return buildString {
            appendLine("Artwork background diagnostics (redacted)")
            appendLine("source=${snapshot.source.name} paletteBuckets=${snapshot.buckets}")
            appendLine("accents=${snapshot.accents.joinToString(", ")}")
            appendLine("layers: image=$AMBIENT_ALPHA bloom=$BLOOM_ALPHA " +
                "diffusion=$DIFFUSION_ALPHA scrim=$SCRIM_TOP_ALPHA fade=$BOTTOM_FADE_ALPHA")
            appendLine("hero: feather=$HERO_EDGE_FEATHER_FRACTION halo=$HALO_SCALE " +
                "blur=${HALO_BLUR_DP}dp tail=${HALO_TAIL_DP}dp")
            append("crossfade=${snapshot.fadeMs}ms blooms=${snapshot.blooms}")
        }
    }
}

private val ambienceCache = LinkedHashMap<Long, ArtworkAmbience>()

@Synchronized
private fun cachedAmbience(id: Long): ArtworkAmbience? = ambienceCache[id]

@Synchronized
private fun cacheAmbience(id: Long, value: ArtworkAmbience) {
    ambienceCache[id] = value
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

internal fun fallbackAmbience(id: Long): ArtworkAmbience =
    ArtworkAmbience(id, null, fallbackPalette(id), ArtworkSource.FALLBACK)

internal fun loadAmbience(id: Long, path: String): ArtworkAmbience {
    cachedAmbience(id)?.let { return it }
    val small = decodeScaled(path, PALETTE_SAMPLE_LIMIT)
        ?: return fallbackAmbience(id)
    val soft = decodeScaled(path, 128)
        ?: return fallbackAmbience(id)
    val pixels = IntArray(small.width * small.height)
    small.getPixels(pixels, 0, small.width, 0, 0, small.width, small.height)
    val palette = extractPalette(pixels)
    if (palette.buckets == 0 || palette.accents.isEmpty() || palette.accents.any(::isDim)) {
        artworkIssue("palette", "reason=emptyOrDim buckets=${palette.buckets}")
        return fallbackAmbience(id)
    }
    val result = ArtworkAmbience(id, soft.asImageBitmap(), palette, ArtworkSource.IMAGE)
    cacheAmbience(id, result)
    logArtwork("Ambient loaded source=IMAGE buckets=${palette.buckets} accents=${paletteHexes(palette).size}")
    return result
}

@Composable
internal fun rememberAmbience(id: Long, path: String?): ArtworkAmbience {
    val fallback = remember(id) { fallbackAmbience(id) }
    var ambience by remember(id, path) {
        mutableStateOf(if (path != null) cachedAmbience(id) ?: fallback else fallback)
    }
    LaunchedEffect(id, path) {
        val resolved = if (path != null) {
            withContext(Dispatchers.IO) { loadAmbience(id, path) }
        } else fallback
        ambience = resolved
        logArtwork("Ambient resolved source=${resolved.source.name} buckets=${resolved.palette.buckets}")
        ArtworkDiagnostics.record(
            ArtworkSnapshot(
                source = resolved.source,
                buckets = resolved.palette.buckets,
                accents = paletteHexes(resolved.palette),
            ),
        )
        DiagnosticsLog.log(
            LogLevel.INFO,
            "UI/Detail",
            "ambient source=${resolved.source.name} buckets=${resolved.palette.buckets}",
        )
    }
    return ambience
}

data class AmbienceSelection(val current: ArtworkAmbience, val settled: ArtworkAmbience?)

/** Keeps the outgoing artwork alive until the next bookmark has been sampled. */
@Composable
internal fun rememberAmbienceSelection(id: Long, path: String?): AmbienceSelection {
    val current = rememberAmbience(id, path)
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
