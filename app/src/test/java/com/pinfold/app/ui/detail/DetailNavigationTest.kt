package com.pinfold.app.ui.detail

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pinfold.app.data.model.Bookmark
import com.pinfold.app.ui.HOME_BACKGROUND_DARK
import com.pinfold.app.ui.HOME_BACKGROUND_LIGHT
import com.pinfold.app.ui.home.SONIL_LOGO_FONT_RES_NAME
import com.pinfold.app.ui.home.SONIL_LOGO_TEXT
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
    fun metadataPanelStaysFixedAtThirtyFivePercentFromBottom() {
        assertEquals(0.35f, METADATA_BOTTOM_FRACTION, 0.0001f)
        assertEquals(280.dp, metadataBottomOffset(800.dp))
        assertEquals(0.dp, metadataBottomOffset(0.dp))
    }

    @Test
    fun titleAnchorIsThirtyFivePercentOfScreenHeight() {
        assertEquals(0.35f, METADATA_BOTTOM_FRACTION, 0.0001f)
        assertEquals(280f, titleBottomOffsetPx(800f, METADATA_BOTTOM_FRACTION), 0.0001f)
        assertEquals(0f, titleBottomOffsetPx(0f, METADATA_BOTTOM_FRACTION), 0.0001f)
        assertEquals(420f, titleBottomOffsetPx(1200f, METADATA_BOTTOM_FRACTION), 0.0001f)
        assertEquals(0f, titleBottomOffsetPx(800f, -0.5f), 0.0001f)
        assertEquals(800f, titleBottomOffsetPx(800f, 1.5f), 0.0001f)
        assertEquals(280.dp, metadataBottomOffset(800.dp))
    }

    @Test
    fun metadataBodyFitsBetweenTitleAnchorAndBottomActions() {
        assertEquals(68.dp, DETAIL_ACTIONS_RESERVED_HEIGHT)
        assertEquals(3.dp, METADATA_SECTION_GAP)
        val bottomOffset = metadataBottomOffset(800.dp)
        val bodyMaxHeight = maxOf(0.dp, bottomOffset - DETAIL_ACTIONS_RESERVED_HEIGHT)
        assertEquals(212.dp, bodyMaxHeight)
        assertTrue(bottomOffset > DETAIL_ACTIONS_RESERVED_HEIGHT)
    }

    @Test
    fun metadataSectionGapIsThreeDpBetweenSections() {
        assertEquals(3f, metadataSectionGap(), 0f)
        assertEquals(3.dp, METADATA_SECTION_GAP)
    }

    @Test
    fun descriptionStartsExpandedOnEnteringDetail() {
        assertTrue(DESCRIPTION_STARTS_EXPANDED)
        assertEquals(Int.MAX_VALUE, descriptionMaxLines(DESCRIPTION_STARTS_EXPANDED))
        assertEquals(3, descriptionMaxLines(!DESCRIPTION_STARTS_EXPANDED))
    }

    @Test
    fun bottomButtonRowAddsFourDpVerticalMargin() {
        assertEquals(4.dp, BOTTOM_BUTTON_VERTICAL_MARGIN)
        assertEquals(68.dp, DETAIL_ACTIONS_RESERVED_HEIGHT)
    }

    @Test
    fun openLabelKeepsWhiteInkAndLetterSpacing() {
        assertEquals(Color.White, OPEN_TEXT_COLOR)
        assertEquals(2.sp, OPEN_TEXT_LETTER_SPACING)
    }

    @Test
    fun openTextUsesOverlayBlendWithWhiteInk() {
        assertEquals(Color.White, OPEN_TEXT_COLOR)
        assertEquals(BlendMode.Overlay, OPEN_TEXT_BLEND_MODE)
    }

    @Test
    fun detailMetadataInkIsWhiteInBothThemes() {
        assertEquals(Color.White, DETAIL_METADATA_INK)
    }

    @Test
    fun sonilLogoUsesNikeaFontResource() {
        assertEquals("nikea", SONIL_LOGO_FONT_RES_NAME)
        assertEquals("Sonil", SONIL_LOGO_TEXT)
    }

    @Test
    fun stickyDetailLogoDrawsOnceWithoutBlend() {
        assertEquals(22.sp, DETAIL_LOGO_FONT_SIZE)
        assertTrue(DETAIL_LOGO_FONT_SIZE.value < 64f)
    }

    @Test
    fun homeBackgroundTokensArePureBlackAndWhite() {
        assertEquals(Color.Black, HOME_BACKGROUND_DARK)
        assertEquals(Color.White, HOME_BACKGROUND_LIGHT)
    }

    @Test
    fun openLabelFontIsLargerThanHeadlineAndKeepsOutfitSemiBold() {
        assertEquals(30.sp, OPEN_TEXT_FONT_SIZE)
        assertEquals(34.sp, OPEN_TEXT_LINE_HEIGHT)
        assertEquals(FontWeight.SemiBold, OPEN_TEXT_FONT_WEIGHT)
        assertTrue(OPEN_TEXT_FONT_SIZE.value > 26f)
    }

    @Test
    fun descriptionDissolveFadesOnlyInsideTopAndBottomEdges() {
        val edge = 0.14f
        val stops = descriptionDissolveStops(edge)
        assertEquals(4, stops.size)
        assertEquals(0f, stops[0].first, 0f)
        assertEquals(Color.Transparent, stops[0].second)
        assertEquals(edge, stops[1].first, 0f)
        assertEquals(Color.Black, stops[1].second)
        assertEquals(1f - edge, stops[2].first, 0f)
        assertEquals(Color.Black, stops[2].second)
        assertEquals(1f, stops[3].first, 0f)
        assertEquals(Color.Transparent, stops[3].second)
        assertEquals(
            DESCRIPTION_DISSOLVE_EDGE_DP.value / DESCRIPTION_SCROLL_MAX_HEIGHT.value,
            edge,
            0.0001f,
        )
    }

    @Test
    fun descriptionDissolveEdgeOverlapsInnerPaddingButKeepsFirstLineReadable() {
        assertEquals(16.dp, DESCRIPTION_INNER_PADDING)
        assertEquals(28.dp, DESCRIPTION_DISSOLVE_EDGE_DP)
        assertTrue(DESCRIPTION_DISSOLVE_EDGE_DP > DESCRIPTION_INNER_PADDING)
        val edge = DESCRIPTION_DISSOLVE_EDGE_DP.value / DESCRIPTION_SCROLL_MAX_HEIGHT.value
        val stops = descriptionDissolveStops(edge)
        val paddingFraction = DESCRIPTION_INNER_PADDING.value / DESCRIPTION_SCROLL_MAX_HEIGHT.value
        val alphaAtPadding = (paddingFraction - stops[0].first) / (stops[1].first - stops[0].first)
        assertTrue(alphaAtPadding in 0.5f..1.0f)
        assertEquals(16f / 28f, alphaAtPadding, 0.0001f)
    }

    @Test
    fun metadataBodyHasVerticalGapAboveAndBelowInAdditionToSectionGap() {
        assertEquals(3.dp, METADATA_BODY_VERTICAL_GAP)
        assertEquals(METADATA_SECTION_GAP, METADATA_BODY_VERTICAL_GAP)
    }

    @Test
    fun shareGlyphMatchesBackAndMoreGlyphSizes() {
        assertEquals(44.dp, DETAIL_GLYPH_BUTTON_SIZE)
        assertEquals(24.dp, DETAIL_GLYPH_ICON_SIZE)
    }

    @Test
    fun metadataPanelHeightLeavesRoomAboveAndTitleTracksFixedOffset() {
        assertEquals(496.dp, metadataPanelMaxHeight(800.dp, 280.dp))
        assertEquals(0.dp, metadataPanelMaxHeight(800.dp, 800.dp))
        assertEquals(320.dp, titleTopOffset(800.dp, 280.dp, 200.dp))
        assertEquals(0.dp, titleTopOffset(800.dp, 280.dp, 800.dp))
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
