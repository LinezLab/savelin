package com.pinfold.app.ui.detail

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientCompositionTest {
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
        assertFalse(text.contains("halo"))
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

    @Test
    fun heroBottomDissolveRampIsClampedAndComplementary() {
        assertTrue(HERO_BOTTOM_DISSOLVE_FRACTION in 0f..1f)
        val start = heroDissolveStart(HERO_BOTTOM_DISSOLVE_FRACTION)
        assertEquals(1f - HERO_BOTTOM_DISSOLVE_FRACTION, start, 0.0001f)
        assertEquals(1f, start + HERO_BOTTOM_DISSOLVE_FRACTION, 0.0001f)
        assertEquals(1f, heroDissolveStart(-0.5f), 0.0001f)
        assertEquals(0f, heroDissolveStart(1.5f), 0.0001f)
        assertEquals(1f, heroDissolveStart(0f), 0.0001f)
        assertEquals(0f, heroDissolveStart(1f), 0.0001f)
    }
}
