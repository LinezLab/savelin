package com.pinfold.app.ui.liquidglass

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogWindowProvider

/* OPT-IN SWITCH */

class GlassSettings(initial: Boolean = false) {
    var enabled by mutableStateOf(initial)
}

val LocalGlassSettings = staticCompositionLocalOf { GlassSettings() }

/** Wrap your app (or one screen) once. Hoist/persist GlassSettings (DataStore) if you want it to survive restarts. */
@Composable
fun GlassProvider(settings: GlassSettings, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlassSettings provides settings, content = content)
}

/** Drop this in your settings screen to test. */
@Composable
fun GlassToggleRow(modifier: Modifier = Modifier, onEnabledChange: (Boolean) -> Unit = {}) {
    val s = LocalGlassSettings.current
    Row(
        modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Glass UI (experimental)")
        Switch(checked = s.enabled, onCheckedChange = {
            s.enabled = it
            onEnabledChange(it)
        })
    }
}

/* CORE EFFECTS */

/**
 * Vibrancy: composites the content with BlendMode.Plus ("plus-lighter"),
 * so it ADDS light to whatever is behind it instead of covering it.
 * This is what makes dim white icons/text take on the background hue.
 */
fun Modifier.liquidVibrancy(): Modifier = drawWithContent {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply { blendMode = BlendMode.Plus }
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
        drawContent()
        canvas.restore()
    }
}

/**
 * Glass surface: translucent tint + top-down sheen + specular edge.
 * Returns the modifier UNCHANGED when the toggle is off.
 */
@Composable
fun Modifier.liquidSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.White.copy(alpha = 0.10f),
): Modifier {
    if (!LocalGlassSettings.current.enabled) return this
    return this
        .clip(shape)
        .background(tint)
        .background(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.02f))
            )
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.60f), // lit top-left edge
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.25f), // bounce light bottom-right
                )
            ),
            shape = shape
        )
}

/**
 * Put on your album art / background image: blurs, scales up so blurred edges
 * are hidden, and darkens it so white controls always have contrast.
 * (Modifier.blur is a no-op below Android 12, the dark overlay still keeps contrast.)
 */
@Composable
fun Modifier.glassBackdrop(blurRadius: Int = 60, darken: Float = 0.45f): Modifier {
    if (!LocalGlassSettings.current.enabled) return this
    return this
        .graphicsLayer { scaleX = 1.3f; scaleY = 1.3f }
        .blur(blurRadius.dp)
        .drawWithContent {
            drawContent()
            drawRect(Color.Black.copy(alpha = darken))
        }
}

/* TEXT & ICONS */

/**
 * secondary = true  -> dim white + vibrancy (timestamps, captions, inactive icons)
 * secondary = false -> solid white (primary labels)
 * Glass off -> plain Text with your existing styling.
 */
@Composable
fun VibrantText(
    text: String,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    if (!LocalGlassSettings.current.enabled) {
        Text(text, modifier, color = color, maxLines = maxLines, overflow = overflow, style = style)
        return
    }
    val m = if (secondary) modifier.liquidVibrancy() else modifier
    Text(
        text,
        m,
        color = if (color != Color.Unspecified) color else Color.White.copy(alpha = if (secondary) 0.45f else 1f),
        maxLines = maxLines,
        overflow = overflow,
        style = style
    )
}

/** Wrap any Icon: `VibrantIcon { Icon(Icons.Default.List, null, tint = Color.White.copy(0.45f)) }` */
@Composable
fun VibrantIcon(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val on = LocalGlassSettings.current.enabled
    Box(if (on) modifier.liquidVibrancy() else modifier) { content() }
}

/* COMPONENTS */

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    if (!LocalGlassSettings.current.enabled) {
        Button(onClick = onClick, modifier = modifier, content = content) // your old look
        return
    }
    Row(
        modifier
            .liquidSurface(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun GlassPill(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!LocalGlassSettings.current.enabled) {
        Surface(
            onClick = onClick,
            shape = CircleShape,
            tonalElevation = if (selected) 4.dp else 0.dp,
            modifier = modifier
        ) { Text(text, Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) }
        return
    }
    val tint = if (selected) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f)
    Row(
        modifier
            .liquidSurface(CircleShape, tint)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        VibrantText(text, secondary = !selected)
    }
}

/**
 * Pop-up. With glass on, also asks Android 12+ to blur the window BEHIND the dialog
 * (real backdrop blur). Falls back to a plain translucent panel if the device
 * doesn't support cross-window blur.
 */
@Composable
fun GlassPopup(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val on = LocalGlassSettings.current.enabled
    Dialog(onDismissRequest = onDismiss) {
        if (on) {
            val view = LocalView.current
            SideEffect {
                val window = (view.parent as? DialogWindowProvider)?.window
                if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val attrs = window.attributes
                    attrs.blurBehindRadius = 40
                    window.attributes = attrs
                }
            }
            Column(
                Modifier
                    .liquidSurface(RoundedCornerShape(32.dp), tint = Color(0x66150000))
                    .padding(24.dp),
                content = content
            )
        } else {
            Surface(shape = RoundedCornerShape(28.dp), tonalElevation = 6.dp) {
                Column(Modifier.padding(24.dp), content = content) // your old look
            }
        }
    }
}
