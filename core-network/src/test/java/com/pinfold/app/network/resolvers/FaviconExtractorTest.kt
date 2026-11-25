package com.pinfold.app.network.resolvers

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaviconExtractorTest {
    private val instagramHtml = """
        <html><head>
        <link rel="shortcut icon" href="https://static.cdninstagram.com/rsrc.php/v3/y6/l/0,TCfoVBfNigfMZFvqD3V9C9Nb9e7B.ico" type="image/x-icon">
        <link rel="icon" sizes="192x192" href="https://static.cdninstagram.com/rsrc.php/yr/r/rzWiSjZRxk5.webp" type="image/webp">
        <link rel="apple-touch-icon" href="https://static.cdninstagram.com/rsrc.php/v3/yE/r/vlTY-_fE1aQ.png">
        <link rel="apple-touch-icon" sizes="76x76" href="https://static.cdninstagram.com/rsrc.php/v3/yL/r/0_n5dMP8Yg.png">
        <link rel="apple-touch-icon" sizes="120x120" href="https://static.cdninstagram.com/rsrc.php/v3/y1/r/tza4d4dbKc.png">
        <link rel="apple-touch-icon" sizes="152x152" href="https://static.cdninstagram.com/rsrc.php/v3/yw/r/PK5tS4bYlE.png">
        <link rel="apple-touch-icon" sizes="167x167" href="https://static.cdninstagram.com/rsrc.php/v3/y2/r/fpiSxVm8n8.png">
        <link rel="apple-touch-icon" sizes="180x180" href="https://static.cdninstagram.com/rsrc.php/v3/y3/r/s7OIKr6y8g.png">
        </head><body></body></html>
    """.trimIndent()

    @Test
    fun instagramPageKeeps192WebpFirstAndSkipsBrokenIcoFallback() {
        val icons = FaviconExtractor.collect(Jsoup.parse(instagramHtml), "https://www.instagram.com/p/ABC/")
        assertEquals("https://static.cdninstagram.com/rsrc.php/yr/r/rzWiSjZRxk5.webp", icons.first())
        assertTrue(icons.all { it.startsWith("https://static.cdninstagram.com/") })
        assertFalse(icons.any { it.endsWith("/favicon.ico") })
    }

    @Test
    fun openGraphChainExposesTheSameInstagramIcons() {
        val preview = OpenGraphResolver.parse(instagramHtml, "https://www.instagram.com/p/ABC/", "test")
        assertEquals("https://static.cdninstagram.com/rsrc.php/yr/r/rzWiSjZRxk5.webp", preview.faviconUrls.first())
        assertTrue(preview.faviconUrls.none { it.endsWith("/favicon.ico") })
    }

    @Test
    fun instagramLookalikeIconHostIsRejectedDuringExtraction() {
        val html = """<html><head><link rel="icon" href="https://cdninstagram.com.evil.example/icon.png"></head></html>"""
        val icons = FaviconExtractor.collect(Jsoup.parse(html), "https://www.instagram.com/")
        assertTrue(icons.isEmpty())
    }
}
