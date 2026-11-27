/*
 * MIT License
 *
 * Copyright (c) 2026 Dimitris Vlachos
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Adapted from DimiVlachos/compose-lab, files
 * core/presentation/components/pullcord/{PullCordLamp,PullCordState,LampRig}.kt.
 */
package com.pinfold.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val Rod = 52.dp
private val GrabRadius = 24.dp
private val ClickPull = 48.dp
private val Brass = Color(0xFFB08D57)

/**
 * Minimal pull-cord theme switch. Grab the bead and pull down at least 48 dp (a sideways tug only
 * swings) to cycle AUTO -> LIGHT -> DARK -> AUTO once per pull. Adapted from compose-lab's pullcord
 * (MIT, Copyright (c) 2026 Dimitris Vlachos).
 */
@Composable
fun PullCord(
    mode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    across: Float = 0.5f,
    label: String = "Theme cord",
) {
    val systemDark = isSystemInDarkTheme()
    val litNow = mode == "DARK" || (mode == "AUTO" && systemDark)
    val currentMode by rememberUpdatedState(mode)
    val currentLit by rememberUpdatedState(litNow)
    val change by rememberUpdatedState(onModeChange)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()

    val toggle: () -> Unit = {
        val next = when (currentMode) {
            "AUTO" -> "LIGHT"
            "LIGHT" -> "DARK"
            else -> "AUTO"
        }
        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
        change(next)
    }

    val state = remember { CordState(toggle) }
    LaunchedEffect(state.held) { if (!state.held) state.settle() }

    Box(
        modifier
            .fillMaxSize()
            .onSizeChanged { state.place(it.width, density) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    var down: PointerInputChange? = null
                    while (down == null) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        down = event.changes.firstOrNull { it.changedToDown() && state.grab(it.position) }
                        if (down == null && event.changes.none { it.pressed }) return@awaitEachGesture
                    }
                    down.consume()
                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val move = event.changes.firstOrNull { it.id == down.id } ?: break
                            move.consume()
                            state.dragTo(move.position)
                            if (!move.pressed) break
                        }
                    } finally {
                        state.release()
                    }
                }
            },
    ) {
        Spacer(
            Modifier.fillMaxSize().drawBehind {
                val anchor = state.anchor()
                val rod = Rod.value * density
                val bead = state.bead()
                drawRoundRect(
                    Brass,
                    topLeft = Offset(anchor.x - 13f, 0f),
                    size = Size(26f, 6f),
                    cornerRadius = CornerRadius(3f),
                )
                drawLine(Brass, anchor, anchor + Offset(0f, rod), strokeWidth = 2f)
                drawCircle(
                    if (currentLit) Color(0xFFFFD98A) else Color(0xFF9E9E9E),
                    8f,
                    anchor + Offset(0f, rod),
                )
                drawLine(Color(0xFF6D4C41), anchor + Offset(0f, rod), bead, strokeWidth = 1.6f)
                drawCircle(Brass, 7f, bead)
            },
        )
        val rest = state.anchor() + Offset(0f, Rod.value * density)
        Spacer(
            Modifier.offset {
                IntOffset(
                    (rest.x - GrabRadius.value * density).roundToInt(),
                    (rest.y - GrabRadius.value * density).roundToInt(),
                )
            }
                .size(GrabRadius * 2)
                .semantics {
                    contentDescription = label
                    role = Role.Switch
                    stateDescription = "$mode theme"
                    onClick {
                        toggle()
                        scope.launch { state.pulse() }
                        true
                    }
                },
        )
    }
}

private class CordState(private val onToggle: () -> Unit) {
    private var pull by mutableFloatStateOf(0f)
    var width = 0f
    var density = 1f
    var across = 0.5f
    var held by mutableStateOf(false)
        private set
    private var grabbedAt = Offset.Zero
    private var clicked = false

    fun place(w: Int, d: Float) {
        width = w.toFloat()
        density = d
    }

    fun anchor(): Offset = Offset(width * across, 0f)
    fun bead(): Offset = anchor() + Offset(0f, Rod.value * density + pull)

    fun grab(at: Offset): Boolean {
        if (held || (at - bead()).getDistance() > GrabRadius.value * density) return false
        held = true
        grabbedAt = at
        clicked = false
        return true
    }

    fun dragTo(at: Offset) {
        if (!held) return
        val down = at.y - grabbedAt.y
        val along = abs(at.x - grabbedAt.x)
        pull = down.coerceAtLeast(0f)
        if (!clicked && down >= ClickPull.value * density && down >= 2f * along) {
            clicked = true
            onToggle()
        }
    }

    fun release() {
        held = false
    }

    suspend fun settle() {
        Animatable(pull).animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) { pull = value }
    }

    suspend fun pulse() {
        val anim = Animatable(pull)
        anim.animateTo(ClickPull.value * density, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) { pull = value }
        anim.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) { pull = value }
    }
}
