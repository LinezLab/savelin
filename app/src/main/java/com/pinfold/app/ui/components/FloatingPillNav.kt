package com.pinfold.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pinfold.app.ui.glass.Capsule
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
    var coords: LayoutCoordinates? by remember { mutableStateOf(null) }
    var pressedSlot by remember { mutableIntStateOf(-1) }
    var lensSlot by remember { mutableIntStateOf(1) }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "navAlpha")
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
                    val slot = coords?.let { slotAt(down.position.x, it.size.width) } ?: SLOT_PLUS
                    pressedSlot = slot
                    lensSlot = slot
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
                        coords?.let { current = slotAt(x, it.size.width) }
                        if (abs(x - down.position.x) > with(density) { 10.dp.toPx() }) moved = true
                        if (!change.pressed) break
                    }
                    if (!longFired) {
                        when (if (moved) current else slot) {
                            SLOT_SEARCH -> onSearchToggle()
                            SLOT_PLUS -> onAdd()
                            SLOT_SETTINGS -> onSettings()
                        }
                    }
                    pressedSlot = -1
                    lensSlot = 1
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
                Modifier.align(Alignment.CenterEnd).size(48.dp).clickable { onSearchToggle() },
                contentAlignment = Alignment.Center,
            ) { AppIcons.Icon(AppIcons.Close, "Close search", tint = colors.ink) }
        } else {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                NavButton(AppIcons.Search, "Search", SLOT_SEARCH, pressedSlot, colors.ink, Modifier.fillMaxWidth(0.33f))
                NavButton(AppIcons.Add, "Add link", SLOT_PLUS, pressedSlot, Candy.Sky, Modifier.fillMaxWidth(0.33f), emphasised = true)
                NavButton(AppIcons.Settings, "Settings", SLOT_SETTINGS, pressedSlot, colors.ink, Modifier.fillMaxWidth(0.33f))
            }
        }
    }
}

@Composable
private fun NavButton(
    icon: Int,
    label: String,
    slot: Int,
    pressedSlot: Int,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier,
    emphasised: Boolean = false,
) {
    val active = pressedSlot == slot || (slot == SLOT_PLUS && pressedSlot == SLOT_PLUS)
    val scale = if (active) 1.18f else 1f
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (emphasised) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color.White)
                    .graphicsLayer { scaleX = scale; scaleY = scale },
                contentAlignment = Alignment.Center,
            ) {
                AppIcons.Icon(icon, label, tint = tint, size = 24.dp)
            }
        } else {
            AppIcons.Icon(
                icon,
                label,
                tint = tint,
                size = 24.dp,
                modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            )
        }
    }
}

private fun slotAt(x: Float, width: Int): Int = when {
    x < width / 3f -> SLOT_SEARCH
    x < width * 2f / 3f -> SLOT_PLUS
    else -> SLOT_SETTINGS
}
