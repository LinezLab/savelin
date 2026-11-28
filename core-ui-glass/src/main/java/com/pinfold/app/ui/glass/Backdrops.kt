package com.pinfold.app.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import com.pinfold.app.ui.glass.theme.clear
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * What a glass surface refracts: everything drawn beneath it. Provided by the layer below, never
 * passed by hand, so no surface can sample the wallpaper when something else is actually behind it.
 */
val LocalBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

/**
 * Draws [content] (recording it), then [above] over it. Glass in [above] refracts the content and
 * everything beneath this layer; glass inside [content] refracts only what is beneath the layer.
 *
 * Use it wherever glass floats over something: the app over its wallpaper, the dock over the
 * screens, a viewer's bars over its pages, a glass chip over a picture.
 */
@Composable
fun GlassLayer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
    above: @Composable BoxScope.() -> Unit,
) {
    val below = LocalBackdrop.current
    val layer = rememberLayerBackdrop()
    val combined = rememberCombinedBackdrop(below, layer)
    Box(modifier) {
        Box(Modifier.matchParentSize().layerBackdrop(layer), content = content)
        CompositionLocalProvider(LocalBackdrop provides combined) { Box(Modifier.matchParentSize(), content = above) }
    }
}

/**
 * A mood of the wallpaper: where its blooms of light sit, as fractions of the screen, and how big they
 * are. [painting]: in the light theme, the sky the design mock gives that screen instead (its gradient
 * sampled from the mock and baked into a small picture, drawn smooth and stretched to the screen).
 */
data class AuroraMood(val blobs: List<Offset>, val radius: Float = 0.62f, val painting: Int? = null)

object AuroraMoods {
    // the blooms, in ClearColors.blooms' order: aqua, lilac, pink, periwinkle, haze
    val Desk = AuroraMood(listOf(Offset(0.0f, 0.58f), Offset(0.98f, 0.44f), Offset(1.0f, 0.1f), Offset(0.5f, 1.08f), Offset(0.2f, -0.05f)), painting = com.pinfold.app.ui.glass.R.drawable.aurora)

    /** Thought of the day: blue at the ends, lilac and pink through the middle (the mock's). */
    val Thoughts = AuroraMood(listOf(Offset(0.05f, 0.85f), Offset(0.6f, 0.35f), Offset(0.95f, 0.2f), Offset(0.4f, 1.1f), Offset(0.1f, 0.0f)), 0.68f, com.pinfold.app.ui.glass.R.drawable.aurora)

    /** Today: periwinkle at the top, peach on the left and pink on the right, pale blue below (the mock's). */
    val Today = AuroraMood(listOf(Offset(0.0f, 0.35f), Offset(0.9f, 0.6f), Offset(0.95f, 0.15f), Offset(0.3f, 1.05f), Offset(0.5f, 0.0f)), 0.7f, com.pinfold.app.ui.glass.R.drawable.aurora)
    val Files = AuroraMood(listOf(Offset(0.1f, 0.3f), Offset(0.9f, 0.7f), Offset(0.85f, 0.0f), Offset(0.2f, 1.05f), Offset(0.6f, -0.1f)), 0.58f)
    val Tools = AuroraMood(listOf(Offset(0.9f, 0.25f), Offset(0.1f, 0.65f), Offset(0.0f, 0.05f), Offset(0.8f, 1.05f), Offset(0.45f, -0.08f)))
    val Settings = AuroraMood(listOf(Offset(0.15f, 0.15f), Offset(0.95f, 0.85f), Offset(1.0f, 0.3f), Offset(0.1f, 1.0f), Offset(0.6f, 0.0f)), 0.55f)
    val Note = AuroraMood(listOf(Offset(0.05f, 0.85f), Offset(0.6f, 0.35f), Offset(0.95f, 0.2f), Offset(0.4f, 1.1f), Offset(0.1f, 0.0f)), 0.68f)
    val Welcome = AuroraMood(listOf(Offset(0.0f, 0.35f), Offset(0.9f, 0.6f), Offset(0.95f, 0.15f), Offset(0.3f, 1.05f), Offset(0.5f, 0.0f)), 0.7f)
}

