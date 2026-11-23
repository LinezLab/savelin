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
        assertEquals(DetailsBackgroundMode.ARTWORK, resolveDetailsBackgroundMode(" artwork ", true))
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

    @Test
    fun ambientBloomsAreDerivedFromArtworkAndLayeredWithoutFlatFill() {
        val palette = extractArtworkPalette(solid(0xFF1E5ADC.toInt()))
        val blooms = ambientBlooms(palette)
        assertEquals(8, blooms.size)
        assertEquals(2, blooms.count { it.alpha == BLOOM_ALPHA_PRIMARY })
        assertEquals(3, blooms.count { it.alpha == BLOOM_ALPHA_SECONDARY })
        assertEquals(3, blooms.count { it.alpha == BLOOM_ALPHA_TERTIARY })
        assertEquals(8, blooms.map { it.x to it.y }.distinct().size)
        assertTrue(blooms.all { it.radius in 0.4f..0.9f })
        assertTrue(blooms.all { it.color.alpha >= 0.95f })
        assertTrue(blooms.any { hueOf(it.color) in 195f..235f })
        assertTrue(blooms.map { hueOf(it.color) }.distinct().size >= 3)
    }

    @Test
    fun readabilityScrimStaysSubtleAndTransparentInTheMiddle() {
        assertTrue(SCRIM_TOP_ALPHA in 0.2f..0.4f)
        assertTrue(SCRIM_BOTTOM_ALPHA in 0.4f..0.6f)
        assertTrue(SCRIM_BOTTOM_ALPHA > SCRIM_TOP_ALPHA)
        assertTrue(ARTWORK_IMAGE_ALPHA < 1f)
        assertTrue(ARTWORK_SCALE > 1f)
    }

    @Test
    fun diagnosticsReportModeImagePaletteAndAlphas() {
        ArtworkDiagnostics.record(
            ArtworkDecision(
                bookmarkId = 7L,
                setting = "ARTWORK",
                mode = DetailsBackgroundMode.ARTWORK,
                imageAvailable = true,
                imageFile = "preview.jpg",
                source = ArtworkSource.IMAGE,
                bucketCount = 5,
                accents = listOf("#FFD97123"),
                anyBlackOrTransparent = false,
                composed = true,
                crossfadeChanged = false,
            ),
        )
        val text = ArtworkDiagnostics.format()
        assertTrue(text.contains("resolvedMode=ARTWORK"))
        assertTrue(text.contains("imageAvailable=true"))
        assertTrue(text.contains("#FFD97123"))
        assertTrue(text.contains("bloomPrimary=$BLOOM_ALPHA_PRIMARY"))
        assertTrue(text.contains("scrimBottom=$SCRIM_BOTTOM_ALPHA"))
    }
}
