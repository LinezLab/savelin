package com.pinfold.app.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataCleanerTest {
    @Test
    fun parsesFullVariantWithStats() {
        val raw = "283K likes, 3,275 comments - parentingnotess on November 20, 2026: \"Hello from the archive!\""
        val c = MetadataCleaner.clean("parentingnotess on Instagram: \"Hello\"", raw, "", "Instagram", "Instagram")
        assertEquals("283K", c.likesText)
        assertEquals("3,275", c.commentsText)
        assertEquals("parentingnotess", c.authorHandle)
        assertEquals("November 20, 2026", c.postedAt)
        assertEquals("Hello from the archive!", c.caption)
    }

    @Test
    fun parsesShortVariantWithoutStatsAndUsesCaptionFirstLineAsTitle() {
        val raw = "sam.moto_ on 19 Nov 2026: “Aise Jise Kabhi Khatam Na Hone Do ✨ #yamaha #r15v4”."
        val c = MetadataCleaner.clean(raw, raw, "", "Instagram", "Instagram")
        assertEquals("sam.moto_", c.authorHandle)
        assertEquals("19 Nov 2026", c.postedAt)
        assertEquals("Aise Jise Kabhi Khatam Na Hone Do ✨", c.displayTitle)
        assertTrue(c.caption.startsWith("Aise Jise Kabhi Khatam Na Hone Do"))
        assertTrue(c.hashtags.contains("#yamaha"))
    }

    @Test
    fun handlesQuotesInsideCaption() {
        val raw = "someone on March 2, 2026: \"He said \"hello\" and left.\"."
        val c = MetadataCleaner.clean(raw, raw, "", "Instagram", "Instagram")
        assertEquals("He said \"hello\" and left.", c.caption)
    }

    @Test
    fun doesNotGuessWhenPatternMisses() {
        val raw = "Follow for more"
        val c = MetadataCleaner.clean("Follow for more", raw, "someone", "Instagram", "Instagram")
        assertEquals("Follow for more", c.caption)
        assertEquals("Follow for more", c.displayTitle)
        assertTrue(c.likesText.isEmpty())
    }
}
