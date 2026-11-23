package com.pinfold.app.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.diagnostics.DiagnosticsLog
import java.io.File

internal const val HERO_BOTTOM_FADE_FRACTION = 0.26f
internal const val HERO_EDGE_BLUR_DP = 44f
internal const val HERO_EDGE_FADE_ALPHA = 0.92f

/** Fully transparent at the band top, faded into the ambient tone at the hero's bottom edge. */
internal fun heroBottomFadeColors(base: Color): List<Color> = listOf(
    Color.Transparent,
    base.copy(alpha = HERO_EDGE_FADE_ALPHA * 0.55f),
    lerp(base, Color.Black, 0.35f).copy(alpha = HERO_EDGE_FADE_ALPHA),
)

/**
 * Background-first detail screen. Draw order is deliberate:
 * 1. [NowPlayingBackdrop] - the full-screen blurred, image-derived artwork field, behind everything.
 * 2. The sharp hero artwork, full width in the upper portion, uncovered in its upper/main area.
 * 3. [HeroBottomBlend] - an image-derived blur/fade that hides ONLY the hero's bottom edge.
 * 4. The minimal Back affordance. No music controls are present.
 */
@Composable
fun DetailScreen(
    item: BookmarkWithTags,
    reduceMotion: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bookmark = item.bookmark
    val path = remember(bookmark.id, bookmark.imagePath) {
        bookmark.imagePath?.takeIf { it.isNotBlank() }
            ?.let { File(it).takeIf { file -> file.isFile }?.absolutePath }
    }
    val selection = rememberAmbienceSelection(bookmark.id, path, bookmark.platform, bookmark.siteName)
    val ambience = selection.settled ?: selection.current
    val blooms = remember(ambience.tone) { toneBlooms(ambience.tone) }
    val aspect = remember(bookmark.imageWidth, bookmark.imageHeight) {
        if (bookmark.imageWidth > 0 && bookmark.imageHeight > 0) {
            (bookmark.imageWidth.toFloat() / bookmark.imageHeight.toFloat()).coerceIn(0.42f, 2.6f)
        } else 1f
    }
    val context = LocalContext.current

    LaunchedEffect(Unit) { DiagnosticsLog.uiAction("Detail", "Open") }

    Box(modifier.fillMaxSize().background(ambience.tone.base)) {
        Crossfade(
            targetState = ambience,
            animationSpec = if (reduceMotion) snap() else tween(crossfadeDurationMs(reduceMotion)),
            label = "detailAmbience",
        ) { target ->
            NowPlayingBackdrop(target)
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val heroHeight = minOf(maxHeight * 0.66f, maxWidth / aspect)
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(heroHeight),
            ) {
                if (path != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(File(path)).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBehind { drawToneField(ambience.tone, blooms, 1.3f) },
                    )
                }
                HeroBottomBlend(ambience)
            }
        }

        Box(
            Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
                .padding(16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.24f))
                .clickable(onClick = onBack, onClickLabel = "Back"),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(22.dp)) { drawBackChevron() }
        }
    }
}

/**
 * Softly hides only the hero's bottom edge. This is drawn AFTER the sharp hero, so the upper/main
 * artwork stays sharp and uncovered. Only the bottom [HERO_BOTTOM_FADE_FRACTION] of the hero gets a
 * blurred, image-derived copy of the artwork whose alpha is ramped in by a vertical gradient, plus a
 * matching tone fade that blends the edge into the ambient backdrop. The offscreen graphics layer
 * scopes the DstIn mask to this band so it cannot erase the hero above it or the backdrop behind it.
 */
@Composable
private fun BoxScope.HeroBottomBlend(ambience: ArtworkAmbience) {
    val blurred = ambience.image
    if (blurred != null) {
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(HERO_BOTTOM_FADE_FRACTION)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.45f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Black,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            Image(
                bitmap = blurred,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(BOTTOM_DIFFUSION_SCALE)
                    .blur(HERO_EDGE_BLUR_DP.dp),
                contentScale = ContentScale.Crop,
            )
        }
    }
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(HERO_BOTTOM_FADE_FRACTION)
            .drawBehind { drawRect(brush = Brush.verticalGradient(heroBottomFadeColors(ambience.tone.base))) },
    )
}

private fun DrawScope.drawBackChevron() {
    val chevron = Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.36f)
        lineTo(size.width * 0.5f, size.height * 0.66f)
        lineTo(size.width * 0.82f, size.height * 0.36f)
    }
    drawPath(
        path = chevron,
        color = Color.White,
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
