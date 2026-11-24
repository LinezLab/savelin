package com.pinfold.app.ui.detail

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
        assertEquals(MESH_FADE_MS, crossfadeDurationMs(false))
        assertEquals(900, MESH_FADE_MS)
    }

    @Test
    fun fallbackAmbienceHasNoMesh() {
        val ambience = fallbackAmbience(42L)
        assertEquals(ArtworkSource.FALLBACK, ambience.source)
        assertNull(ambience.mesh)
        assertEquals(42L, ambience.id)
    }

    @Test
    fun diagnosticsDescribeMeshLayersAndStayRedacted() {
        ArtworkDiagnostics.record(ArtworkSnapshot(source = ArtworkSource.IMAGE, seamPx = 812))
        val text = ArtworkDiagnostics.format()
        assertTrue(text.contains("source=IMAGE"))
        assertTrue(text.contains("mesh=6x6/32"))
        assertTrue(text.contains("seam=812px"))
        assertTrue(text.contains("fade=900ms"))
        assertFalse(text.contains("halo"))
        assertFalse(text.contains("feather="))
        assertFalse(text.contains("bloom="))
        assertFalse(text.contains("bookmarkId"))
        assertFalse(text.contains("path="))
        assertFalse(text.contains("url="))
        val summary = ArtworkDiagnostics.last()!!.summary
        assertTrue(summary.contains("mesh=6x6/32"))
        assertTrue(summary.contains("seam=812px"))
    }

    @Test
    fun heroBottomDissolveUses42PercentSmootherStep() {
        assertEquals(0.42f, HERO_BOTTOM_DISSOLVE_FRACTION, 0.0001f)
        assertEquals(0f, smootherStep(-0.5f), 0.0001f)
        assertEquals(1f, smootherStep(1.5f), 0.0001f)
        assertEquals(0f, smootherStep(0f), 0.0001f)
        assertEquals(1f, smootherStep(1f), 0.0001f)
        assertEquals(0.5f, smootherStep(0.5f), 0.0001f)
        var previous = smootherStep(0f)
        for (i in 1..100) {
            val value = smootherStep(i / 100f)
            assertTrue(value >= previous)
            previous = value
        }
        assertEquals(1f, 1f - smootherStep(0f), 0.0001f)
        assertEquals(0f, 1f - smootherStep(1f), 0.0001f)
    }
}
