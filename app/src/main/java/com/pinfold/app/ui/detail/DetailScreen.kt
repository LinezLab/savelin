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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.diagnostics.DiagnosticsLog
import java.io.File

/** Fraction of the hero height over which the sharp artwork's alpha ramps from 1 to 0. */
internal const val HERO_EDGE_FEATHER_FRACTION = 0.34f

/** Oversized, image-derived blurred halo: begins behind the hero and continues past its edge. */
internal const val HERO_HALO_OVERSHOOT_DP = 220f
internal const val HERO_HALO_SCALE = 1.46f
internal const val HERO_HALO_BLUR_DP = 60f
internal const val HERO_HALO_ALPHA = 0.96f

/** The halo's own fade starts below the hero edge, so it is fully opaque at the seam. */
internal const val HERO_HALO_TAIL_FEATHER_DP = 120f

/** Soft colour wash sampled from the artwork; transparent at both ends of the feather band. */
internal const val HERO_EDGE_WASH_ALPHA = 0.26f

internal val heroEdgeFeatherStart: Float get() = 1f - HERO_EDGE_FEATHER_FRACTION

/** Alpha of the sharp hero at a normalised vertical position (0 at its top, 1 at its bottom). */
internal fun heroEdgeAlphaAt(progress: Float): Float = when {
    progress <= heroEdgeFeatherStart -> 1f
    progress >= 1f -> 0f
    else -> (1f - progress) / HERO_EDGE_FEATHER_FRACTION
}

/** Alpha of the halo at a normalised vertical position, with its tail starting at [tailStart]. */
internal fun heroHaloAlphaAt(tailStart: Float, progress: Float): Float = when {
    progress <= tailStart -> 1f
    progress >= 1f -> 0f
    else -> (1f - progress) / (1f - tailStart)
}

/** Dark wash colour sampled from the artwork base so the transition stays in the same palette. */
internal fun heroEdgeWash(base: Color): Color = lerp(base, Color.Black, 0.42f)

/**
 * Background-first detail screen. The sharp hero dissolves into an oversized, blurred copy of the
 * same artwork instead of ending at a hard edge: see docs/APPLE-MUSIC-ARTWORK-EDGE.md. The v1.7.8
 * ambient backdrop, crossfade, Back affordance and diagnostics remain untouched.
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

            HeroAmbientHalo(ambience.image, ambience.tone, blooms, heroHeight)
            SharpHero(path, ambience, blooms, heroHeight, context)
            HeroEdgeWash(ambience.tone.base, heroHeight)
        }

        Box(
            Modifier.align(Alignment.TopStart)
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
 * Oversized blurred copy of the same artwork. It sits behind the sharp hero and extends
 * [HERO_HALO_OVERSHOOT_DP] past its bottom edge, so the hero's feather always reveals matching
 * colour. Its own tail fades out well below the seam, leaving no second edge.
 */
@Composable
private fun BoxScope.HeroAmbientHalo(
    image: ImageBitmap?,
    tone: ArtworkTone,
    blooms: List<ToneBloom>,
    heroHeight: Dp,
) {
    val haloHeight = heroHeight + HERO_HALO_OVERSHOOT_DP.dp
    val tailStart = ((haloHeight - HERO_HALO_TAIL_FEATHER_DP.dp) / haloHeight).coerceIn(0f, 1f)
    Box(
        Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(haloHeight)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        tailStart to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
                    .scale(HERO_HALO_SCALE)
                    .blur(HERO_HALO_BLUR_DP.dp)
                    .alpha(HERO_HALO_ALPHA),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize().drawBehind { drawToneField(tone, blooms, 1.15f) })
        }
    }
}

/**
 * Sharp artwork with a long alpha ramp at its bottom. The mask is scoped to this box with an
 * offscreen layer, so only the hero is feathered; the ambient backdrop behind it is untouched.
 */
@Composable
private fun BoxScope.SharpHero(
    path: String?,
    ambience: ArtworkAmbience,
    blooms: List<ToneBloom>,
    heroHeight: Dp,
    context: android.content.Context,
) {
    Box(
        Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(heroHeight)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Black,
                        heroEdgeFeatherStart to Color.Black,
                        1f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        if (path != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(path)).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(Modifier.fillMaxSize().drawBehind { drawToneField(ambience.tone, blooms, 1.3f) })
        }
    }
}

/**
 * Image-derived dark wash over the feather band. Both ends are fully transparent, so it cannot
 * introduce a line where the band starts or where the hero ends.
 */
@Composable
private fun BoxScope.HeroEdgeWash(base: Color, heroHeight: Dp) {
    Box(
        Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(heroHeight)
            .drawBehind {
                val wash = heroEdgeWash(base).copy(alpha = HERO_EDGE_WASH_ALPHA)
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        heroEdgeFeatherStart to Color.Transparent,
                        (heroEdgeFeatherStart + 1f) / 2f to wash,
                        1f to Color.Transparent,
                    ),
                )
            },
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
