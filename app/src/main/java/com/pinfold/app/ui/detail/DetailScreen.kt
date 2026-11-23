package com.pinfold.app.ui.detail

import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.diagnostics.DiagnosticsLog
import java.io.File


/**
 * A background-first detail screen: full-bleed artwork, the same artwork blurred behind it, and
 * nothing else yet. All previous detail chrome was removed; components will be layered on later.
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
            if (path != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(File(path)).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(heroHeight),
                )
            } else {
                Box(
                    Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(heroHeight)
                        .drawBehind { drawToneField(ambience.tone, blooms, 1.3f) },
                )
            }
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
