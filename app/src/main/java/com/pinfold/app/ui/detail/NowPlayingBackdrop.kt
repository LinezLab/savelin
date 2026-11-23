package com.pinfold.app.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

internal data class MeshDrawRegions(val heldHeight: Int, val meshTop: Int, val meshHeight: Int)

internal fun meshSeamY(seamPx: Float, surfaceHeight: Float): Float = seamPx.coerceIn(0f, surfaceHeight)

internal fun meshDrawRegions(width: Float, height: Float, seamY: Float): MeshDrawRegions {
    val seam = meshSeamY(seamY, height)
    return MeshDrawRegions(
        heldHeight = seam.toInt(),
        meshTop = seam.toInt(),
        meshHeight = (height - seam).toInt().coerceAtLeast(0),
    )
}

private fun DrawScope.drawMesh(mesh: ImageBitmap, seamY: Float) {
    val regions = meshDrawRegions(size.width, size.height, seamY)
    val width = size.width.toInt().coerceAtLeast(1)
    if (regions.heldHeight > 0) {
        drawImage(
            image = mesh,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(mesh.width, 1),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(width, regions.heldHeight),
            alpha = 1f,
            filterQuality = FilterQuality.Low,
        )
    }
    if (regions.meshHeight > 0) {
        drawImage(
            image = mesh,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(mesh.width, mesh.height),
            dstOffset = IntOffset(0, regions.meshTop),
            dstSize = IntSize(width, regions.meshHeight),
            alpha = 1f,
            filterQuality = FilterQuality.Low,
        )
    }
}

/**
 * Artwork-derived colour field behind the sharp hero: the sleeve's own bottom row is held above
 * the seam and the full mesh continues below it, then a light scrim keeps foreground legible.
 */
@Composable
internal fun NowPlayingBackdrop(
    ambience: ArtworkAmbience,
    seam: () -> Dp,
    modifier: Modifier = Modifier,
) {
    val mesh = remember(ambience.meshTexels) { meshBitmap(ambience.meshTexels) }
    val tone = ambience.tone
    DisposableEffect(ambience.id, ambience.source) {
        logArtwork("Backdrop enter source=${ambience.source.name} mesh=${MESH_TEX}px")
        onDispose { logArtwork("Backdrop disposed source=${ambience.source.name}") }
    }
    Canvas(modifier.fillMaxSize().background(tone.base)) {
        drawMesh(mesh, seam().toPx())
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = MESH_SCRIM_TOP_ALPHA),
                    Color.Black.copy(alpha = MESH_SCRIM_BOTTOM_ALPHA),
                ),
            ),
        )
    }
}
