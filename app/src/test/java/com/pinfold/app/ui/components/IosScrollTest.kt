package com.pinfold.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IosScrollTest {
    @Test
    fun springConstantsAreIosLike() {
        assertEquals(300f, SCROLL_BOUNCE_STIFFNESS, 0f)
        assertTrue(SCROLL_BOUNCE_DAMPING in 0f..1f)
        assertTrue(SCROLL_BOUNCE_DAMPING < 1f)
        assertTrue(SCROLL_REDUCED_STIFFNESS > SCROLL_BOUNCE_STIFFNESS)
    }

    @Test
    fun normalSpringSpecUsesBouncyConstants() {
        val spec = iosSpringSpec<Float>(reduceMotion = false)
        assertEquals(SCROLL_BOUNCE_DAMPING, spec.dampingRatio, 0f)
        assertEquals(SCROLL_BOUNCE_STIFFNESS, spec.stiffness, 0f)
    }

    @Test
    fun reducedSpringSpecDoesNotOvershoot() {
        val spec = iosSpringSpec<Float>(reduceMotion = true)
        assertEquals(1f, spec.dampingRatio, 0f)
        assertEquals(SCROLL_REDUCED_STIFFNESS, spec.stiffness, 0f)
    }

    @Test
    fun flingDistanceScalesWithVelocityAndIsCapped() {
        assertEquals(0f, iosFlingDistance(0f), 0f)
        assertEquals(200f, iosFlingDistance(900f), 0.0001f)
        assertEquals(-200f, iosFlingDistance(-900f), 0.0001f)
        assertEquals(SCROLL_FLING_MAX_PX, iosFlingDistance(1_000_000f), 0f)
        assertEquals(-SCROLL_FLING_MAX_PX, iosFlingDistance(-1_000_000f), 0f)
    }

    @Test
    fun flingDistanceIsFiniteForExtremeVelocity() {
        assertTrue(iosFlingDistance(Float.MAX_VALUE).isFinite())
        assertTrue(iosFlingDistance(-Float.MAX_VALUE).isFinite())
    }

    @Test
    fun flingCancelsOnlyWhenTheContainerLeavesAFrameUnconsumed() {
        assertFalse(flingHitBound(40f, 40f))
        assertFalse(flingHitBound(40f, 39.6f))
        assertTrue(flingHitBound(40f, 39.4f))
        assertTrue(flingHitBound(-40f, -30f))
        assertFalse(flingHitBound(0f, 0f))
    }
}
