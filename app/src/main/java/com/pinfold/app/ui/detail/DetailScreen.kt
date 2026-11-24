package com.pinfold.app.ui.detail

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.diagnostics.DiagnosticsLog
import java.io.File

internal const val HERO_BOTTOM_DISSOLVE_FRACTION = 0.42f

internal fun smootherStep(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * x * (x * (x * 6f - 15f) + 10f)
}

@Composable
fun DetailScreen(
    item: BookmarkWithTags,
    reduceMotion: Boolean,
    onBack: () -> Unit,
    detailAmbientBackground: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val bookmark = item.bookmark
    val path = remember(bookmark.id, bookmark.imagePath) {
        bookmark.imagePath?.takeIf { it.isNotBlank() }
            ?.let { File(it).takeIf { file -> file.isFile }?.absolutePath }
    }
    val selection = rememberAmbienceSelection(bookmark.id, path)
    val ambience = selection.settled ?: selection.current
    val heroFallback = remember(bookmark.id) { fallbackHeroColor(bookmark.id) }
    val aspect = remember(bookmark.imageWidth, bookmark.imageHeight) {
        if (bookmark.imageWidth > 0 && bookmark.imageHeight > 0) {
            (bookmark.imageWidth.toFloat() / bookmark.imageHeight.toFloat()).coerceIn(0.42f, 2.6f)
        } else 1f
    }
    val context = LocalContext.current
    val plainBackground = MaterialTheme.colorScheme.background

    LaunchedEffect(Unit) { DiagnosticsLog.uiAction("Detail", "Open") }

    Box(
        modifier.fillMaxSize()
            .background(if (detailAmbientBackground) MeshFloorColor else plainBackground),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val naturalHeight = maxWidth / aspect
            val heroHeight = minOf(naturalHeight, maxHeight * 0.72f)
            val heroScale = ContentScale.FillWidth
            val seamPx = if (detailAmbientBackground && path != null) {
                with(LocalDensity.current) { heroHeight.toPx() }
            } else 0f
            if (detailAmbientBackground) {
                AmbientBackdrop(ambience, seamPx, reduceMotion)
                LaunchedEffect(ambience.source, seamPx) {
                    ArtworkDiagnostics.record(
                        ArtworkSnapshot(source = ambience.source, seamPx = seamPx.toInt()),
                    )
                }
            }
            SharpHero(path, heroFallback, heroHeight, heroScale, context)
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

@Composable
private fun BoxScope.SharpHero(
    path: String?,
    fallbackColor: Color,
    heroHeight: Dp,
    heroScale: ContentScale,
    context: android.content.Context,
) {
    Box(
        Modifier.align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        if (path != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(File(path)).build(),
                contentDescription = null,
                contentScale = heroScale,
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = Array(17) { i ->
                                    val t = i / 16f
                                    t to Color.Black.copy(alpha = 1f - smootherStep(t))
                                },
                                startY = size.height * (1f - HERO_BOTTOM_DISSOLVE_FRACTION),
                                endY = size.height,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    },
            )
        } else {
            Box(Modifier.fillMaxSize().background(fallbackColor))
        }
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
