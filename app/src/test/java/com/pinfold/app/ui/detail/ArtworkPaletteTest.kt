package com.pinfold.app.ui.detail

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPaletteTest {
    private fun solid(rgb: Int, count: Int = 400): IntArray =
        IntArray(count) { (0xFF shl 24) or rgb }

    private fun hueOf(color: Color): Float {
        val r = (color.red * 255f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f).toInt().coerceIn(0, 255)
        return rgbToHsv(r, g, b)[0]
    }

    private fun hsvOf(color: Color): FloatArray {
        val r = (color.red * 255f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f).toInt().coerceIn(0, 255)
        return rgbToHsv(r, g, b)
    }

    @Test
    fun extractedHuesFollowTheArtwork() {
        listOf(
            0xFFD97123.toInt() to 10f..45f,
            0xFF1E5ADC.toInt() to 195f..240f,
            0xFF8B3FD1.toInt() to 255f..295f,
            0xFF3FA34D.toInt() to 100f..150f,
        ).forEach { (rgb, expected) ->
            val hue = hueOf(extractPalette(solid(rgb)).dominant)
            assertTrue("hue=$hue", hue in expected)
        }
    }

    @Test
    fun baseIsBrightImageDerivedAndAccentsAreRich() {
        val palette = extractPalette(solid(0xFF1E5ADC.toInt()))
        val baseHsv = hsvOf(palette.base)
        assertTrue("value=${baseHsv[2]}", baseHsv[2] in 0.55f..0.92f)
        assertTrue("saturation=${baseHsv[1]}", baseHsv[1] in 0.30f..0.85f)
        assertTrue(palette.base.alpha >= 0.99f)
        assertTrue(palette.buckets >= 1)
        assertTrue(palette.accents.size >= 4)
        assertTrue(palette.accents.none(::isDim))
    }

    @Test
    fun mostPopulatedVividColourBecomesDominant() {
        val pixels = solid(0xFF1E5ADC.toInt(), 300) + solid(0xFFD97123.toInt(), 60)
        val hue = hueOf(extractPalette(pixels).dominant)
        assertTrue("hue=$hue", hue in 195f..240f)
    }

    @Test
    fun greyscaleAndNearBlackStillProduceVisibleAccents() {
        listOf(0xFF808080.toInt(), 0xFF0A0A10.toInt(), 0xFF0F0F12.toInt()).forEach { rgb ->
            val palette = extractPalette(solid(rgb))
            assertTrue(palette.accents.size >= 3)
            assertTrue(palette.accents.none(::isDim))
            assertTrue(palette.accents.any { maxOf(it.red, it.green, it.blue) >= 0.5f })
        }
    }

    @Test
    fun transparentPixelsAreIgnoredWithoutDimmingThePalette() {
        val pixels = IntArray(200) + solid(0xFF0F0F12.toInt(), 200)
        val palette = extractPalette(pixels)
        assertTrue(palette.accents.isNotEmpty())
        assertTrue(palette.accents.none(::isDim))
    }

    @Test
    fun emptyInputUsesAStableVisiblePalette() {
        val palette = extractPalette(IntArray(0))
        assertEquals(0, palette.buckets)
        assertTrue(palette.accents.size >= 3)
        assertTrue(palette.accents.none(::isDim))
    }

    @Test
    fun fallbackPaletteIsDeterministicAndDifferentPerBookmark() {
        val first = fallbackPalette(11L)
        assertEquals(first, fallbackPalette(11L))
        assertNotEquals(first.dominant, fallbackPalette(12L).dominant)
        assertEquals(4, first.accents.size)
        assertEquals(0, first.buckets)
        assertTrue(hsvOf(first.base)[2] >= 0.70f)
        assertTrue(first.accents.none(::isDim))
        assertTrue(first.accents.distinct().size >= 3)
    }

    @Test
    fun downsamplingShrinksTheImageButKeepsItsColour() {
        val pixels = solid(0xFF1E5ADC.toInt(), 512 * 512)
        val small = downsampleArgb(pixels, 512, 512)
        assertTrue(small.size < pixels.size)
        assertTrue(small.isNotEmpty())
        assertTrue(small.all { (it and 0xFFFFFF) == 0x1E5ADC })
        assertEquals(0, downsampleArgb(pixels, 0, 512).size)
    }

    @Test
    fun bloomsAreSoftOverlappingAndImageDerived() {
        val palette = extractPalette(solid(0xFFD97123.toInt()))
        val blooms = paletteBlooms(palette)
        assertEquals(BLOOM_COUNT, blooms.size)
        assertTrue(blooms.all { it.x in 0f..1f && it.y in 0f..1f })
        assertTrue(blooms.all { it.radius > 0.4f && it.radius < 0.8f })
        assertTrue(blooms.all { it.alpha > 0f && it.alpha <= BLOOM_ALPHA })
        assertTrue(blooms.all { palette.accents.contains(it.color) })
    }

    @Test
    fun bottomFadeIsSmoothAndMonotonic() {
        assertEquals(0f, bottomFade(-1f), 0.0001f)
        assertEquals(1f, bottomFade(2f), 0.0001f)
        var previous = bottomFade(0f)
        for (i in 1..100) {
            val value = bottomFade(i / 100f)
            assertTrue(value >= previous)
            previous = value
        }
        assertFalse(bottomFade(0.5f) == bottomFade(0.51f))
    }
}