private val OffsetPairConverter = TwoWayConverter<Pair<Offset, Offset>, AnimationVector4D>(
    { (a, b) -> AnimationVector4D(a.x, a.y, b.x, b.y) },
    { v -> Offset(v.v1, v.v2) to Offset(v.v3, v.v4) },
)

/**
 * The wallpaper: the theme's sky (a soft vertical gradient) with blooms of light pooled on it, as
 * Apple's wallpapers do. It is painted into one small bitmap (an eighth of the screen each way:
 * soft light needs no more) that is drawn scaled up, so a frame costs one image instead of six
 * full-screen gradients. The bitmap is kept and repainted in place only while a new [mood] glides
 * the blooms to new places (no garbage per frame), and the glide is slow and soft, as a wallpaper's.
 */
@Composable
fun Aurora(mood: AuroraMood, modifier: Modifier = Modifier) {
    // the old ClearPDF's glossy blobs: their hard, shaded edges are what the glass's lens bends at
    // every rim, so the buttons over them read raised, as they did there
    val custom = GlassSettings.customWallpaper
    if (GlassSettings.wallpaper == Wallpaper.Liquid || (GlassSettings.wallpaper == Wallpaper.Custom && custom != null)) {
        val dark = clear.dark
        val image = if (GlassSettings.wallpaper == Wallpaper.Custom && custom != null) custom else ImageBitmap.imageResource(
            if (dark) com.pinfold.app.ui.glass.R.drawable.aurora else com.pinfold.app.ui.glass.R.drawable.aurora
        )
        androidx.compose.foundation.Image(
            image, null, modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            filterQuality = FilterQuality.Low,
        )
        return
    }
    val colors = clear
    val anims = remember { List(5) { i -> Animatable(mood.blobs[i % mood.blobs.size] to Offset(mood.radius, 0f), OffsetPairConverter) } }
    LaunchedEffect(mood) {
        mood.blobs.forEachIndexed { i, p ->
            val target = p to Offset(mood.radius, 0f)
            if (anims[i].targetValue != target) launch { anims[i].animateTo(target, IosMotion.spec(IosMotion.LayoutDamping, 900f)) }
        }
    }
    val bitmap = remember { arrayOfNulls<ImageBitmap>(1) }
    Box(
        modifier.fillMaxSize().drawWithCache {
            val w = (size.width / 8f).toInt().coerceAtLeast(1)
            val h = (size.height / 8f).toInt().coerceAtLeast(1)
            // the wall's gradient is opaque, so repainting over the last frame needs no clear
            val image = bitmap[0]?.takeIf { it.width == w && it.height == h } ?: ImageBitmap(w, h).also { bitmap[0] = it }
            val small = Size(w.toFloat(), h.toFloat())
            // reading the blooms here makes the cache follow them
            val blooms = anims.map { it.value }
            CanvasDrawScope().draw(this, layoutDirection, Canvas(image), small) {
                drawRect(Brush.verticalGradient(colors.wall))
                val r = small.maxDimension
                colors.blooms.forEachIndexed { i, (c, strength) ->
                    val (p, rad) = blooms.getOrNull(i) ?: return@forEachIndexed
                    if (!(r * rad.x > 1f)) return@forEachIndexed
                    val center = Offset(p.x * small.width, p.y * small.height)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(c.copy(alpha = strength), c.copy(alpha = strength * 0.55f), c.copy(alpha = strength * 0.18f), Color.Transparent),
                            center, r * rad.x,
                        ),
                        radius = r * rad.x, center = center,
                    )
                }
            }
            onDrawBehind { drawImage(image, dstSize = IntSize(size.width.toInt(), size.height.toInt()), filterQuality = FilterQuality.Low) }
        },
    )
    // the mock's own sky for this screen (light theme), crossfading as the screens change
    val painting = if (!colors.dark) mood.painting else null
    androidx.compose.animation.Crossfade(painting, modifier.fillMaxSize(), animationSpec = IosMotion.finiteAlpha(), label = "sky") { id ->
        if (id != null) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.graphics.painter.BitmapPainter(ImageBitmap.imageResource(id), filterQuality = FilterQuality.High),
                null, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
            )
        }
    }
}

