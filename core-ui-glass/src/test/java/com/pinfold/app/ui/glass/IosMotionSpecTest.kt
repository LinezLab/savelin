package com.pinfold.app.ui.glass

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.ui.unit.IntOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IosMotionSpecTest {
    @Test
    fun iosConstantsMatchTheDesign() {
        assertEquals(0.55f, IosMotion.PressDamping, 0f)
        assertEquals(420f, IosMotion.PressStiffness, 0f)
        assertEquals(0.8f, IosMotion.LayoutDamping, 0f)
        assertEquals(320f, IosMotion.LayoutStiffness, 0f)
        assertEquals(0.7f, IosMotion.MenuDamping, 0f)
        assertEquals(360f, IosMotion.MenuStiffness, 0f)
    }

    @Test
    fun iosStyleIsDefaultLabelledAndPreselected() {
        assertEquals(0.55f, MotionStyle.Ios.bounce, 0f)
        assertEquals(1f, MotionStyle.Ios.speed, 0f)
        assertEquals(1.05f, MotionStyle.Ios.press, 0f)
        assertEquals(0.6f, MotionStyle.Ios.wobble, 0f)
        assertEquals(0.8f, MotionStyle.Ios.stretch, 0f)
        assertEquals(0.9f, MotionStyle.Ios.bloom, 0f)
        assertSame(MotionStyle.Ios, MotionStyle.Default)
        assertEquals("iOS", MotionStyle.Ios.label)
        assertTrue(MotionStyle.Presets.contains(MotionStyle.Ios))
    }

    @Test
    fun reduceMotionSwapsEveryIosSpecForTheShortFade() {
        MotionSettings.updateReduceMotion(false)
        assertTrue(IosMotion.Press !is TweenSpec<*>)
        assertTrue(IosMotion.Menu !is TweenSpec<*>)
        assertTrue(IosMotion.Layout !is TweenSpec<*>)
        assertTrue(IosMotion.Place !is TweenSpec<*>)
        MotionSettings.updateReduceMotion(true)
        assertTrue(IosMotion.Press is TweenSpec<*>)
        assertTrue(IosMotion.Menu is TweenSpec<*>)
        assertTrue(IosMotion.Layout is TweenSpec<*>)
        assertTrue(IosMotion.Place is TweenSpec<*>)
        assertTrue(IosMotion.alpha<Float>() is TweenSpec<*>)
        assertTrue(IosMotion.finiteAlpha<Float>() is TweenSpec<*>)
        MotionSettings.updateReduceMotion(false)
    }

    @Test
    fun layoutAndPlaceAreFiniteForAnimateItem() {
        val layout: FiniteAnimationSpec<Float> = IosMotion.Layout
        val place: FiniteAnimationSpec<IntOffset> = IosMotion.Place
        assertTrue(layout !== null)
        assertTrue(place !== null)
    }
}
