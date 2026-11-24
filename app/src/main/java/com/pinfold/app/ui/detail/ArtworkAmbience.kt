package com.pinfold.app.ui.detail

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.pinfold.app.diagnostics.DiagnosticsLog
import com.pinfold.app.diagnostics.LogLevel

internal const val ARTWORK_LOG_TAG = "SavelinArtwork"

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
    val mesh: ImageBitmap?,
    val source: ArtworkSource,
)

internal fun crossfadeChanged(previousId: Long?, currentId: Long): Boolean =
    previousId != null && previousId != currentId

internal fun crossfadeDurationMs(reduceMotion: Boolean): Int = if (reduceMotion) 0 else MESH_FADE_MS

data class ArtworkSnapshot(
    val source: ArtworkSource = ArtworkSource.FALLBACK,
    val seamPx: Int = 0,
    val fadeMs: Int = MESH_FADE_MS,
) {
    val summary: String
        get() = "source=${source.name} mesh=${meshDescriptor()} seam=${seamPx}px fade=${fadeMs}ms"
}

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
            appendLine("source=${snapshot.source.name} mesh=${meshDescriptor()} seam=${snapshot.seamPx}px")
            append("fade=${snapshot.fadeMs}ms")
        }
    }
}

internal fun fallbackAmbience(id: Long): ArtworkAmbience =
    ArtworkAmbience(id, null, ArtworkSource.FALLBACK)

@Composable
internal fun rememberAmbience(id: Long, path: String?): ArtworkAmbience {
    val context = LocalContext.current
    val fallback = remember(id) { fallbackAmbience(id) }
    var ambience by remember(id, path) {
        mutableStateOf(
            if (path != null) {
                cachedMesh(id)?.let { ArtworkAmbience(id, it, ArtworkSource.IMAGE) } ?: fallback
            } else fallback,
        )
    }
    LaunchedEffect(id, path) {
        val resolved = if (path != null) {
            val mesh = loadMesh(context, id, path)
            if (mesh != null) ArtworkAmbience(id, mesh, ArtworkSource.IMAGE) else fallbackAmbience(id)
        } else fallback
        ambience = resolved
        logArtwork("Ambient resolved source=${resolved.source.name} hasMesh=${resolved.mesh != null}")
        DiagnosticsLog.log(
            LogLevel.INFO,
            "UI/Detail",
            "ambient source=${resolved.source.name} hasMesh=${resolved.mesh != null}",
        )
    }
    return ambience
}

data class AmbienceSelection(val current: ArtworkAmbience, val settled: ArtworkAmbience?)

@Composable
internal fun rememberAmbienceSelection(id: Long, path: String?): AmbienceSelection {
    val current = rememberAmbience(id, path)
    var settled by remember { mutableStateOf<ArtworkAmbience?>(null) }
    var lastId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(id) {
        if (crossfadeChanged(lastId, id)) logArtwork("Ambient crossfade requested")
        lastId = id
    }
    LaunchedEffect(current.id, current.mesh, path) {
        if (current.id != id) return@LaunchedEffect
        if (path == null || current.mesh != null) settled = current
    }
    return AmbienceSelection(current, settled)
}
