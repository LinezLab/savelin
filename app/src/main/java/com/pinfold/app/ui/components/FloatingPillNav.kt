package com.pinfold.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.Capsule
import com.pinfold.app.ui.glass.GlassMotion
import com.pinfold.app.ui.glass.glass
import com.pinfold.app.ui.glass.theme.Candy
import com.pinfold.app.ui.glass.theme.clear
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

private const val SLOT_SEARCH = 0
private const val SLOT_PLUS = 1
private const val SLOT_SETTINGS = 2

@Composable
fun FloatingPillNav(
    searchActive: Boolean,
    query: String,
    visible: Boolean,
    onSearchToggle: () -> Unit,
    onQueryChange: (String) -> Unit,
    onAdd: () -> Unit,
    onAddLong: () -> Unit,
    onSettings: () -> Unit,
    onSettingsLong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = clear
    val density = LocalDensity.current
    var coords: LayoutCoordinates? = remember { null }
    val pressed = remember { Animatable(-1f) }
    val lens = remember { Animatable(1f) }

    val alpha by animateFloatAsState(if (visible) 1f else 0f, GlassMotion.settle(), label = "navAlpha")

    val width: Dp by animateDpAsState(
        targetValue = if (searchActive) 360.dp else 288.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "pillWidth",
    )

    Box(
        modifier
            .navigationBarsPadding()
            .padding(bottom = 18.dp)
            .padding(horizontal = 16.dp)
            .width(width)
            .height(64.dp)
            .alpha(alpha)
            .glass(Capsule, colors.glass, blur = 3.dp, lensHeight = 14.dp, lensAmount = 26.dp)
            .clip(Capsule)
            .padding(6.dp)
            .onGloballyPositioned { coords = it }
            .pointerInput(searchActive) {
                if (searchActive) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val slot = coords?.let { slotAt(down.position, it.size.width) } ?: SLOT_PLUS
                    pressed.snapTo(slot.toFloat())
                    lens.snapTo(slot.toFloat())
                    var longFired = false
                    var moved = false
                    var current = slot
                    var x = down.position.x
                    while (true) {
                        val event = withTimeoutOrNull(400L) { awaitPointerEvent() }
                        if (event == null) {
                            longFired = true
                            when (slot) {
                                SLOT_PLUS -> onAddLong()
                                SLOT_SETTINGS -> onSettingsLong()
                                SLOT_SEARCH -> onSearchToggle()
                            }
                            break
                        }
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        x = change.position.x
                        coords?.let { current = slotAt(Offset(x, 0f), it.size.width) }
                        if (abs(x - down.position.x) > with(density) { 10.dp.toPx() }) moved = true
                        if (!change.pressed) break
                    }
                    if (!longFired) {
                        val target = if (moved) current else slot
                        when (target) {
                            SLOT_SEARCH -> onSearchToggle()
                            SLOT_PLUS -> onAdd()
                            SLOT_SETTINGS -> onSettings()
                        }
                    }
                    pressed.animateTo(-1f, GlassMotion.settle())
                    lens.animateTo(1f, GlassMotion.fold())
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (searchActive) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search bookmarks", color = colors.inkSoft) },
                singleLine = true,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.align(Alignment.CenterEnd).size(40.dp).clickable { onSearchToggle() },
                contentAlignment = Alignment.Center,
            ) { Text("×", color = colors.ink) }
        } else {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                NavButton(
                    icon = NavIcons.Search,
                    label = "Search",
                    slot = SLOT_SEARCH,
                    pressed = pressed.value,
                    tint = colors.ink,
                    modifier = Modifier.fillMaxWidth(0.33f),
                )
                Box(Modifier.width(4.dp))
                NavButton(
                    icon = NavIcons.Plus,
                    label = "Add link",
                    slot = SLOT_PLUS,
                    pressed = pressed.value,
                    tint = com.pinfold.app.ui.glass.theme.Candy.Sky,
                    emphasised = true,
                    modifier = Modifier.fillMaxWidth(0.33f),
                )
                Box(Modifier.width(4.dp))
                NavButton(
                    icon = NavIcons.Settings,
                    label = "Settings",
                    slot = SLOT_SETTINGS,
                    pressed = pressed.value,
                    tint = colors.ink,
                    modifier = Modifier.fillMaxWidth(0.33f),
                )
            }
        }
    }
}

@Composable
private fun NavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    slot: Int,
    pressed: Float,
    tint: Color,
    modifier: Modifier = Modifier,
    emphasised: Boolean = false,
) {
    val active = pressed == slot.toFloat()
    val scale = if (active) 1.18f else 1f
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (emphasised) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).graphicsLayer { scaleX = scale; scaleY = scale },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
            }
        } else {
            Icon(
                icon,
                label,
                tint = tint,
                modifier = Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

private fun slotAt(x: Float, width: Int): Int = when {
    x < width / 3f -> SLOT_SEARCH
    x < width * 2f / 3f -> SLOT_PLUS
    else -> SLOT_SETTINGS
}
