package com.pinfold.app.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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

internal val heroEdgeFeatherStart: Float get() = 1f - HERO_EDGE_FEATHER_FRACTION

/** Alpha of the sharp hero at a normalised vertical position (0 at its top, 1 at its bottom). */
internal fun heroEdgeAlphaAt(progress: Float): Float = when {
    progress <= heroEdgeFeatherStart -> 1f
    progress >= 1f -> 0f
    else -> (1f - progress) / HERO_EDGE_FEATHER_FRACTION
}

/**
 * Background-first detail screen: a sharp hero whose bottom alpha feathers into the artwork-derived
 * mesh held at that seam, plus a Back affordance. There are no player controls here.
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
    val aspect = remember(bookmark.imageWidth, bookmark.imageHeight) {
        if (bookmark.imageWidth > 0 && bookmark.imageHeight > 0) {
            (bookmark.imageWidth.toFloat() / bookmark.imageHeight.toFloat()).coerceIn(0.42f, 2.6f)
        } else 1f
    }

    LaunchedEffect(Unit) { DiagnosticsLog.uiAction("Detail", "Open") }

    Box(modifier.fillMaxSize().background(ambience.tone.base)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val heroHeight = minOf(maxHeight * 0.66f, maxWidth / aspect)
            Crossfade(
                targetState = ambience,
                animationSpec = if (reduceMotion) snap() else tween(
                    durationMillis = crossfadeDurationMs(reduceMotion),
                    easing = FastOutSlowInEasing,
                ),
                label = "detailAmbience",
            ) { target ->
                NowPlayingBackdrop(target, seam = { heroHeight })
            }
            SharpHero(path, heroHeight)
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
 * The sharp artwork with a long alpha ramp at its bottom. The mask is scoped to this box with an
 * offscreen layer, so only the hero is feathered; the mesh behind it is untouched.
 */
@Composable
private fun BoxScope.SharpHero(path: String?, heroHeight: Dp) {
    if (path == null) return
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
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(File(path)).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
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
