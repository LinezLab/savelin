package com.pinfold.app.network.resolvers

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfStyleResolverTest {
    private fun testContext() = ResolverContext(client = OkHttpClient(), userAgent = "test")

    private val fixture = """
        <html><head>
        <title>Page title</title>
        <meta property="og:title" content="Shelf title">
        <meta property="og:description" content="Shelf description">
        <meta property="og:site_name" content="Example">
        <meta property="og:image" content="https://cdn.example.com/og.jpg">
        <meta name="twitter:image" content="https://cdn.example.com/twitter.jpg">
        </head><body></body></html>
    """.trimIndent()

    @Test
    fun parsesShelfMetadataFields() {
        val preview = ShelfStyleResolver.parse(fixture, "https://example.com/story/a")
        assertEquals("Shelf title", preview.title)
        assertEquals("Shelf description", preview.description)
        assertEquals("Example", preview.siteName)
        assertEquals(listOf("https://cdn.example.com/og.jpg"), preview.imageUrls)
    }

    @Test
    fun ogImageWinsAndFallbacksFollowShelfOrder() {
        val noOg = fixture.replace(Regex("""<meta property="og:image"[^>]*>"""), "")
        assertEquals(
            "https://cdn.example.com/twitter.jpg",
            ShelfStyleResolver.parse(noOg, "https://example.com/a").imageUrls.single(),
        )

        val icon = """<html><head><link rel="apple-touch-icon" href="/icon.png"></head></html>"""
        assertEquals(
            "https://example.com/icon.png",
            ShelfStyleResolver.parse(icon, "https://example.com/a").imageUrls.single(),
        )

        val bigImg = """<html><body><img src="big.jpg" width="640" height="480"><img src="tiny.jpg" width="32" height="32"></body></html>"""
        assertEquals(
            "https://example.com/big.jpg",
            ShelfStyleResolver.parse(bigImg, "https://example.com/a").imageUrls.single(),
        )
    }

    @Test
    fun resolvesProtocolRelativeImages() {
        val html = """<html><head><meta property="og:image" content="//cdn.example.com/p.png"></head></html>"""
        assertEquals(
            "https://cdn.example.com/p.png",
            ShelfStyleResolver.parse(html, "https://example.com/a").imageUrls.single(),
        )
    }

    @Test
    fun triesWhatsAppThenDesktopChromeAndStopsAtFirstUsableResult() = runBlocking {
        val agents = mutableListOf<String>()
        val fetcher = ShelfDocumentFetcher { url, agent ->
            agents += agent
            if (agent.contains("WhatsApp")) {
                ShelfFetchOutcome.Success("<html><head></head></html>", url)
            } else {
                ShelfFetchOutcome.Success(fixture, url)
            }
        }
        val preview = ShelfStyleResolver(fetcher).resolve("https://example.com/a", testContext())
        assertEquals(2, agents.size)
        assertTrue(agents[0].contains("WhatsApp"))
        assertTrue(agents[1].contains("Chrome/96"))
        assertEquals("Shelf title", preview?.title)
    }

    @Test
    fun stopsAfterFirstAgentWhenItProducesMetadata() = runBlocking {
        var calls = 0
        val fetcher = ShelfDocumentFetcher { url, _ ->
            calls++
            ShelfFetchOutcome.Success(fixture, url)
        }
        val preview = ShelfStyleResolver(fetcher).resolve("https://example.com/a", testContext())
        assertEquals(1, calls)
        assertEquals("Shelf title", preview?.title)
    }

    @Test
    fun reportsFailureReasonWithoutUrl() = runBlocking {
        val outcomes = mutableListOf<String>()
        val fetcher = ShelfDocumentFetcher { _, _ -> ShelfFetchOutcome.Failure("not-html") }
        val preview = ShelfStyleResolver(fetcher, onAttempt = { outcomes += it })
            .resolve("https://example.com/a", testContext())
        assertEquals(null, preview)
        assertEquals(listOf("ua1-not-html", "ua2-not-html"), outcomes)
    }
}
