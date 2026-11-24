package com.pinfold.app.ui.splash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashSpecTest {
    @Test
    fun reducedDurationIsShorterThanNormal() {
        assertTrue(splashDurationMs(reduceMotion = true) < splashDurationMs(reduceMotion = false))
        assertEquals(SPLASH_DURATION_MS, splashDurationMs(reduceMotion = false))
        assertEquals(SPLASH_DURATION_REDUCED_MS, splashDurationMs(reduceMotion = true))
    }

    @Test
    fun logoWidthFractionIsEightyPercent() {
        assertEquals(0.8f, SPLASH_LOGO_WIDTH_FRACTION, 0.0001f)
    }

    @Test
    fun logoFontSizeScalesMeasuredWidthToEightyPercent() {
        assertEquals(288f, splashLogoFontSizeSp(1080f, 300f, 100f, 0.8f), 0.0001f)
    }

    @Test
    fun logoFractionIsClampedToUnitRange() {
        assertEquals(
            splashLogoFontSizeSp(1080f, 300f, 100f, 1f),
            splashLogoFontSizeSp(1080f, 300f, 100f, 1.5f),
            0.0001f,
        )
        assertEquals(
            splashLogoFontSizeSp(1080f, 300f, 100f, 0f),
            splashLogoFontSizeSp(1080f, 300f, 100f, -0.5f),
            0.0001f,
        )
        assertEquals(0f, splashLogoFontSizeSp(1080f, 300f, 100f, -1f), 0.0001f)
    }

    @Test
    fun zeroMeasuredWidthDoesNotDivideByZero() {
        val result = splashLogoFontSizeSp(1080f, 0f)
        assertTrue(result.isFinite())
        assertTrue(result > 0f)
    }

    @Test
    fun blankImagePathIsUnusable() {
        assertNull(usableImagePath(null))
        assertNull(usableImagePath(""))
        assertNull(usableImagePath("   "))
        assertEquals("/tmp/pic.webp", usableImagePath("/tmp/pic.webp"))
    }
}
