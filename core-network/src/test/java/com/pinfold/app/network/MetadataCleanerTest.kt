package com.pinfold.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataCleanerTest {
    @Test
    fun parsesInstagramDescription() {
        val raw = "283K likes, 3,275 comments - parentingnotess on November 20, 2026: \"A lovely reel #motherhood #momlife\""
        val c = MetadataCleaner.clean("parentingnotess on Instagram: \"A lovely reel\"", raw, "", "Instagram", "Instagram")
        assertEquals("283K", c.likesText)
        assertEquals("3,275", c.commentsText)
        assertEquals("parentingnotess", c.authorHandle)
        assertEquals("November 20, 2026", c.postedAt)
        assertEquals("A lovely reel #motherhood #momlife", c.caption)
        assertEquals(listOf("#motherhood", "#momlife"), c.hashtags)
    }

    @Test
    fun doesNotGuessWhenPatternMisses() {
        val raw = "Some other site description"
        val c = MetadataCleaner.clean("A real title", raw, "someone", "Instagram", "Instagram")
        assertEquals("A real title", c.displayTitle)
        assertEquals(raw, c.caption)
        assertTrue(c.likesText.isEmpty())
        assertTrue(c.hashtags.isEmpty())
    }

    @Test
    fun neverUsesBareSiteNameAsTitle() {
        val c = MetadataCleaner.clean("Instagram", "", "", "Instagram", "Instagram", "instagram.com/reel/x")
        assertEquals("instagram.com/reel/x", c.displayTitle)
    }

    @Test
    fun fallsBackToAuthorHandle() {
        val c = MetadataCleaner.clean("Instagram", "", "@travelblog", "Instagram", "Instagram")
        assertEquals("@travelblog", c.displayTitle)
    }
}
