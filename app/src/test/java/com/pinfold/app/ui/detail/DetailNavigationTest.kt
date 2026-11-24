package com.pinfold.app.ui.detail

import androidx.compose.ui.unit.dp
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.data.model.Tag
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

    @Test
    fun hashtagsAreNormalisedDeduplicatedAndCapped() {
        val bookmark = Bookmark(
            url = "https://example.com",
            originalUrl = "https://example.com",
            normalizedUrl = "https://example.com",
            hashtags = " News, #tech, tech , , ",
        )
        val tags = listOf(Tag(name = "Design"), Tag(name = "#design"), Tag(name = " news "))
        assertEquals(listOf("#News", "#tech", "#Design"), hashtagPills(bookmark, tags))
        val many = Bookmark(
            url = "https://example.com",
            originalUrl = "https://example.com",
            normalizedUrl = "https://example.com",
            hashtags = (1..20).joinToString(",") { "tag$it" },
        )
        assertEquals(HASHTAG_LIMIT, hashtagPills(many, emptyList()).size)
        assertTrue(hashtagPills(Bookmark(url = "", originalUrl = "", normalizedUrl = ""), emptyList()).isEmpty())
    }

    @Test
    fun collapsedLineCountsAreThreeAndUnlimited() {
        assertEquals(3, collapsedMaxLines(false))
        assertEquals(Int.MAX_VALUE, collapsedMaxLines(true))
        assertEquals(3, COLLAPSED_DESCRIPTION_LINES)
        assertEquals(2, COLLAPSED_TITLE_LINES)
    }

    @Test
    fun metadataTopOffsetAddsGapToHeroHeight() {
        assertEquals(100.dp, metadataTopOffset(88.dp, 12.dp))
        assertEquals(12.dp, metadataTopOffset(0.dp, 12.dp))
        assertEquals(0.dp, metadataTopOffset(0.dp, 0.dp))
    }

    @Test
    fun detailStatsSkipsBlankFieldsAndFallsBackToCreatedAt() {
        val bookmark = Bookmark(
            url = "https://example.com",
            originalUrl = "https://example.com",
            normalizedUrl = "https://example.com",
            postedAt = "19 Nov 2026",
            likesText = "283K",
            commentsText = "  ",
        )
        assertEquals(listOf("19 Nov 2026", "283K"), detailStats(bookmark).map { it.value })
        val fallback = bookmark.copy(postedAt = "", likesText = "", commentsText = "", createdAt = 0L)
        val stats = detailStats(fallback)
        assertEquals(1, stats.size)
        assertTrue(stats[0].value.isNotBlank())
    }

    @Test
    fun metadataPanelStaysFixedAtTwentyPercentFromBottom() {
        assertEquals(0.20f, METADATA_BOTTOM_FRACTION, 0.0001f)
        assertEquals(160.dp, metadataBottomOffset(800.dp))
        assertEquals(0.dp, metadataBottomOffset(0.dp))
    }

    @Test
    fun metadataPanelHeightLeavesRoomAboveAndTitleTracksFixedOffset() {
        assertEquals(616.dp, metadataPanelMaxHeight(800.dp, 160.dp))
        assertEquals(0.dp, metadataPanelMaxHeight(800.dp, 800.dp))
        assertEquals(440.dp, titleTopOffset(800.dp, 160.dp, 200.dp))
        assertEquals(0.dp, titleTopOffset(800.dp, 160.dp, 800.dp))
    }

    @Test
    fun descriptionMaxLinesIsThreeCollapsedAndUnlimitedExpanded() {
        assertEquals(3, descriptionMaxLines(false))
        assertEquals(Int.MAX_VALUE, descriptionMaxLines(true))
        assertEquals(descriptionMaxLines(false), collapsedMaxLines(false))
        assertEquals(descriptionMaxLines(true), collapsedMaxLines(true))
    }

    @Test
    fun expandedDescriptionScrollHeightIsFixedAt200Dp() {
        assertEquals(200.dp, DESCRIPTION_SCROLL_MAX_HEIGHT)
    }

    @Test
    fun descriptionOnlyNeverIncludesStatsOrFallbackMetadata() {
        val bookmark = Bookmark(
            url = "https://example.com",
            originalUrl = "https://example.com",
            normalizedUrl = "https://example.com",
            description = "  Only the description  ",
            siteName = "Example",
            author = "Ada",
            postedAt = "19 Nov 2026",
            likesText = "283K",
            commentsText = "42",
        )
        assertEquals("Only the description", descriptionOnly(bookmark))
        assertTrue(!descriptionOnly(bookmark).contains("283K"))
        assertTrue(!descriptionOnly(bookmark).contains("Ada"))
        assertTrue(descriptionOnly(bookmark.copy(description = "  ")).isEmpty())
    }
}