/**
 * An in-window portal for floating glass (menus, sheets, dialogs, toasts). Glass can't refract the
 * layer it is drawn in, so anything that floats is composed here, above every screen and the dock,
 * where [LocalBackdrop] is the whole live screen.
 */
@Stable
class GlassOverlayHost {
    internal val entries = mutableStateListOf<OverlayEntry>()
    internal var originInWindow by mutableStateOf(Offset.Zero)
    var size by mutableStateOf(IntSize.Zero)
        internal set

    /**
     * Chrome that animates over a still desk (the + fan, the recorder) holds the screens still while
     * it is out: they are kept as one texture instead of being redrawn under it on every frame.
     */
    var stillHolds by mutableStateOf(0)

    /** True while a sheet, dialog, menu or toast is composed in this portal. */
    val hasOverlays: Boolean get() = entries.isNotEmpty()

    internal fun register(entry: OverlayEntry) {
        entries.removeAll { it.key === entry.key }
        entries.add(entry)
    }

    /** The caller is gone: an overlay that animates out stays (as it was) until it says it's done. */
    internal fun unregister(key: Any) {
        val e = entries.firstOrNull { it.key === key } ?: return
        if (e.animatesOut) e.leaving = true else entries.remove(e)
    }

    internal fun remove(key: Any) {
        entries.removeAll { it.key === key }
    }

    /** A window point in the overlay's space. Read at layout or draw time. */
    fun windowToLocal(point: Offset): Offset = point - originInWindow
}

internal class OverlayEntry(val key: Any, val animatesOut: Boolean, val content: @Composable BoxScope.() -> Unit) {
    var leaving by mutableStateOf(false)
}

/**
 * An overlay's place in the portal: [leaving] turns true when whoever showed it goes away; an
 * overlay that animates out plays its exit then calls [done].
 */
@Stable
class OverlayPresence internal constructor(private val host: GlassOverlayHost, private val entry: OverlayEntry) {
    val leaving: Boolean get() = entry.leaving

    fun done() = host.remove(entry.key)
}

/** The presence of the overlay being composed (null outside the portal). */
val LocalOverlayPresence = staticCompositionLocalOf<OverlayPresence?> { null }

val LocalGlassOverlayHost = staticCompositionLocalOf<GlassOverlayHost?> { null }

@Composable
fun GlassOverlayLayer(host: GlassOverlayHost, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize().onGloballyPositioned {
            host.originInWindow = it.positionInWindow()
            host.size = it.size
        },
    ) {
        host.entries.forEach { entry ->
            key(entry.key) {
                val presence = remember(entry) { OverlayPresence(host, entry) }
                CompositionLocalProvider(LocalOverlayPresence provides presence) { entry.content(this) }
                // on its way out an overlay takes no more touches (nothing is chosen twice)
                if (entry.leaving) Box(Modifier.matchParentSize().pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } })
            }
        }
    }
}

/**
 * Shows [content] in the overlay for as long as the caller is composed. With [animatesOut] it stays
 * a moment longer, until it has played its exit (see [rememberOverlayAppear]). Draws nothing while idle.
 */
@Composable
fun GlassOverlay(animatesOut: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val host = LocalGlassOverlayHost.current ?: return
    val latest by rememberUpdatedState(content)
    val key = remember { Any() }
    DisposableEffect(host, key) {
        host.register(OverlayEntry(key, animatesOut) { latest() })
        onDispose { host.unregister(key) }
    }
}

/**
 * An overlay's 0 → 1 entrance on [enter], and 1 → 0 on [exit] once its caller is gone, after which
 * it leaves the portal. Read the value in draw (graphicsLayer).
 */
@Composable
fun rememberOverlayAppear(
    enter: androidx.compose.animation.core.AnimationSpec<Float> = IosMotion.alpha(IosMotion.MenuStiffness),
    exit: androidx.compose.animation.core.AnimationSpec<Float> = IosMotion.alpha(IosMotion.PressStiffness),
): Animatable<Float, androidx.compose.animation.core.AnimationVector1D> {
    val presence = LocalOverlayPresence.current
    val a = remember { Animatable(0f) }
    val leaving = presence?.leaving == true
    LaunchedEffect(leaving) {
        if (!leaving) {
            a.animateTo(1f, enter)
        } else {
            a.animateTo(0f, exit)
            presence?.done()
        }
    }
    return a
}

