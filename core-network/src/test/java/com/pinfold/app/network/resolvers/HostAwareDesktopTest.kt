package com.pinfold.app.network.resolvers

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostAwareDesktopTest {
    private fun context() = ResolverContext(client = OkHttpClient(), userAgent = "test")

    private val playStoreHtml = """
        <html><head>
        <meta property="og:title" content="Example App - Apps on Google Play">
        <meta property="og:description" content="An example app for Android.">
        <meta property="og:image" content="https://play-lh.googleusercontent.com/icon123">
        <meta property="og:site_name" content="Google Play">
        </head><body></body></html>
    """.trimIndent()

    private val apkMirrorHtml = """
        <html><head>
        <meta property="og:title" content="Example App 1.2.3 APK Download by Example Inc. - APKMirror">
        <meta property="og:description" content="Download Example App 1.2.3 APK for Android.">
        <meta property="og:image" content="https://www.apkmirror.com/wp-content/themes/APKMirror/icon.png">
        <meta property="og:site_name" content="APKMirror">
        </head><body></body></html>
    """.trimIndent()

    @Test
    fun detectsOnlyPlayStoreAndApkMirrorHosts() {
        assertTrue(isDesktopSiteHost("https://play.google.com/store/apps/details?id=com.example.app"))
        assertTrue(isDesktopSiteHost("https://www.apkmirror.com/apk/example/app/app-1.0-release/"))
        assertTrue(isDesktopSiteHost("http://apkmirror.com/apk/x"))
        assertFalse(isDesktopSiteHost("https://www.reddit.com/r/x/"))
        assertFalse(isDesktopSiteHost("https://play.google.com.evil.com/x"))
        assertFalse(isDesktopSiteHost("not-a-url"))
    }

    @Test
    fun parsesRecordedPlayStoreHtmlWithTitleAndImage() {
        val preview = OpenGraphResolver.parse(
            playStoreHtml,
            "https://play.google.com/store/apps/details?id=com.example.app",
            "test",
        )
        assertEquals("Example App - Apps on Google Play", preview.title)
        assertEquals("Google Play", preview.siteName)
        assertEquals(listOf("https://play-lh.googleusercontent.com/icon123"), preview.imageUrls)
    }

    @Test
    fun parsesRecordedApkMirrorHtmlWithTitleAndImage() {
        val preview = OpenGraphResolver.parse(
            apkMirrorHtml,
            "https://www.apkmirror.com/apk/example/app/app-1.2.3-release/",
            "test",
        )
        assertEquals("Example App 1.2.3 APK Download by Example Inc. - APKMirror", preview.title)
        assertEquals(listOf("https://www.apkmirror.com/wp-content/themes/APKMirror/icon.png"), preview.imageUrls)
    }

    @Test
    fun usesDesktopUserAgentOnlyForAllowedHosts() = runBlocking {
        val agents = mutableListOf<String>()
        var calls = 0
        val resolver = HostAwareDesktopResolver(
            fetch = { url, _, agent ->
                calls++
                agents += agent
                playStoreHtml to url
            },
        )

        assertNull(resolver.resolve("https://example.com/a", context()))
        assertEquals(0, calls)

        val preview = resolver.resolve("https://play.google.com/store/apps/details?id=com.example.app", context())
        assertEquals(1, calls)
        assertEquals(listOf(DESKTOP_UA), agents)
        assertEquals("Example App - Apps on Google Play", preview?.title)
        assertTrue(preview?.imageUrls?.isNotEmpty() == true)
        assertEquals("desktop-site", preview?.source)
    }
}
