package com.pinfold.app.ui.detail

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientCompositionTest {
    @Test
    fun haloIsOversizedOpaqueAtTheEdgeAndFadesBelowIt() {
        assertTrue(HALO_SCALE > 1.2f)
        val heroHeight = 600f
        val haloHeight = heroHeight + HALO_OVERSHOOT_DP
        val tailStart = haloTailStart(haloHeight)
        assertTrue("tailStart=$tailStart", tailStart in 0.7f..0.99f)
        assertEquals(1f, haloAlpha(tailStart, heroHeight / haloHeight), 0.0001f)
        assertEquals(0f, haloAlpha(tailStart, 1f), 0.0001f)

        var previous = 1f
        for (i in 0..100) {
            val progress = tailStart + (1f - tailStart) * (i / 100f)
            val alpha = haloAlpha(tailStart, progress)
            assertTrue(alpha <= previous + 0.0001f)
            previous = alpha
        }
    }

    @Test
    fun crossfadeIsPerBookmarkAndReduceMotionSnaps() {
        assertFalse(crossfadeChanged(null, 7L))
        assertFalse(crossfadeChanged(7L, 7L))
        assertTrue(crossfadeChanged(7L, 8L))
        assertEquals(0, crossfadeDurationMs(true))
        assertEquals(CROSSFADE_MS, crossfadeDurationMs(false))
        assertTrue(CROSSFADE_MS in 200..1200)
    }

    @Test
    fun fallbackAmbienceHasNoImageAndAStablePalette() {
        val ambience = fallbackAmbience(42L)
        assertEquals(ArtworkSource.FALLBACK, ambience.source)
        assertNull(ambience.image)
        assertEquals(42L, ambience.id)
        assertEquals(fallbackPalette(42L), ambience.palette)
        assertTrue(ambience.palette.accents.none(::isDim))
    }

    @Test
    fun diagnosticsDescribeLayersAndStayRedacted() {
        ArtworkDiagnostics.record(
            ArtworkSnapshot(
                source = ArtworkSource.IMAGE,
                buckets = 5,
                accents = listOf("#FFD97123"),
            ),
        )
        val text = ArtworkDiagnostics.format()
        assertTrue(text.contains("source=IMAGE"))
        assertTrue(text.contains("paletteBuckets=5"))
        assertTrue(text.contains("#FFD97123"))
        assertTrue(text.contains("layers: image="))
        assertTrue(text.contains("hero: halo="))
        assertFalse(text.contains("feather="))
        assertTrue(text.contains("bottomFade="))
        assertTrue(text.contains("crossfade=${CROSSFADE_MS}ms"))
        assertFalse(text.contains("bookmarkId"))
        assertFalse(text.contains("path="))
        assertFalse(text.contains("url="))
        assertTrue(ArtworkDiagnostics.last()!!.summary.contains("palette=5"))
    }

    @Test
    fun accentHexIsEightDigits() {
        assertEquals("#FFD97123", Color(0xD9, 0x71, 0x23).hex())
    }
}
