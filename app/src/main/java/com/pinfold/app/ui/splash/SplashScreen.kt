package com.pinfold.app.ui.splash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.pinfold.app.R
import com.pinfold.app.data.model.BookmarkWithTags
import com.pinfold.app.ui.detail.rememberTextShimmerBrush
import com.pinfold.app.ui.glass.theme.clear
import com.pinfold.app.ui.home.SONIL_LOGO_TEXT
import kotlinx.coroutines.delay

private const val SPLASH_REF_SP = 100f

@Composable
fun SplashScreen(
    item: BookmarkWithTags?,
    reduceMotion: Boolean,
    onDone: () -> Unit,
    keepOnScreen: Boolean = false,
) {
    val bg = if (clear.dark) Color.Black else Color.White

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

    Box(Modifier.fillMaxSize().background(bg)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SplashSonilLogo(reduceMotion)
        }
    }
}

@Composable
private fun SplashSonilLogo(reduceMotion: Boolean) {
    val measurer = rememberTextMeasurer()
    val ink = if (clear.dark) Color.White else Color.Black
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
    val shimmerBrush = rememberTextShimmerBrush(ink, layout.size.width.toFloat(), reduceMotion)
    Canvas(
        Modifier.size(width, height)
            .semantics { contentDescription = "Sonil" },
    ) {
        drawText(layout, brush = shimmerBrush)
    }
}
