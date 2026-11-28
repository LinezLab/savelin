package com.pinfold.app.ui.components

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class CordStateTest {
    @Test
    fun beadFollowsPendulumGeometry() {
        val state = CordState({})
        state.place(1000, 1f)
        val rest = state.bead()
        assertEquals(500f, rest.x, 0.0001f)
        assertTrue(rest.y > 0f)

        assertTrue(state.grab(rest))
        state.dragTo(Offset(600f, rest.y))
        state.release()
        val angle = state.angle.value
        assertTrue(angle > 0f)
        assertEquals(500f + sin(angle) * rest.y, state.bead().x, 0.0001f)
        assertEquals(cos(angle) * rest.y, state.bead().y, 0.0001f)
    }

    @Test
    fun dampedSwingIsReseededSoItNeverDies() {
        val state = CordState({})
        state.place(1000, 1f)
        state.resetSwing()
        var peak = 0f
        repeat(2000) {
            state.advance(0.016f)
            peak = maxOf(peak, abs(state.angle.value))
        }
        assertTrue(peak > 0.05f)
        assertTrue(state.angle.value.isFinite())
        state.advance(0f)
        assertTrue(state.angle.value.isFinite())
    }

    @Test
    fun pullFiresOnceAndReduceMotionSettlesToRest() {
        var toggles = 0
        val state = CordState({ toggles++ })
        state.place(1000, 1f)
        val rest = state.bead()
        assertTrue(state.grab(rest))
        state.dragTo(Offset(rest.x, rest.y + 60f))
        assertEquals(1, toggles)
        state.dragTo(Offset(rest.x, rest.y + 80f))
        assertEquals(1, toggles)
        state.release()
        assertTrue(state.bead().y > rest.y)
        runBlocking { state.settle(true) }
        assertEquals(500f, state.bead().x, 0.0001f)
        assertEquals(rest.y, state.bead().y, 0.0001f)
    }
}
