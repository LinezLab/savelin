package com.pinfold.app.network

import com.pinfold.app.network.model.Preview
import com.pinfold.app.network.resolvers.Resolver
import com.pinfold.app.network.resolvers.ResolverContext
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewFetcherTest {
    private class FakeResolver(
        override val name: String,
        private val result: Preview?,
        private val onCall: (String) -> Unit = {},
    ) : Resolver {
        override suspend fun resolve(url: String, context: ResolverContext): Preview? {
            onCall(name)
            return result
        }
    }

    private val primary = Preview(
        url = "https://example.com/article",
        title = "Primary title",
        description = "Primary description",
        imageUrls = listOf("https://cdn.example.com/primary.jpg"),
        source = "shelf-primary",
    )

    @Test
    fun primaryIsAttemptedFirstAndLegacyChainIsSkippedWhenImageExists() = runBlocking {
        val calls = mutableListOf<String>()
        val legacy = FakeResolver("legacy", primary.copy(title = "Legacy title", source = "legacy")) { calls += it }
        val fetcher = PreviewFetcher(
            client = OkHttpClient(),
            primaryResolver = FakeResolver("shelf-primary", primary) { calls += it },
            legacyResolvers = listOf(legacy),
        )

        val result = fetcher.fetch("https://example.com/article", "ua")

        assertEquals(listOf("shelf-primary"), calls)
        assertEquals("Primary title", result.title)
        assertEquals(listOf("https://cdn.example.com/primary.jpg"), result.imageUrls)
    }

    @Test
    fun metadataOnlyPrimaryFallsBackToCurrentChainForImages() = runBlocking {
        val calls = mutableListOf<String>()
        val metadataOnly = primary.copy(imageUrls = emptyList())
        val legacyPreview = primary.copy(
            title = "Legacy title",
            imageUrls = listOf("https://cdn.example.com/legacy.jpg"),
            source = "legacy",
        )
        val fetcher = PreviewFetcher(
            client = OkHttpClient(),
            primaryResolver = FakeResolver("shelf-primary", metadataOnly) { calls += it },
            legacyResolvers = listOf(FakeResolver("legacy", legacyPreview) { calls += it }),
        )

        val result = fetcher.fetch("https://example.com/article", "ua")

        assertEquals(listOf("shelf-primary", "legacy"), calls)
        assertEquals("Primary title", result.title)
        assertEquals(listOf("https://cdn.example.com/legacy.jpg"), result.imageUrls)
    }

    @Test
    fun failedPrimaryStillRunsCurrentChain() = runBlocking {
        val calls = mutableListOf<String>()
        val fetcher = PreviewFetcher(
            client = OkHttpClient(),
            primaryResolver = FakeResolver("shelf-primary", null) { calls += it },
            legacyResolvers = listOf(FakeResolver("legacy", primary.copy(source = "legacy")) { calls += it }),
        )

        val result = fetcher.fetch("https://example.com/article", "ua")

        assertEquals(listOf("shelf-primary", "legacy"), calls)
        assertTrue(result.hasImage)
    }

    @Test
    fun breadcrumbsAreHostOnly() = runBlocking {
        val crumbs = mutableListOf<Triple<String, String, String>>()
        val fetcher = PreviewFetcher(
            client = OkHttpClient(),
            onBreadcrumb = { step, outcome, host -> crumbs += Triple(step, outcome, host) },
            primaryResolver = FakeResolver("shelf-primary", primary),
            legacyResolvers = emptyList(),
        )

        fetcher.fetch("https://example.com/private/path", "ua")

        assertTrue(crumbs.any { it.first == "shelf-primary" && it.second == "ok-image" })
        crumbs.forEach { (step, outcome, host) ->
            assertEquals("example.com", host)
            assertFalse(step.contains("http"))
            assertFalse(outcome.contains("http"))
        }
    }

    @Test
    fun fetchFallbackSkipsPrimaryResolver() = runBlocking {
        val calls = mutableListOf<String>()
        val fetcher = PreviewFetcher(
            client = OkHttpClient(),
            primaryResolver = FakeResolver("shelf-primary", primary) { calls += it },
            legacyResolvers = listOf(FakeResolver("legacy", primary.copy(source = "legacy")) { calls += it }),
        )

        val result = fetcher.fetchFallback("https://example.com/article", "ua")

        assertEquals(listOf("legacy"), calls)
        assertTrue(result.hasImage)
    }

    @Test
    fun instagramUsesLegacyChainFirstAndSkipsShelfStyle() = runBlocking {
        val hosts = listOf(
            "https://instagram.com/reel/ABC/?igsh=xyz",
            "https://www.instagram.com/reel/ABC/?igsh=xyz",
            "https://m.instagram.com/reel/ABC/?igsh=xyz",
        )
        hosts.forEach { url ->
            val calls = mutableListOf<String>()
            var shelfCalls = 0
            val legacyPreview = primary.copy(
                title = "Legacy title",
                imageUrls = listOf("https://cdn.example.com/legacy.jpg"),
                source = "legacy",
            )
            val fetcher = PreviewFetcher(
                client = OkHttpClient(),
                primaryResolver = FakeResolver("shelf-primary", primary) { shelfCalls++ },
                legacyResolvers = listOf(FakeResolver("legacy", legacyPreview) { calls += it }),
            )

            val result = fetcher.fetch(url, "ua")

            assertEquals(0, shelfCalls)
            assertEquals(listOf("legacy"), calls)
            assertEquals("Legacy title", result.title)
            assertEquals(listOf("https://cdn.example.com/legacy.jpg"), result.imageUrls)
        }
    }

    @Test
    fun nonInstagramUsesShelfStyleFirst() = runBlocking {
        val calls = mutableListOf<String>()
        val fetcher = PreviewFetcher(
            client = OkHttpClient(),
            primaryResolver = FakeResolver("shelf-primary", primary.copy(imageUrls = emptyList())) { calls += it },
            legacyResolvers = listOf(FakeResolver("legacy", primary.copy(source = "legacy")) { calls += it }),
        )

        fetcher.fetch("https://example.com/article", "ua")

        assertEquals(listOf("shelf-primary", "legacy"), calls)
    }
}
