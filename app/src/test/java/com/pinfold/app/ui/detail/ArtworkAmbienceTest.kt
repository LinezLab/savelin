package com.pinfold.app.ui.detail

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkAmbienceTest {
    private fun solid(rgb: Int, count: Int = 400): IntArray = IntArray(count) { (0xFF shl 24) or rgb }

    private fun hueOf(color: Color): Float {
        val r = (color.red * 255f).toInt().coerceIn(0, 255)
        val g = (color.green * 255f).toInt().coerceIn(0, 255)
        val b = (color.blue * 255f).toInt().coerceIn(0, 255)
        return rgbToHsv(r, g, b)[0]
    }

    @Test
    fun paletteHuesFollowTheArtwork() {
        listOf(
            0xFFD97123.toInt() to 10f..45f,
            0xFF1E5ADC.toInt() to 195f..235f,
            0xFF8B3FD1.toInt() to 255f..295f,
            0xFF3FA34D.toInt() to 100f..145f,
        ).forEach { (rgb, expected) ->
            val hue = hueOf(extractArtworkTone(solid(rgb)).dominant)
            assertTrue("hue=$hue", hue in expected)
        }
    }

    @Test
    fun baseIsDarkAndDerivedFromTheDominantColour() {
        val tone = extractArtworkTone(solid(0xFFD97123.toInt()))
        assertTrue(tone.base.red < tone.dominant.red)
        assertTrue(tone.base.green < tone.dominant.green)
        assertTrue(tone.base.blue < tone.dominant.blue)
        assertTrue(tone.accents.size >= 4)
    }

    @Test
    fun mostFrequentVividColourWins() {
        val pixels = solid(0xFF1E5ADC.toInt(), 300) + solid(0xFFD97123.toInt(), 60)
        val hue = hueOf(extractArtworkTone(pixels).dominant)
        assertTrue("hue=$hue", hue in 195f..235f)
    }

    @Test
    fun neutralAndNearBlackArtworkStillYieldVisibleAccents() {
        listOf(0xFF808080.toInt(), 0xFF0A0A10.toInt(), 0xFF0F0F12.toInt()).forEach { rgb ->
            val tone = extractArtworkTone(solid(rgb))
            assertTrue(tone.accents.size >= 3)
            assertTrue(tone.accents.none(::isInvisible))
            assertTrue(tone.accents.any { maxOf(it.red, it.green, it.blue) >= 0.5f })
        }
    }

    @Test
    fun transparentPixelsNeverProduceInvisibleAccents() {
        val pixels = IntArray(200) { 0 } + solid(0xFF0F0F12.toInt(), 200)
        val tone = extractArtworkTone(pixels)
        assertTrue(tone.accents.isNotEmpty())
        assertTrue(tone.accents.none(::isInvisible))
    }

    @Test
    fun fallbackIsDeterministicImageLikeAndDifferentPerBookmark() {
        val first = fallbackTone("YouTube", "youtube", 11L)
        val same = fallbackTone("YouTube", "youtube", 11L)
        val other = fallbackTone("GitHub", "github", 12L)
        assertEquals(first, same)
        assertNotEquals(first.dominant, other.dominant)
        assertEquals(4, first.accents.size)
        assertEquals(0, first.buckets)
        assertTrue(first.accents.none(::isInvisible))
        assertTrue(first.accents.distinct().size >= 3)
        assertTrue(maxOf(first.base.red, first.base.green, first.base.blue) > 0f)

        val ambient = fallbackAmbience(42L, "Reddit", "reddit")
        assertEquals(ArtworkSource.FALLBACK, ambient.source)
        assertEquals(null, ambient.image)
        assertEquals(42L, ambient.id)
        assertEquals(MESH_TEX * MESH_TEX, ambient.meshTexels.size)
        assertTrue(ambient.meshTexels.all { (it ushr 24) == 0xFF })
    }

    @Test
    fun crossfadeDecisionIsPerBookmarkAndReduceMotionIsImmediate() {
        assertFalse(crossfadeChanged(null, 7L))
        assertFalse(crossfadeChanged(7L, 7L))
        assertTrue(crossfadeChanged(7L, 8L))
        assertEquals(0, crossfadeDurationMs(true))
        assertEquals(CROSSFADE_MS, crossfadeDurationMs(false))
        assertEquals(900, CROSSFADE_MS)
    }

    @Test
    fun diagnosticsDescribeTheMeshAndStayRedacted() {
        ArtworkDiagnostics.record(
            ArtworkSnapshot(
                mode = DetailArtworkMode.ARTWORK,
                source = ArtworkSource.IMAGE,
                paletteBuckets = 5,
                accents = listOf("#FFD97123"),
            ),
        )
        val text = ArtworkDiagnostics.format()
        assertTrue(text.contains("mode=ARTWORK source=IMAGE"))
        assertTrue(text.contains("paletteBuckets=5"))
        assertTrue(text.contains("#FFD97123"))
        assertTrue(text.contains("mesh: grid=6x6 texels=32"))
        assertTrue(text.contains("seam=anchored"))
        assertTrue(text.contains("fade=900ms"))
        assertFalse(text.contains("bookmarkId"))
        assertFalse(text.contains("path="))
        assertFalse(text.contains("url="))
        assertTrue(ArtworkDiagnostics.last()!!.summary.contains("mesh=6x6/32"))
    }

    @Test
    fun accentHexIsEightDigits() {
        assertEquals("#FFD97123", Color(0xD9, 0x71, 0x23).hex())
    }

    @Test
    fun heroEdgeIsALongContinuousAlphaRampWithNoHardCut() {
        assertTrue("feather must be long", HERO_EDGE_FEATHER_FRACTION >= 0.25f)
        val start = heroEdgeFeatherStart
        assertTrue("start=$start", start in 0.5f..0.8f)
        assertEquals(1f, heroEdgeAlphaAt(0f), 0.0001f)
        assertEquals(1f, heroEdgeAlphaAt(start), 0.0001f)
        assertEquals(0f, heroEdgeAlphaAt(1f), 0.0001f)
        assertEquals(0.5f, heroEdgeAlphaAt((start + 1f) / 2f), 0.02f)

        var previous = 1f
        var maxStep = 0f
        for (i in 0..200) {
            val progress = start + (1f - start) * (i / 200f)
            val alpha = heroEdgeAlphaAt(progress)
            assertTrue("alpha increased at $progress", alpha <= previous + 0.0001f)
            maxStep = maxOf(maxStep, previous - alpha)
            previous = alpha
        }
        assertTrue("largest alpha step $maxStep must be imperceptible", maxStep <= 0.01f)
    }
}
