package com.pinfold.app.network.resolvers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaviconHostPolicyTest {
    @Test
    fun normalizesWwwAndMobileHosts() {
        assertEquals("instagram.com", FaviconHostPolicy.normalizeHost("https://www.instagram.com/p/ABC"))
        assertEquals("instagram.com", FaviconHostPolicy.normalizeHost("https://m.instagram.com/p/ABC"))
        assertEquals("example.com", FaviconHostPolicy.normalizeHost("http://WWW.Example.com/a"))
        assertEquals("", FaviconHostPolicy.normalizeHost("not a url"))
    }

    @Test
    fun allowsInstagramCdnIconsOnlyForInstagramPages() {
        val page = "https://www.instagram.com/p/ABC"
        assertTrue(
            FaviconHostPolicy.allowed(
                page,
                "https://static.cdninstagram.com/rsrc.php/yr/r/rzWiSjZRxk5.webp",
            ),
        )
        assertTrue(FaviconHostPolicy.allowed(page, "https://scontent-fco1-1.fbcdn.net/v/t51.2885-19/a.jpg"))
        assertFalse(FaviconHostPolicy.allowed("https://example.com/a", "https://static.cdninstagram.com/a.webp"))
    }

    @Test
    fun rejectsLookalikesAndNonHttpSchemes() {
        val page = "https://www.instagram.com/p/ABC"
        assertFalse(FaviconHostPolicy.allowed(page, "https://instagram.com.evil.com/icon.png"))
        assertFalse(FaviconHostPolicy.allowed(page, "https://static.cdninstagram.com.evil.com/icon.png"))
        assertFalse(FaviconHostPolicy.allowed(page, "javascript:alert(1)"))
        assertFalse(FaviconHostPolicy.allowed(page, "file:///etc/passwd"))
        assertFalse(FaviconHostPolicy.allowed("not-a-url", "https://static.cdninstagram.com/icon.webp"))
    }

    @Test
    fun keepsSameSiteAllowanceForNormalSites() {
        assertTrue(FaviconHostPolicy.allowed("https://www.example.com/a", "https://cdn.example.com/icon.png"))
        assertTrue(FaviconHostPolicy.allowed("https://example.com/a", "https://example.com/favicon.ico"))
        assertFalse(FaviconHostPolicy.allowed("https://example.com/a", "https://other.com/icon.png"))
    }

    @Test
    fun recognizesInstagramHostVariants() {
        assertTrue(FaviconHostPolicy.isInstagramPage("instagram.com"))
        assertTrue(FaviconHostPolicy.isInstagramPage("www.instagram.com"))
        assertTrue(FaviconHostPolicy.isInstagramPage("m.instagram.com"))
        assertFalse(FaviconHostPolicy.isInstagramPage("instagram.com.evil.com"))
        assertFalse(FaviconHostPolicy.isInstagramPage("notinstagram.com"))
    }

    @Test
    fun skipsIcoFallbackOnlyForInstagram() {
        assertFalse(FaviconHostPolicy.shouldAddIcoFallback("https://www.instagram.com/p/ABC"))
        assertFalse(FaviconHostPolicy.shouldAddIcoFallback("https://m.instagram.com/"))
        assertTrue(FaviconHostPolicy.shouldAddIcoFallback("https://example.com/a"))
    }
}
