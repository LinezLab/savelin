package com.pinfold.app.ui.detail

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawPaletteField(
    palette: ArtworkPalette,
    blooms: List<ToneBloom>,
    strength: Float = 1f,
) {
    drawRect(
        Brush.verticalGradient(
            listOf(
                blendColors(palette.base, palette.dominant, 0.18f),
                palette.base,
                blendColors(palette.base, palette.dominant, 0.10f),
            ),
        ),
    )
    drawLightPools(blooms, strength)
}

internal fun DrawScope.drawLightPools(blooms: List<ToneBloom>, strength: Float = 1f) {
    val span = maxOf(size.width, size.height)
    blooms.forEach { bloom ->
        val center = Offset(size.width * bloom.x, size.height * bloom.y)
        val radius = span * bloom.radius
        val tint = bloom.color.copy(alpha = (bloom.alpha * strength).coerceIn(0f, 1f))
        drawCircle(
            brush = Brush.radialGradient(
                listOf(tint, tint.copy(alpha = tint.alpha * 0.45f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            center = center,
            radius = radius,
            blendMode = BlendMode.Screen,
        )
    }
}

private fun grainTexture(): ImageBitmap {
    val side = 96
    val pixels = IntArray(side * side) { index ->
        val value = ((index * 1103515245 + 12345) ushr 8) and 0xFF
        (value shl 24) or 0x00FFFFFF
    }
    return Bitmap.createBitmap(pixels, side, side, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Layered artwork ambient: blurred image, soft palette blooms, bottom diffusion, scrim, grain. */
@Composable
internal fun AmbientBackdrop(ambience: ArtworkAmbience, modifier: Modifier = Modifier) {
    val palette = ambience.palette
    val blooms = remember(palette) { paletteBlooms(palette) }
    val grain = remember { grainTexture() }
    DisposableEffect(ambience.id, ambience.source) {
        logArtwork("Ambient enter source=${ambience.source.name} buckets=${palette.buckets}")
        onDispose { logArtwork("Ambient disposed source=${ambience.source.name}") }
    }

    Box(modifier.fillMaxSize().background(palette.base)) {
        if (ambience.image != null) {
            Image(
                bitmap = ambience.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
                    .scale(AMBIENT_SCALE)
                    .blur(AMBIENT_BLUR_DP.dp)
                    .alpha(AMBIENT_ALPHA),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().drawBehind { drawPaletteField(palette, blooms) })
        }

        Box(Modifier.fillMaxSize().drawBehind { drawLightPools(blooms) })

        if (ambience.image != null) {
            Image(
                bitmap = ambience.image,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(DIFFUSION_HEIGHT)
                    .scale(DIFFUSION_SCALE)
                    .blur(DIFFUSION_BLUR_DP.dp)
                    .alpha(DIFFUSION_ALPHA),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BOTTOM_FADE_DP.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            palette.base.copy(alpha = BOTTOM_FADE_ALPHA * 0.55f),
                            blendColors(palette.base, Color.Black, 0.45f)
                                .copy(alpha = BOTTOM_FADE_ALPHA),
                        ),
                    ),
                ),
        )

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to palette.base.copy(alpha = SCRIM_TOP_ALPHA),
                    0.28f to Color.Transparent,
                    0.66f to Color.Transparent,
                    1f to palette.base.copy(alpha = SCRIM_BOTTOM_ALPHA),
                ),
            ),
        )

        if (GRAIN_ALPHA > 0f) {
            Box(Modifier.fillMaxSize().drawBehind {
                drawRect(
                    brush = ImageShader(grain, TileMode.Repeated, TileMode.Repeated),
                    alpha = GRAIN_ALPHA,
                )
            })
        }
    }
}
