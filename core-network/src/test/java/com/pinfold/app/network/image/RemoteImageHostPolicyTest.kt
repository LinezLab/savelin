package com.pinfold.app.network.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteImageHostPolicyTest {
    private val page = "https://www.instagram.com/p/ABC123/"
    private val cdn = "https://scontent-fra1-1.cdninstagram.com/v/t51.2885-15/photo.jpg?stp=dst-jpg&_nc_cat=1&ccb=1-7&oh=deadbeef&oe=64a1b2c3"
    private val fbcdn = "https://scontent.fbcdn.net/v/t51/photo.jpg?stp=src"

    @Test
    fun instagramPagesAreDetectedWithoutLookalikes() {
        assertTrue(RemoteImageHostPolicy.isInstagramPage(page))
        assertTrue(RemoteImageHostPolicy.isInstagramPage("https://m.instagram.com/reel/ABC/"))
        assertFalse(RemoteImageHostPolicy.isInstagramPage("https://instagram.com.evil.com/p/ABC/"))
        assertFalse(RemoteImageHostPolicy.isInstagramPage("https://example.com/p/ABC/"))
    }

    @Test
    fun allowsSameSiteAndInstagramCdnForInstagramPages() {
        assertTrue(RemoteImageHostPolicy.allows(page, cdn))
        assertTrue(RemoteImageHostPolicy.allows(page, fbcdn))
        assertTrue(RemoteImageHostPolicy.allows(page, "https://www.instagram.com/static/photo.jpg"))
    }

    @Test
    fun rejectsLookalikesAndNonHttpSchemes() {
        assertFalse(RemoteImageHostPolicy.allows(page, "https://cdninstagram.com.evil.com/photo.jpg"))
        assertFalse(RemoteImageHostPolicy.allows(page, "https://evil-cdninstagram.com/photo.jpg"))
        assertFalse(RemoteImageHostPolicy.allows(page, "https://fbcdn.net.evil.com/photo.jpg"))
        assertFalse(RemoteImageHostPolicy.allows(page, "javascript:alert(1)"))
        assertFalse(RemoteImageHostPolicy.allows(page, "/relative/photo.jpg"))
        assertFalse(RemoteImageHostPolicy.allows("https://example.com/a", cdn))
        assertFalse(RemoteImageHostPolicy.allows("https://instagram.com.evil.com/a", cdn))
    }

    @Test
    fun headersAreOnlyAddedForInstagramCdnImages() {
        val headers = RemoteImageHostPolicy.headers(page, cdn)
        assertTrue(headers["User-Agent"]!!.contains("Mozilla/5.0"))
        assertEquals("https://www.instagram.com/", headers["Referer"])
        assertTrue(RemoteImageHostPolicy.headers(page, fbcdn).isNotEmpty())
        assertTrue(RemoteImageHostPolicy.headers(page, "https://www.instagram.com/a.jpg").isEmpty())
        assertTrue(RemoteImageHostPolicy.headers("https://example.com/a", cdn).isEmpty())
        assertTrue(RemoteImageHostPolicy.headers(page, "https://cdninstagram.com.evil.com/a.jpg").isEmpty())
    }
}