/**
 * A pop-up's 0 → 1 growth out of its trigger and 1 → 0 fold back into it, on the active Motion style.
 * The same [Animatable] is kept across an interruption: a second open or dismiss retargets the spring
 * in flight instead of restarting it from rest. Under Reduce motion both directions are short fades.
 */
@Composable
fun rememberSpringAppear(
    enter: AnimationSpec<Float> = IosMotion.Menu,
    exit: AnimationSpec<Float> = IosMotion.alpha(IosMotion.MenuStiffness),
): Animatable<Float, AnimationVector1D> = rememberOverlayAppear(enter = enter, exit = exit)

/**
 * One row of a springing pop-up. Content waits until the pop-up is ~40 % open, then rows arrive 35 ms
 * apart; dismissing folds them away immediately. Reading the shared [appear] snapshot means a quick
 * tap-away retargets every row still on its way in.
 */
@Composable
fun rememberRowReveal(appear: Animatable<Float, AnimationVector1D>, index: Int): Animatable<Float, AnimationVector1D> {
    val row = remember { Animatable(0f) }
    val open by remember { derivedStateOf { appear.value >= 0.4f } }
    LaunchedEffect(open, index) {
        if (open) {
            if (!MotionSettings.reduceMotion && index > 0) delay(index * 35L)
            row.animateTo(1f, IosMotion.Menu)
        } else {
            row.animateTo(0f, IosMotion.alpha(IosMotion.MenuStiffness))
        }
    }
    return row
}

/**
 * A row of a portal menu that owns its reveal: it grows in a beat after the menu opens (35 ms per
 * row) and folds away when the portal says the menu is leaving. Retargets the same Animatable.
 */
@Composable
fun rememberMenuRow(index: Int): Animatable<Float, AnimationVector1D> {
    val row = remember { Animatable(0f) }
    val leaving = LocalOverlayPresence.current?.leaving == true
    LaunchedEffect(leaving, index) {
        if (!leaving) {
            if (!MotionSettings.reduceMotion && index > 0) delay(index * 35L)
            row.animateTo(1f, IosMotion.Menu)
        } else {
            row.animateTo(0f, IosMotion.alpha(IosMotion.MenuStiffness))
        }
    }
    return row
}

/** Draw-time fade/slide for a row revealed by [rememberRowReveal] — no layout, no recomposition. */
fun Modifier.revealRow(row: Animatable<Float, AnimationVector1D>): Modifier = graphicsLayer {
    val r = row.value.coerceIn(0f, 1f)
    alpha = r
    translationY = (1f - r) * 8f
}

/**
 * The app's glass stack, bottom to top: the [wallpaper]; the [screens]; the [chrome] that floats over
 * them (the dock); then the overlay portal over all three. Provide [LocalGlassOverlayHost] above it
 * so overlays composed outside the stage (app-level dialogs and sheets) land in the same portal.
 */
@Composable
fun GlassStage(
    wallpaper: @Composable BoxScope.() -> Unit,
    screens: @Composable BoxScope.() -> Unit,
    chrome: @Composable BoxScope.() -> Unit,
) {
    val provided = LocalGlassOverlayHost.current
    val host = provided ?: remember { GlassOverlayHost() }
    val wall = rememberLayerBackdrop()
    val screensLayer = rememberLayerBackdrop()
    val chromeLayer = rememberLayerBackdrop()
    val underChrome = rememberCombinedBackdrop(wall, screensLayer)
    val underOverlay = rememberCombinedBackdrop(underChrome, chromeLayer)
    CompositionLocalProvider(LocalGlassOverlayHost provides host) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().layerBackdrop(wall), content = wallpaper)
                CompositionLocalProvider(LocalBackdrop provides wall) {
                    Box(Modifier.fillMaxSize().layerBackdrop(screensLayer), content = screens)
                }
            }
            Box(Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalBackdrop provides underChrome) {
                    Box(Modifier.fillMaxSize().layerBackdrop(chromeLayer), content = chrome)
                }
            }
            CompositionLocalProvider(LocalBackdrop provides underOverlay) { GlassOverlayLayer(host) }
        }
    }
}
