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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

private val Rope = 214.dp
private val GrabRadius = 24.dp
private val ClickPull = 48.dp
private val Brass = Color(0xFFB08D57)
private val RopeInk = Color(0xFF6D4C41)
private const val SwingOmega = 4.2f
private const val SwingDamping = 0.32f
private const val SwingSeedAngle = 0.16f
private const val SwingStillAngle = 0.01f
private const val SwingStillSpeed = 0.01f
private const val MaxDragAngle = 1.1f
private const val MaxFrameSeconds = 0.05f

/**
 * Pull-cord theme switch hanging from the status bar. Grab the bead and pull down at least 48 dp
 * (a sideways tug only swings) to cycle AUTO -> LIGHT -> DARK -> AUTO once per pull. A damped
 * pendulum keeps the bead alive so the loop never dies; Reduce Motion stops it and snaps instead.
 * Adapted from compose-lab's pullcord (MIT, Copyright (c) 2026 Dimitris Vlachos).
 */
@Composable
fun PullCord(
    mode: String,
    onModeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    across: Float = 0.5f,
    label: String = "Theme cord",
    reduceMotion: Boolean = false,
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
    state.across = across
    LaunchedEffect(state.held, reduceMotion) { if (!state.held) state.settle(reduceMotion) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            state.resetSwing()
        } else {
            var lastFrame = 0L
            while (true) {
                withFrameNanos { frameTime ->
                    runCatching {
                        val dt = if (lastFrame == 0L) 0f else (frameTime - lastFrame) / 1_000_000f
                        lastFrame = frameTime
                        state.advance(dt)
                    }
                }
            }
        }
    }

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
                runCatching {
                    val anchor = state.anchor()
                    val bead = state.bead()
                    drawRoundRect(
                        Brass,
                        topLeft = Offset(anchor.x - 13f, 0f),
                        size = Size(26f, 6f),
                        cornerRadius = CornerRadius(3f),
                    )
                    drawLine(RopeInk, anchor, bead, strokeWidth = 2f)
                    drawCircle(Brass, 9f, bead)
                    drawCircle(
                        if (currentLit) Color(0xFFFFD98A) else Color(0xFF9E9E9E),
                        6f,
                        bead,
                    )
                }
            },
        )
        Spacer(
            Modifier.offset {
                val bead = state.bead()
                IntOffset(
                    (bead.x - GrabRadius.value * density).roundToInt(),
                    (bead.y - GrabRadius.value * density).roundToInt(),
                )
            }
                .size(GrabRadius * 2)
                .semantics {
                    contentDescription = label
                    role = Role.Switch
                    stateDescription = "$mode theme"
                    onClick {
                        toggle()
                        if (!reduceMotion) scope.launch { state.pulse() }
                        true
                    }
                },
        )
    }
}

internal class CordState(private val onToggle: () -> Unit) {
    private var pull by mutableFloatStateOf(0f)
    internal val angle = mutableFloatStateOf(0f)
    private var angularVelocity = 0f
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

    private fun ropeLength(): Float = Rope.value * density + pull

    fun bead(): Offset {
        val length = ropeLength()
        val a = angle.value
        return anchor() + Offset(sin(a) * length, cos(a) * length)
    }

    fun advance(dtSeconds: Float) {
        if (held || dtSeconds <= 0f) return
        val dt = dtSeconds.coerceAtMost(MaxFrameSeconds)
        val current = angle.value
        val accel = -SwingOmega * SwingOmega * current - SwingDamping * angularVelocity
        angularVelocity += accel * dt
        angle.value = current + angularVelocity * dt
        if (abs(angle.value) < SwingStillAngle && abs(angularVelocity) < SwingStillSpeed) {
            angle.value = SwingSeedAngle
            angularVelocity = 0f
        }
    }

    fun resetSwing() {
        angle.value = 0f
        angularVelocity = 0f
    }

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
        val dx = at.x - grabbedAt.x
        val along = abs(dx)
        pull = down.coerceAtLeast(0f)
        val length = ropeLength()
        angle.value = if (length > 0f) (dx / length).coerceIn(-MaxDragAngle, MaxDragAngle) else 0f
        angularVelocity = 0f
        if (!clicked && down >= ClickPull.value * density && down >= 2f * along) {
            clicked = true
            onToggle()
        }
    }

    fun release() {
        held = false
    }

    suspend fun settle(reduceMotion: Boolean) {
        if (reduceMotion) {
            pull = 0f
            resetSwing()
            return
        }
        Animatable(pull).animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) { pull = value }
    }

    suspend fun pulse() {
        val anim = Animatable(pull)
        anim.animateTo(ClickPull.value * density, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium)) { pull = value }
        anim.animateTo(0f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) { pull = value }
    }
}
