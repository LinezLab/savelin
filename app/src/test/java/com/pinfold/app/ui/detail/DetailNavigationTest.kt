package com.pinfold.app.ui.detail

import com.pinfold.app.data.model.Bookmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailNavigationTest {
    @Test
    fun swipeThresholdIsAtLeast96PxAndScalesWithScreen() {
        assertEquals(96f, swipeThresholdPx(0f), 0.0001f)
        assertEquals(96f, swipeThresholdPx(600f), 0.0001f)
        assertEquals(180f, swipeThresholdPx(1200f), 0.0001f)
        assertEquals(0.15f, SWIPE_HEIGHT_FRACTION, 0.0001f)
    }

    @Test
    fun navigationDeltaMapsUpToNextAndDownToPrevious() {
        val threshold = 100f
        assertEquals(0, navigationDelta(0f, threshold))
        assertEquals(0, navigationDelta(99f, threshold))
        assertEquals(-1, navigationDelta(100f, threshold))
        assertEquals(-1, navigationDelta(140f, threshold))
        assertEquals(1, navigationDelta(-100f, threshold))
        assertEquals(1, navigationDelta(-140f, threshold))
    }

    @Test
    fun titlePrefersDisplayTitle() {
        val bookmark = Bookmark(url = "https://example.com", originalUrl = "https://example.com", normalizedUrl = "https://example.com", title = "Raw", displayTitle = "Shown")
        assertEquals("Shown", titleFor(bookmark))
        assertEquals("Raw", titleFor(bookmark.copy(displayTitle = "  ")))
    }

    @Test
    fun hostIsExtractedFromUrl() {
        assertEquals("example.com", urlHost("https://example.com/a?b=1#c"))
        assertEquals("example.com", urlHost("example.com/path"))
        assertEquals("", urlHost(""))
    }

    @Test
    fun fallbackMetadataOmitsBlankFields() {
        val bookmark = Bookmark(
            url = "https://example.com/a",
            originalUrl = "https://example.com/a",
            normalizedUrl = "https://example.com/a",
            siteName = "Example",
            author = "Ada",
            authorHandle = "ada",
            postedAt = "",
            platform = "  ",
        )
        val lines = fallbackMetadataLines(bookmark)
        assertEquals(listOf("Example", "Ada", "@ada", "example.com"), lines)
        assertTrue(fallbackMetadataLines(bookmark.copy(url = "", siteName = "", author = "", authorHandle = "")).isEmpty())
    }
}
