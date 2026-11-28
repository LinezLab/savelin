package com.pinfold.app.network.resolvers

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedlibUrlTest {
    private val instance = "https://redlib.catsarch.com"

    private fun context() = ResolverContext(client = OkHttpClient(), userAgent = "test")

    @Test
    fun mapsCommentPermalinkOntoInstance() {
        assertEquals(
            "$instance/r/x/comments/abc/title/",
            redlibUrl("https://www.reddit.com/r/x/comments/abc/title/", instance),
        )
        assertEquals(
            "$instance/r/x/comments/abc/title/",
            redlibUrl("https://old.reddit.com/r/x/comments/abc/title/", instance),
        )
        assertEquals(
            "$instance/r/x/comments/abc/title/",
            redlibUrl("https://m.reddit.com/r/x/comments/abc/title/", instance),
        )
    }

    @Test
    fun mapsShortLinkAndPreservesQuery() {
        assertEquals("$instance/comments/abc", redlibUrl("https://redd.it/abc", instance))
        assertEquals(
            "$instance/comments/abc?utm_source=share",
            redlibUrl("https://redd.it/abc?utm_source=share", instance),
        )
    }

    @Test
    fun mapsBareCommentSubredditAndUserPaths() {
        assertEquals(
            "$instance/comments/abc/title/",
            redlibUrl("https://reddit.com/comments/abc/title/", instance),
        )
        assertEquals("$instance/r/x/", redlibUrl("https://www.reddit.com/r/x/", instance))
        assertEquals("$instance/user/ada/", redlibUrl("https://www.reddit.com/user/ada/", instance))
    }

    @Test
    fun leavesNonRedditAndMalformedUrlsUntouched() {
        listOf(
            "https://example.com/r/x/comments/abc/",
            "https://reddit.com.evil.com/r/x/",
            "not a url",
        ).forEach { url -> assertEquals(url, redlibUrl(url, instance)) }
        assertFalse(isRedditUrl("https://example.com/r/x/"))
        assertTrue(isRedditUrl("https://old.reddit.com/r/x/"))
    }

    @Test
    fun instancesAreHttpsAndUnique() {
        assertTrue(REDLIB_INSTANCES.size >= 2)
        assertTrue(REDLIB_INSTANCES.all { it.startsWith("https://") })
        assertEquals(REDLIB_INSTANCES.size, REDLIB_INSTANCES.distinct().size)
    }

    @Test
    fun skipsNonRedditHostsWithoutFetching() = runBlocking {
        var calls = 0
        val resolver = RedlibResolver(fetch = { _, _, _ -> calls++; null })
        assertEquals(null, resolver.resolve("https://example.com/a", context()))
        assertEquals(0, calls)
    }

    @Test
    fun triesInstancesUntilOneHasTitleOrImage() = runBlocking {
        val agents = mutableListOf<String>()
        val targets = mutableListOf<String>()
        val resolver = RedlibResolver(
            instances = listOf("https://one.invalid", "https://two.invalid"),
            fetch = { url, _, agent ->
                agents += agent
                targets += url
                if (url.contains("one.invalid")) {
                    "<html><head></head></html>" to url
                } else {
                    """<html><head><meta property="og:title" content="Post title"><meta property="og:image" content="/thumb.jpg"></head></html>""" to url
                }
            },
        )
        val original = "https://www.reddit.com/r/x/comments/abc/title/"

        val preview = resolver.resolve(original, context())

        assertEquals(2, targets.size)
        assertEquals("https://one.invalid/r/x/comments/abc/title/", targets[0])
        assertEquals("https://two.invalid/r/x/comments/abc/title/", targets[1])
        assertTrue(agents.all { it == DESKTOP_UA })
        assertEquals("Post title", preview?.title)
        assertEquals(listOf("https://two.invalid/thumb.jpg"), preview?.imageUrls)
        assertEquals(original, preview?.url)
        assertEquals("Reddit", preview?.platform)
        assertEquals("redlib", preview?.source)
    }

    @Test
    fun returnsNullWhenEveryInstanceIsEmpty() = runBlocking {
        val resolver = RedlibResolver(
            instances = listOf("https://one.invalid", "https://two.invalid"),
            fetch = { url, _, _ -> "<html></html>" to url },
        )
        assertEquals(null, resolver.resolve("https://redd.it/abc", context()))
    }

    @Test
    fun instagramIsNeverMappedToRedlib() {
        val url = "https://www.instagram.com/reel/ABC123/?igsh=xyz"
        assertFalse(isRedditUrl(url))
        assertFalse(isRedditUrl("https://instagram.com.evil.com/reel/ABC123/"))
        assertEquals(url, redlibUrl(url, instance))
    }
}
