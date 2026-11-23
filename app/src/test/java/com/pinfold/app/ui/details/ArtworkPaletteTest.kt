package com.pinfold.app.ui.details

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkPaletteTest {
    private fun solid(rgb: Int, count: Int = 400): IntArray = IntArray(count) { (0xFF shl 24) or rgb }

    private fun hueOf(color: Color): Float = rgbToHsv(
        (color.red * 255f).toInt().coerceIn(0, 255),
        (color.green * 255f).toInt().coerceIn(0, 255),
        (color.blue * 255f).toInt().coerceIn(0, 255),
    )[0]

    @Test
    fun paletteHuesFollowArtworkRatherThanFixedColours() {
        val cases = listOf(
            0xFFD97123.toInt() to 15f..45f,
            0xFF1E5ADC.toInt() to 195f..235f,
            0xFF8B3FD1.toInt() to 255f..295f,
            0xFF3FA34D.toInt() to 100f..145f,
        )
        cases.forEach { (rgb, expected) ->
            val palette = extractArtworkPalette(solid(rgb))
            assertTrue("hue ${hueOf(palette.dominant)} from ${Integer.toHexString(rgb)}", hueOf(palette.dominant) in expected)
        }
    }

    @Test
    fun baseIsDarkDerivedFromDominant() {
        val palette = extractArtworkPalette(solid(0xFFD97123.toInt()))
        assertTrue(palette.base.red < palette.dominant.red)
        assertTrue(palette.base.green < palette.dominant.green)
        assertTrue(palette.base.blue < palette.dominant.blue)
        assertTrue(palette.colors.size >= 4)
    }

    @Test
    fun mostFrequentVividColourWins() {
        val pixels = solid(0xFF1E5ADC.toInt(), 300) + solid(0xFFD97123.toInt(), 60)
        val hue = hueOf(extractArtworkPalette(pixels).dominant)
        assertTrue("hue=$hue", hue in 195f..235f)
    }

    @Test
    fun neutralArtworkStillProducesDerivedPalette() {
        val palette = extractArtworkPalette(solid(0xFF808080.toInt()))
        assertEquals(0f, hueOf(palette.dominant), 1f)
        assertTrue(palette.colors.isNotEmpty())
    }

    @Test
    fun detailsBackgroundModeIsArtworkByDefaultAndHonoursTheSetting() {
        assertEquals(DetailsBackgroundMode.ARTWORK, resolveDetailsBackgroundMode(null, true))
        assertEquals(DetailsBackgroundMode.ARTWORK, resolveDetailsBackgroundMode("ARTWORK", true))
        assertEquals(DetailsBackgroundMode.ARTWORK, resolveDetailsBackgroundMode("PICTURE", true))
        assertEquals(DetailsBackgroundMode.GENERATED, resolveDetailsBackgroundMode("GENERATED", true))
        assertEquals(DetailsBackgroundMode.THEME, resolveDetailsBackgroundMode("THEME", true))
        assertEquals(DetailsBackgroundMode.THEME, resolveDetailsBackgroundMode("SOLID", true))
        assertEquals(DetailsBackgroundMode.ARTWORK, resolveDetailsBackgroundMode("ARTWORK", false))
    }

    @Test
    fun artworkModeNeverDegradesToThemeWithoutAnImage() {
        assertEquals(DetailsBackgroundMode.ARTWORK, resolveDetailsBackgroundMode("ARTWORK", false))
        assertEquals(DetailsBackgroundMode.ARTWORK, resolveDetailsBackgroundMode(null, false))
        assertEquals(DetailsBackgroundMode.ARTWORK, resolveDetailsBackgroundMode("PICTURE", false))
        assertEquals(DetailsBackgroundMode.ARTWORK, resolveDetailsBackgroundMode(" artwork ", false))
        assertEquals(DetailsBackgroundMode.GENERATED, resolveDetailsBackgroundMode("generated", false))
        assertEquals(DetailsBackgroundMode.THEME, resolveDetailsBackgroundMode(" solid ", false))
    }

    @Test
    fun ambientUsesSeveralClearlyVisibleDerivedBlooms() {
        val palette = extractArtworkPalette(solid(0xFF1E5ADC.toInt()))
        assertTrue("accents=${palette.colors.size}", palette.colors.size >= 3)
        assertTrue(BLOOM_ALPHA_PRIMARY >= 0.45f)
        assertTrue(BLOOM_ALPHA_SECONDARY >= 0.45f)
        assertTrue(palette.colors.distinct().size >= 3)
    }

    @Test
    fun nearBlackArtworkStillProducesNonBlackAmbient() {
        val palette = extractArtworkPalette(solid(0xFF0A0A10.toInt()))
        assertTrue(palette.colors.size >= 3)
        assertTrue(palette.colors.any { maxOf(it.red, it.green, it.blue) >= 0.2f })
        assertTrue(maxOf(palette.base.red, palette.base.green, palette.base.blue) > 0f)
        assertTrue(palette.colors.none { isEffectivelyBlackOrTransparent(it) })
    }

    @Test
    fun transparentAndNeutralPixelsNeverYieldBlackOrTransparentAccents() {
        val pixels = IntArray(200) { 0x00000000 } + solid(0xFF0F0F12.toInt(), 200)
        val palette = extractArtworkPalette(pixels)
        assertTrue(palette.colors.isNotEmpty())
        assertTrue(palette.colors.none { isEffectivelyBlackOrTransparent(it) })
    }

    @Test
    fun fallbackPaletteIsDeterministicVisibleAndDifferentPerBookmark() {
        val first = fallbackPalette("YouTube", "youtube", 11L)
        val same = fallbackPalette("YouTube", "youtube", 11L)
        val other = fallbackPalette("GitHub", "github", 12L)
        assertEquals(first.colors, same.colors)
        assertEquals(first.base, same.base)
        assertNotEquals(first.dominant, other.dominant)
        assertEquals(4, first.colors.size)
        assertEquals(0, first.bucketCount)
        assertTrue(first.colors.none { isEffectivelyBlackOrTransparent(it) })
        assertTrue(maxOf(first.base.red, first.base.green, first.base.blue) > 0f)
        assertTrue(first.colors.distinct().size >= 3)
    }

    @Test
    fun fallbackAmbientHasNoBitmapAndIsNonFlat() {
        val ambient = fallbackAmbient(42L, "Reddit", "reddit")
        assertEquals(null, ambient.blurred)
        assertEquals(ArtworkSource.FALLBACK, ambient.source)
        assertEquals(4, ambient.palette.colors.size)
        assertTrue(ambient.palette.colors.distinct().size > 1)
    }

    @Test
    fun argbHexIsEightDigits() {
        assertEquals("#FFD97123", Color(0xD9, 0x71, 0x23).argbHex())
    }
}
