package com.pinfold.app.ui.detail

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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

internal const val BOTTOM_DIFFUSION_SCALE = 1.55f
internal const val BOTTOM_DIFFUSION_HEIGHT = 0.54f
internal const val BOTTOM_FADE_DP = 340f

/** Layered artwork field used when no stored preview exists: derived colour, never a flat fill. */
internal fun DrawScope.drawToneField(tone: ArtworkTone, blooms: List<ToneBloom>, strength: Float = 1f) {
    drawRect(
        brush = Brush.linearGradient(
            listOf(
                lerp(tone.base, tone.dominant, 0.12f),
                lerp(tone.base, tone.dominant, 0.28f),
                tone.base,
            ),
        ),
    )
    val width = size.width
    val height = size.height
    blooms.forEach { bloom ->
        val center = Offset(width * bloom.x, height * bloom.y)
        val radius = maxOf(width, height) * bloom.radius
        val tint = bloom.color.copy(alpha = (bloom.alpha * strength).coerceIn(0f, 1f))
        drawCircle(
            brush = Brush.radialGradient(
                listOf(tint, tint.copy(alpha = tint.alpha * 0.4f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
            blendMode = BlendMode.Screen,
        )
    }
}

private fun DrawScope.drawBlooms(blooms: List<ToneBloom>) {
    val width = size.width
    val height = size.height
    blooms.forEach { bloom ->
        val center = Offset(width * bloom.x, height * bloom.y)
        val radius = maxOf(width, height) * bloom.radius
        val tint = bloom.color.copy(alpha = bloom.alpha)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(tint, tint.copy(alpha = bloom.alpha * 0.4f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
            blendMode = BlendMode.Screen,
        )
    }
}

/**
 * Apple Music style ambient: the same artwork enlarged and blurred behind the sharp hero,
 * palette-derived light blooms, and a heavy image-derived diffusion at the bottom. No bars,
 * panels, cards or app-default colours.
 */
@Composable
internal fun NowPlayingBackdrop(ambience: ArtworkAmbience, modifier: Modifier = Modifier) {
    val tone = ambience.tone
    val blooms = remember(tone) { toneBlooms(tone) }
    DisposableEffect(ambience.id, ambience.source) {
        logArtwork("Backdrop enter source=${ambience.source.name} buckets=${tone.buckets}")
        onDispose { logArtwork("Backdrop disposed source=${ambience.source.name}") }
    }

    Box(modifier.fillMaxSize().background(tone.base)) {
        if (ambience.image != null) {
            Image(
                bitmap = ambience.image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
                    .scale(AMBIENT_SCALE)
                    .blur(AMBIENT_BLUR_DP.dp)
                    .alpha(AMBIENT_IMAGE_ALPHA),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().drawBehind { drawToneField(tone, blooms) })
        }

        Box(Modifier.fillMaxSize().drawBehind { drawBlooms(blooms) })

        if (ambience.image != null) {
            Image(
                bitmap = ambience.image,
                contentDescription = null,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(BOTTOM_DIFFUSION_HEIGHT)
                    .scale(BOTTOM_DIFFUSION_SCALE)
                    .blur(BOTTOM_DIFFUSION_BLUR_DP.dp)
                    .alpha(BOTTOM_DIFFUSION_ALPHA),
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
                            tone.base.copy(alpha = BOTTOM_FADE_ALPHA * 0.55f),
                            lerp(tone.base, Color.Black, 0.45f).copy(alpha = BOTTOM_FADE_ALPHA),
                        ),
                    ),
                ),
        )

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to tone.base.copy(alpha = TOP_SCRIM_ALPHA),
                    0.28f to Color.Transparent,
                    0.64f to Color.Transparent,
                    1f to tone.base.copy(alpha = 0.28f),
                ),
            ),
        )
    }
}
