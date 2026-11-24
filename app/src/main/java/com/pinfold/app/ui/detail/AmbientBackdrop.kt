package com.pinfold.app.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

internal const val MESH_SEAM_ANIM_MS = 320

@Composable
internal fun AmbientBackdrop(
    ambience: ArtworkAmbience,
    seamPx: Float,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    var incoming by remember { mutableStateOf(ambience) }
    var outgoing by remember { mutableStateOf<ArtworkAmbience?>(null) }
    val fade = remember { Animatable(1f) }
    val seam = remember { Animatable(0f) }

    DisposableEffect(ambience.id) {
        logArtwork("Mesh backdrop enter source=${ambience.source.name}")
        onDispose { logArtwork("Mesh backdrop disposed source=${ambience.source.name}") }
    }

    LaunchedEffect(ambience, reduceMotion) {
        if (ambience === incoming) {
            if (reduceMotion && fade.value < 1f) fade.snapTo(1f)
            return@LaunchedEffect
        }
        outgoing = incoming
        incoming = ambience
        fade.snapTo(0f)
        if (reduceMotion) {
            fade.snapTo(1f)
        } else {
            fade.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = crossfadeDurationMs(reduceMotion),
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    LaunchedEffect(seamPx, reduceMotion) {
        if (seamPx <= 0f) {
            seam.snapTo(0f)
        } else if (seam.value <= 0f || reduceMotion) {
            seam.snapTo(seamPx)
        } else {
            seam.animateTo(seamPx, tween(MESH_SEAM_ANIM_MS, FastOutSlowInEasing))
        }
    }

    Canvas(modifier.fillMaxSize().background(MeshFloorColor)) {
        val split = seam.value.coerceIn(0f, size.height)
        fun drawMeshLayer(mesh: ImageBitmap?, alpha: Float) {
            if (mesh == null || alpha <= 0f) return
            if (split > 0.5f) {
                drawImage(
                    image = mesh,
                    srcSize = IntSize(MESH_TEX, 1),
                    dstSize = Size(size.width, split),
                    alpha = alpha,
                    filterQuality = FilterQuality.Low,
                )
            }
            val belowHeight = size.height - split
            if (belowHeight > 0.5f) {
                drawImage(
                    image = mesh,
                    srcSize = IntSize(MESH_TEX, MESH_TEX),
                    dstOffset = Offset(0f, split),
                    dstSize = Size(size.width, belowHeight),
                    alpha = alpha,
                    filterQuality = FilterQuality.Low,
                )
            }
        }
        val progress = fade.value.coerceIn(0f, 1f)
        drawMeshLayer(outgoing?.mesh, 1f - progress)
        drawMeshLayer(incoming.mesh, progress)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = 0.06f),
                    Color.Black.copy(alpha = 0.30f),
                ),
            ),
        )
    }
}
