package com.pinfold.app.network

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkExtractorTest {
    @Test
    fun findsLinkInsideMessage() {
        val text = "Check this out: https://www.instagram.com/reel/ABC/?igsh=xyz really nice"
        assertEquals(listOf("https://www.instagram.com/reel/ABC/?igsh=xyz"), LinkExtractor.extract(text))
    }

    @Test
    fun addsSchemeToBareDomain() {
        assertEquals(listOf("https://www.example.com/page"), LinkExtractor.extract("www.example.com/page"))
    }

    @Test
    fun findsSeveralLinksAndTrimsPunctuation() {
        val text = "https://a.com/x, and https://b.com/y)."
        assertEquals(listOf("https://a.com/x", "https://b.com/y"), LinkExtractor.extract(text))
    }

    @Test
    fun ignoresNonWebSchemes() {
        assertEquals(emptyList<String>(), LinkExtractor.extract("intent://foo#Intent;scheme=x;end"))
    }
}
