package com.pinfold.app.ui.glass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.floatingLayer
import com.pinfold.app.ui.glass.theme.ClearType
import com.pinfold.app.ui.glass.theme.clear

/**
 * The one glass vocabulary of the app. Every chip, button, floating pill, pop-up, menu, dialog and
 * container reads its surface, wash, rim and ink from here, so they all wear exactly the same glass
 * as the home navigation bar and all answer a Settings change in the same frame.
 *
 * Nothing here owns a look of its own: [modifier] is the nav pill's effect stack (the shared
 * [glass], [glassEffects] and [glassHighlight] with the neutral [clear.glass] wash), the pressable
 * parts are [GlassPill]/[GlassCircle]/[glassControl] (the shared liquid press), and every value
 * comes from [GlassSettings]/[MotionSettings] and [clear].
 */
object SavelinGlass {
    /** The neutral tint of every surface: the same glass the nav pill wears, never an accent fill. */
    val wash: Color @Composable get() = clear.glass

    /** Content colour of every label and glyph on glass. */
    val ink: Color @Composable get() = clear.ink

    /** The specular edge, at the user's rim-light strength (drawn by [glassHighlight]). */
    val rim: Color @Composable get() = clear.rim

    /** The lens/blur of the nav pill, tied to the live glass preset and sliders. */
    @Composable
    fun metrics(): GlassMetrics {
        val s = GlassSettings.style
        return GlassMetrics(
            blur = s.blur.dp.coerceAtMost(8.dp),
            lensHeight = 24.dp.coerceAtMost(s.refractionHeight.dp * 2f),
            lensAmount = 40.dp.coerceAtMost(s.refractionAmount.dp * 2f),
        )
    }

    /** The canonical still-glass modifier: blur + lens/refraction + rim + neutral wash. */
    @Composable
    fun modifier(
        shape: Shape = Capsule,
        wash: Color = SavelinGlass.wash,
    ): Modifier {
        val m = metrics()
        return Modifier.glass(
            shape = shape,
            wash = wash,
            blur = m.blur,
            lensHeight = m.lensHeight,
            lensAmount = m.lensAmount,
        )
    }
}

/** The nav pill's glass geometry, in dp, read from the active preset. */
@Immutable
data class GlassMetrics(val blur: Dp, val lensHeight: Dp, val lensAmount: Dp)

/** A still glass surface in the global system (chrome, cards, panels). */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = Capsule,
    wash: Color = SavelinGlass.wash,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.then(SavelinGlass.modifier(shape, wash))) { content() }
}

/** A light-glass label (a card's platform/important/reminder chip): neutral tint and rim only. */
@Composable
fun LightGlassChip(
    modifier: Modifier = Modifier,
    shape: Shape = Capsule,
    label: String? = null,
    contentDescription: String? = null,
    icon: (@Composable () -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val desc = contentDescription
    Row(
        modifier
            .then(SavelinGlass.modifier(shape))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .then(if (desc != null) Modifier.semantics { this.contentDescription = desc } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon?.invoke()
        if (label != null) {
            Text(label, color = SavelinGlass.ink, style = ClearType.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        content?.invoke(this)
    }
}

/** A neutral glass pill button: no candy fill, the theme's ink, the shared liquid press and haptics. */
@Composable
fun GlassPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    label: String? = null,
    onLongClick: (() -> Unit)? = null,
    tap: Feel? = Feel.Tap,
    content: @Composable RowScope.() -> Unit,
) {
    GlassPill(onClick = onClick, modifier = modifier, height = height, label = label, onLongClick = onLongClick, tap = tap, content = content)
}

/** A neutral glass circle button: no candy fill, the theme's ink, the shared liquid press and haptics. */
@Composable
fun GlassCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = HeaderButtonSize,
    label: String? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    GlassCircle(onClick = onClick, modifier = modifier, size = size, label = label, onLongClick = onLongClick, ink = SavelinGlass.ink, content = content)
}

/** A floating glass popover container (menus, dropdowns) in the global system. */
@Composable
fun GlassPopover(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedRectangle(24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.then(SavelinGlass.modifier(shape)), content = content)
}

/**
 * The app's dialog: a live-glass card in the screen's own overlay portal, never a Material grey
 * panel. Its surface, rim, buttons and motion all come from the global system, so changing the glass
 * preset or Motion in Settings changes it at once. Accent is never a fill: buttons are neutral glass
 * with the theme's ink; [destructive] only colours the confirm text.
 */
@Composable
fun SavelinGlassDialog(
    onDismiss: () -> Unit,
    title: String? = null,
    confirm: String? = null,
    onConfirm: () -> Unit = {},
    dismiss: String? = null,
    onDismissButton: (() -> Unit)? = null,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val c = clear
    val latest by rememberUpdatedState(onConfirm)
    GlassOverlay(animatesOut = true) {
        val leaving = LocalOverlayPresence.current?.leaving == true
        BackHandler(enabled = dismissible && !leaving, onBack = onDismiss)
        val appear = rememberOverlayAppear(enter = GlassMotion.morph(), exit = Fluid.dismiss())
        Box(Modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) }
                    .background(Color.Black.copy(alpha = if (c.dark) 0.42f else 0.22f))
                    .clickable(remember { MutableInteractionSource() }, null) { if (dismissible) onDismiss() },
            )
            Column(
                Modifier
                    .padding(24.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .floatingLayer {
                        val s = 0.92f + 0.08f * appear.value.coerceIn(0f, 1f)
                        scaleX = s
                        scaleY = s
                        alpha = appear.value.coerceIn(0f, 1f)
                    }
                    .then(SavelinGlass.modifier(RoundedRectangle(28.dp)))
                    .clickable(remember { MutableInteractionSource() }, null) {}
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (title != null) {
                    Text(title, style = ClearType.Headline, color = SavelinGlass.ink)
                }
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) { content() }
                if (confirm != null || dismiss != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        if (dismiss != null) {
                            GlassPillButton(onClick = { (onDismissButton ?: onDismiss)() }, height = 46.dp) {
                                Text(dismiss, style = ClearType.BodyStrong, color = SavelinGlass.ink, maxLines = 1)
                            }
                        }
                        if (confirm != null) {
                            GlassPillButton(
                                onClick = { if (confirmEnabled) latest() },
                                height = 46.dp,
                                tap = if (confirmEnabled) Feel.Tap else null,
                            ) {
                                Text(
                                    confirm,
                                    style = ClearType.BodyStrong,
                                    color = if (!confirmEnabled) c.inkFaint else if (destructive) com.pinfold.app.ui.glass.theme.Candy.Tomato else SavelinGlass.ink,
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
