package com.pinfold.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlNormalizerTest {
    @Test
    fun lowercasesHostAndDropsFragment() {
        assertEquals(
            "https://example.com/article",
            UrlNormalizer.normalize("HTTPS://EXAMPLE.COM/article#comments"),
        )
    }

    @Test
    fun stripsTrackingParameters() {
        assertEquals(
            "https://example.com/p?keep=1",
            UrlNormalizer.normalize(
                "https://example.com/p?utm_source=share&fbclid=abc&keep=1&gclid=xyz#top",
            ),
        )
    }

    @Test
    fun keepsYoutubeVideoId() {
        assertEquals(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            UrlNormalizer.normalize("https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=abc&feature=share"),
        )
    }

    @Test
    fun normalizesMobileInstagramHost() {
        assertEquals(
            "https://www.instagram.com/p/ABC123",
            UrlNormalizer.normalize("https://m.instagram.com/p/ABC123/?igsh=xyz"),
        )
    }

    @Test
    fun detectsShortenersAndTracking() {
        assertTrue(UrlNormalizer.isShortener("https://t.co/abc"))
        assertFalse(UrlNormalizer.isShortener("https://example.com/abc"))
        assertTrue(UrlNormalizer.hasTracking("https://example.com/?utm_medium=x"))
    }
}
