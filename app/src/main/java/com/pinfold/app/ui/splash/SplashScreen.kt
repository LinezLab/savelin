package com.pinfold.app.ui.splash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinfold.app.R
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.ui.components.rememberPhotoPreview
import com.pinfold.app.ui.detail.AmbientBackdrop
import com.pinfold.app.ui.detail.rememberAmbience
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.SONIL_LOGO_TEXT
import kotlinx.coroutines.delay
import java.io.File

private const val SPLASH_REF_SP = 100f
private const val SPLASH_PREVIEW_ALPHA = 0.85f

@Composable
fun SplashScreen(
    item: BookmarkWithTags?,
    reduceMotion: Boolean,
    onDone: () -> Unit,
    keepOnScreen: Boolean = false,
) {
    val themeFloor = MaterialTheme.colorScheme.background
    val path = remember(item) {
        item?.bookmark?.imagePath
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it).takeIf { file -> file.isFile }?.absolutePath }
    }
    val ambience = rememberAmbience(item?.bookmark?.id ?: 0L, path)
    val preview = rememberPhotoPreview(path, item?.bookmark?.blurhash)

    var done by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onDone)
    fun complete() {
        if (!done) {
            done = true
            finish()
        }
    }

    LaunchedEffect(Unit) {
        if (keepOnScreen) return@LaunchedEffect
        delay(splashDurationMs(reduceMotion))
        complete()
    }
    LaunchedEffect(Unit) {
        if (keepOnScreen) return@LaunchedEffect
        delay(SPLASH_MAX_WAIT_MS)
        complete()
    }
    BackHandler(enabled = true) {}

    Box(Modifier.fillMaxSize().background(themeFloor)) {
        AmbientBackdrop(
            ambience = ambience,
            seamPx = 0f,
            reduceMotion = reduceMotion,
            floor = themeFloor,
            drawScrim = false,
        )
        BlurredHero(preview)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SplashSonilLogo()
        }
    }
}

@Composable
private fun BlurredHero(preview: ImageBitmap?) {
    if (preview == null || preview.width < 1 || preview.height < 1) return
    Canvas(Modifier.fillMaxSize()) {
        drawImage(
            image = preview,
            srcSize = IntSize(preview.width, preview.height),
            dstSize = IntSize(
                size.width.toInt().coerceAtLeast(1),
                size.height.toInt().coerceAtLeast(1),
            ),
            alpha = SPLASH_PREVIEW_ALPHA,
            filterQuality = FilterQuality.Low,
        )
    }
}

@Composable
private fun SplashSonilLogo() {
    val measurer = rememberTextMeasurer()
    val ink = clear.hero
    val refStyle = TextStyle(
        fontFamily = FontFamily(Font(R.font.nikea)),
        fontSize = SPLASH_REF_SP.sp,
        color = ink,
    )
    val refWidthPx = remember(measurer) {
        measurer.measure(SONIL_LOGO_TEXT, style = refStyle, maxLines = 1).size.width.toFloat()
    }
    val screenWidthPx = LocalWindowInfo.current.containerSize.width.toFloat()
    val fontSizeSp = splashLogoFontSizeSp(screenWidthPx, refWidthPx)
    val style = refStyle.copy(fontSize = fontSizeSp.sp)
    val layout = remember(measurer, fontSizeSp) {
        measurer.measure(SONIL_LOGO_TEXT, style = style, maxLines = 1)
    }
    val density = LocalDensity.current
    val width = with(density) { layout.size.width.toDp() }
    val height = with(density) { layout.size.height.toDp() }
    Canvas(
        Modifier.size(width, height)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                blendMode = BlendMode.Overlay
            }
            .semantics { contentDescription = "Sonil" },
    ) {
        drawText(layout, color = ink)
    }
}
