package com.pinfold.app.ui.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.ClearType
import com.pinfold.app.ui.glass.theme.clear
import com.kyant.backdrop.floatingLayer
import kotlin.math.roundToInt
import kotlin.math.tanh
import kotlinx.coroutines.launch

/** The dim behind a dialog or sheet; a tap on it dismisses. */
@Composable
private fun BoxScope.Scrim(appear: Animatable<Float, AnimationVector1D>, onDismiss: () -> Unit) {
    val c = clear
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = if (c.dark) 0.42f else 0.22f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
    )
}

/**
 * A dialog: a frosted card that springs in from 90 % (a glass spring) and fades quickly away, an
 * optional candy [icon] blob, a big title, then [content] and the buttons. [destructive] paints the
 * confirm button tomato. [centered] sets it out as an alert: everything centred, the buttons side
 * by side across the card.
 */
@Composable
fun GlassDialog(
    onDismiss: () -> Unit,
    title: String?,
    confirm: String? = null,
    onConfirm: () -> Unit = {},
    dismiss: String? = null,
    onDismissButton: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    icon: ImageVector? = null,
    accent: Color = Candy.Sky,
    dismissible: Boolean = true,
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val c = clear
    val tint = if (destructive) Candy.Tomato else accent
    GlassOverlay(animatesOut = true) {
        val leaving = LocalOverlayPresence.current?.leaving == true
        BackHandler(enabled = dismissible && !leaving, onBack = onDismiss)
        // pops in on a glass spring (a little past full size, then settles); leaves quickly, no bounce
        val appear = rememberSpringAppear(enter = IosMotion.Menu, exit = IosMotion.alpha(IosMotion.PressStiffness))
        val contentReveal = rememberRowReveal(appear, 0)
        val dismissReveal = rememberRowReveal(appear, 1)
        val confirmReveal = rememberRowReveal(appear, 2)
        Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
            Scrim(appear) { if (dismissible) onDismiss() }
            GlassCard(
                Modifier
                    .padding(24.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .floatingLayer {
                        val s = lerp(0.86f, 1f, appear.value.coerceIn(0f, 1f))
                        scaleX = s
                        scaleY = s
                        alpha = appear.value.coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 0.86f)
                    }
                    .clickable(remember { MutableInteractionSource() }, null) {},
                shape = RoundedRectangle(34.dp),
                wash = c.frost,
                blur = 16.dp,
            ) {
                Column(
                    Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
                    horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    if (icon != null) {
                        Box(Modifier.size(46.dp).clip(CircleShape).then(SavelinGlass.modifier(CircleShape)), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = SavelinGlass.ink, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                    if (title != null) {
                        Text(
                            title, style = if (centered) ClearType.Title.copy(fontSize = 19.sp, lineHeight = 24.sp) else ClearType.Headline, color = c.ink,
                            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                            modifier = Modifier.revealRow(contentReveal),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Column(Modifier.revealRow(contentReveal).heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { content() }
                    if (confirm != null || dismiss != null) {
                        Spacer(Modifier.height(20.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, if (centered) Alignment.CenterHorizontally else Alignment.End),
                        ) {
                            val each = if (centered) Modifier.weight(1f) else Modifier
                            if (dismiss != null) {
                                GlassPill(onClick = { (onDismissButton ?: onDismiss)() }, height = 46.dp, modifier = each.revealRow(dismissReveal)) {
                                    Text(dismiss, style = ClearType.BodyStrong, color = c.ink, maxLines = 1)
                                }
                            }
                            if (confirm != null) {
                                GlassPill(
                                    onClick = { if (confirmEnabled) onConfirm() },
                                    height = 46.dp,
                                    modifier = each.revealRow(confirmReveal),
                                ) {
                                    Text(
                                        confirm,
                                        style = ClearType.BodyStrong,
                                        color = if (!confirmEnabled) c.onGlassSoft else if (destructive) Candy.Tomato else SavelinGlass.ink,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A sheet: a floating frosted card along the bottom, inset from the edges. It rises on the glass
 * spring (Settings ▸ Motion sets how far it swings) and the title and rows fade in a beat behind, then
 * it slides away on an accelerating curve. [title] in the headline face, an optional [subtitle], then
 * [content].
 *
 * Cost while it moves: the card slides as a finished texture ([floatingLayer]); nothing behind it is
 * re-sampled or re-blurred until it has settled.
 */
@Composable
fun GlassSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    maxHeightFraction: Float = 0.86f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = clear
    GlassOverlay(animatesOut = true) {
        val leaving = LocalOverlayPresence.current?.leaving == true
        BackHandler(enabled = !leaving, onBack = onDismiss)
        val appear = rememberOverlayAppear(enter = SheetEnter, exit = SheetExit)
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        // the finger's pull on the sheet, in px: down follows 1:1, up rubber-bands
        val drag = remember { Animatable(0f) }
        var height by remember { mutableStateOf(0f) }
        val overlayHeight = LocalGlassOverlayHost.current?.size?.height ?: 0
        Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.BottomCenter) {
            Scrim(appear, onDismiss)
            val maxH = with(density) { if (overlayHeight > 0) (overlayHeight * maxHeightFraction).toDp() else 640.dp }
            GlassCard(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxH)
                    .onSizeChanged { height = it.height.toFloat() }
                    .floatingLayer {
                        val travel = size.height + 48.dp.toPx()
                        translationY = (1f - appear.value) * travel + drag.value
                        val up = (-drag.value).coerceAtLeast(0f)
                        if (up > 0f && size.height > 0f) {
                            scaleY = 1f + up / size.height * 0.5f
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                    }
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .draggable(
                        rememberDraggableState { delta ->
                            val cur = drag.value
                            val next = if (cur + delta < 0f) cur + delta * 0.25f else cur + delta
                            scope.launch { drag.snapTo(next.coerceAtLeast(-with(density) { 40.dp.toPx() })) }
                        },
                        Orientation.Vertical,
                        enabled = !leaving,
                        onDragStopped = { velocity ->
                            if (drag.value > height * 0.3f || velocity > 1200f) {
                                // carry on from where the finger let go
                                val travel = height + with(density) { 48.dp.toPx() }
                                val from = 1f - (drag.value / travel).coerceIn(0f, 1f)
                                drag.snapTo(0f)
                                appear.snapTo(from)
                                onDismiss()
                            } else {
                                drag.animateTo(0f, IosMotion.spec(IosMotion.PressDamping, IosMotion.PressStiffness), if (MotionSettings.reduceMotion) 0f else velocity)
                            }
                        },
                    ),
                shape = RoundedRectangle(32.dp),
                wash = c.frost,
                blur = 16.dp,
            ) {
                Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(36.dp, 5.dp).clip(Capsule).background(c.inkFaint.copy(alpha = 0.35f)))
                }
                if (title != null) {
                    Text(title, style = ClearType.Headline, color = c.ink, modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 14.dp))
                    if (subtitle != null) Text(subtitle, style = ClearType.Label, color = c.inkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 4.dp))
                    Spacer(Modifier.height(10.dp))
                } else {
                    Spacer(Modifier.height(8.dp))
                }
                content()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// the old app's sheet, a shade bouncier: it rises ~3 % past rest and settles once, whatever the motion
// setting (scaled by Jelly it shivered)
private val SheetEnter: AnimationSpec<Float> get() = IosMotion.spec(IosMotion.MenuDamping, IosMotion.PressStiffness, 0.001f)
private val SheetExit: AnimationSpec<Float> get() = IosMotion.alpha(IosMotion.PressStiffness)

/** A row of a sheet or menu: a candy icon blob, a label, an optional value. */
@Composable
fun GlassRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    accent: Color = Candy.Sky,
    value: String? = null,
    destructive: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = clear
    Row(
        modifier.fillMaxWidth().pressable(onClick).padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            val ink = if (destructive) Candy.Tomato else SavelinGlass.ink
            Box(Modifier.size(34.dp).clip(RoundedRectangle(11.dp)).then(SavelinGlass.modifier(RoundedRectangle(11.dp))), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = ink, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(14.dp))
        }
        Text(label, style = ClearType.BodyStrong, color = if (destructive) Candy.Tomato else c.ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (value != null) Text(value, style = ClearType.Label, color = c.inkSoft, modifier = Modifier.padding(start = 8.dp))
        trailing?.invoke()
    }
}

/** An item of a [GlassMenu]. */
data class GlassMenuItem(val label: String, val icon: ImageVector? = null, val destructive: Boolean = false, val onClick: () -> Unit)

/**
 * A popover menu anchored to [anchor] (window bounds of its trigger): unfolds from the trigger's
 * corner, width first then height, and folds away on a tap outside or Back.
 */
/** How long a chosen menu item waits for the menu to fold away (its dismiss tween, about 260 ms). */
private const val MenuGoneMillis = 220L

/**
 * The card every menu and popover is made of: the app's own live glass, with a heavier neutral frost
 * so what is behind is softened and every row meets contrast. Blur, refraction, colour, rim and
 * saturation all come from the active preset and its sliders; nothing is frozen here. Adaptive
 * luminance still picks dark or light ink to suit the page underneath.
 */
@Composable
fun MenuCard(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedRectangle(24.dp), content: @Composable ColumnScope.() -> Unit) {
    val c = clear
    val luminance = rememberGlassLuminance()
    GlassCard(
        // unseen for the frame or two it takes to read what is behind it (see [GlassLuminance])
        modifier.graphicsLayer { alpha = if (luminance == null || luminance.measured) 1f else 0f },
        shape = shape,
        wash = c.frost,
        blur = 12.dp + 4.dp * GlassSettings.style.blur,
        luminance = luminance,
    ) {
        val ink = adaptiveInk(luminance, c.ink)
        CompositionLocalProvider(LocalGlassInk provides ink, LocalMenuInk provides ink) { content() }
    }
}

@Composable
fun GlassMenu(anchor: Rect, onDismiss: () -> Unit, items: List<GlassMenuItem>) {
    // opened from a round glass button: that button's glass becomes the menu (MorphButtonSurface)
    if (isMorphAnchor(anchor, LocalDensity.current)) {
        MorphButtonSurface(anchor, open = true, onDismiss = onDismiss, onGone = {}, items = items)
        return
    }
    var closing by remember { mutableStateOf(false) }
    GlassMenuLayout(anchor, onDismiss) {
        Spacer(Modifier.height(6.dp))
        items.forEachIndexed { i, item ->
            val row = rememberMenuRow(i)
            Row(
                Modifier.fillMaxWidth().revealRow(row).pressable({
                    if (!closing) {
                        closing = true
                        onDismiss()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ item.onClick() }, MenuGoneMillis)
                    }
                }).padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item.label, style = ClearType.BodyStrong, color = if (item.destructive) Candy.Tomato else LocalGlassInk.current, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.icon != null) Icon(item.icon, null, tint = if (item.destructive) Candy.Tomato else LocalGlassInk.current.copy(alpha = 0.65f), modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

/** Same anchored frost and rounded shape as [GlassMenu], with room for a preserved custom layout. */
@Composable
fun GlassMenuLayout(
    anchor: Rect,
    onDismiss: () -> Unit,
    width: Dp = 236.dp,
    maxHeight: Dp = 480.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassMenuLayoutContent(anchor, onDismiss, width, maxHeight) { content() }
}

@Composable
private fun GlassMenuLayoutContent(
    anchor: Rect,
    onDismiss: () -> Unit,
    width: Dp,
    maxHeight: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = clear
    GlassOverlay(animatesOut = true) {
        val leaving = LocalOverlayPresence.current?.leaving == true
        BackHandler(enabled = !leaving, onBack = onDismiss)
        val host = LocalGlassOverlayHost.current ?: return@GlassOverlay
        val bars = WindowInsets.systemBars
        val view = LocalView.current
        // unfolds out of its button like a drop of gel: width first, the height a beat behind, each on
        // a glass spring (Settings ▸ Motion) that swells a little past rest and settles;
        // folding back into the button is quick and never bounces
        // (a spring both ways: closed mid-swing it carries its speed on instead of starting a new
        // curve from a standstill, and the screens' curve from Settings no longer reaches the glass)
        val appear = rememberOverlayAppear(enter = IosMotion.Menu, exit = IosMotion.alpha(IosMotion.PressStiffness))
        val lag = remember { Animatable(0f) }
        LaunchedEffect(leaving) {
            if (!leaving) lag.animateTo(1f, IosMotion.spec(IosMotion.LayoutDamping, IosMotion.MenuStiffness)) else lag.animateTo(0f, IosMotion.alpha(IosMotion.PressStiffness))
        }
        Box(Modifier.fillMaxSize().clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss))
        val local = Rect(host.windowToLocal(anchor.topLeft), host.windowToLocal(anchor.bottomRight))
        val margin = with(LocalDensity.current) { 12.dp.toPx() }
        var fromRight by remember { mutableStateOf(true) }
        var fromBelow by remember { mutableStateOf(true) }
        Layout(
            content = {
                MenuCard(
                    Modifier
                        .width(width)
                        .floatingLayer {
                            val p = appear.value
                            scaleX = lerp(0.5f, 1f, p).coerceAtLeast(0.2f)
                            scaleY = lerp(0.35f, 1f, lag.value).coerceAtLeast(0.2f)
                            transformOrigin = TransformOrigin(if (fromRight) 1f else 0f, if (fromBelow) 0f else 1f)
                            alpha = (p * 1.6f).coerceIn(0f, 1f)
                        },
                ) {
                    Column(Modifier.revealRow(rememberMenuRow(0)).heightIn(max = maxHeight).verticalScroll(rememberScrollState()), content = content)
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val p = measurables.first().measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
            // the system bars are in the way only where this portal reaches under them: the app's does,
            // a card's (Quick Preview) doesn't. Positions stay in the portal's own space, unshifted.
            val origin = host.originInWindow
            val root = view.rootView
            val barTop = (bars.getTop(this) - origin.y).coerceAtLeast(0f)
            val barBottom = (bars.getBottom(this) - (root.height - origin.y - constraints.maxHeight)).coerceAtLeast(0f)
            val barLeft = (bars.getLeft(this, layoutDirection) - origin.x).coerceAtLeast(0f)
            val barRight = (bars.getRight(this, layoutDirection) - (root.width - origin.x - constraints.maxWidth)).coerceAtLeast(0f)
            layout(constraints.maxWidth, constraints.maxHeight) {
                val right = local.center.x > constraints.maxWidth / 2f
                fromRight = right
                val x = if (right) (local.right - p.width) else local.left
                val below = local.bottom + 6f + p.height < constraints.maxHeight - barBottom - margin
                fromBelow = below
                val y = if (below) local.bottom + 6f else local.top - 6f - p.height
                val minX = barLeft + margin
                val maxX = (constraints.maxWidth - barRight - p.width - margin).coerceAtLeast(minX)
                val minY = barTop + margin
                val maxY = (constraints.maxHeight - barBottom - p.height - margin).coerceAtLeast(minY)
                p.place(IntOffset(x.coerceIn(minX, maxX).roundToInt(), y.coerceIn(minY, maxY).roundToInt()))
            }
        }
    }
}

/** A text field on glass: a rounded frosted well, a quiet [placeholder], the theme's ink. */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    error: Boolean = false,
    focus: FocusRequester? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    ink: Color? = null,
) {
    val c = clear
    val resolvedInk = ink ?: c.ink
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = ClearType.Body.copy(color = resolvedInk),
        cursorBrush = SolidColor(resolvedInk),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth().then(if (focus != null) Modifier.focusRequester(focus) else Modifier),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(RoundedRectangle(16.dp))
                    .then(SavelinGlass.modifier(RoundedRectangle(16.dp)))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, style = ClearType.Body, color = if (error) Candy.Tomato else resolvedInk.copy(alpha = 0.72f))
                inner()
            }
        },
    )
}

/**
 * A toast: a glass capsule that rises in above the dock, with an optional candy action.
 *
 * A toast lands on anything (the wallpaper, a white page, a black slide), so it always reads what is
 * behind it and takes light glass with dark ink or dark glass with white ink, whatever the theme and
 * the Adaptive luminance setting: the theme's frost over a black page came out a grey slab. Its lens
 * is the buttons' own (Back, More), so it bends its edges as they do.
 */
@Composable
fun GlassToast(
    text: String,
    action: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: (@Composable () -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
) {
    val c = clear
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, IosMotion.Menu) }
    val luminance = rememberGlassLuminance(enabled = true)
    val ink = adaptiveInk(luminance, c.ink)
    var dragY by remember { mutableStateOf(0f) }
    Row(
        modifier
            .then(
                if (onDismiss == null) Modifier else Modifier.draggable(
                    rememberDraggableState { delta -> dragY = (dragY + delta).coerceAtLeast(0f) },
                    Orientation.Vertical,
                    onDragStopped = {
                        if (dragY > 80f) onDismiss()
                        dragY = 0f
                    },
                ),
            )
            .floatingLayer {
                val p = appear.value
                // The drag offset lives in the same layer as the appear animation; writing
                // translationY twice would drop the drag, so combine them here.
                translationY = (1f - p) * 24.dp.toPx() + dragY
                alpha = p.coerceIn(0f, 1f)
            }
            .widthIn(max = 520.dp)
            .homeToastGlass(luminance = luminance)
            .padding(start = 20.dp, end = if (action != null) 6.dp else 20.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = ClearType.Label, color = ink, modifier = Modifier.weight(1f, fill = false).padding(vertical = 8.dp), maxLines = 3)
        if (action != null) {
            Spacer(Modifier.width(10.dp))
            GlassPill(onClick = onAction, height = 38.dp, horizontalPadding = 14.dp) {
                actionIcon?.invoke() ?: Text(action, style = ClearType.Label, color = SavelinGlass.ink)
            }
        }
    }
}
