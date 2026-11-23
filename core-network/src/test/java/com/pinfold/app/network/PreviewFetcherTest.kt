package com.pinfold.app.network

import com.pinfold.app.network.resolvers.FetchedResource
import com.pinfold.app.network.resolvers.Fixtures
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Proves the universal resolver still produces a complete preview from recorded HTML. */
class PreviewFetcherTest {
    private val page = "https://blog.example.com/2026/11/silent-mornings"

    private fun fetcher(
        html: String,
        issues: MutableList<String> = mutableListOf(),
    ): PreviewFetcher = PreviewFetcher(
        client = OkHttpClient(),
        onIssue = { scope, what, why -> issues += "$scope/$what/$why" },
        fetchOverride = { url, _, _ ->
            if (url == page) {
                FetchedResource(
                    bytes = html.toByteArray(Charsets.UTF_8),
                    finalUrl = url,
                    contentType = "text/html; charset=UTF-8",
                    statusCode = 200,
                )
            } else {
                FetchedResource(ByteArray(0), url, "text/plain", 404)
            }
        },
    )

    @Test
    fun recordedHtmlYieldsMetadataImageAndFavicon() = runBlocking {
        val preview = fetcher(Fixtures.html("generic_blog.html")).fetch(page, "test-agent")

        assertEquals(page, preview.url)
        assertEquals("A Field Guide to Silent Mornings", preview.title)
        assertEquals("Notes on quiet routines and slow coffee.", preview.description)
        assertEquals("The Quiet Journal", preview.siteName)
        assertTrue(preview.imageUrls.contains("https://blog.example.com/img/morning.jpg"))
        assertTrue(preview.faviconUrls.any { it.contains("favicon.ico") })
        assertEquals("universal", preview.source)
    }

    @Test
    fun failedFetchReportsARedactedBreadcrumb() = runBlocking {
        val issues = mutableListOf<String>()
        val offline = PreviewFetcher(
            client = okhttp3.OkHttpClient(),
            onIssue = { scope, what, why -> issues += "$scope/$what/$why" },
            fetchOverride = { _, _, _ -> null },
        )
        offline.fetch(page, "test-agent")

        assertTrue(issues.any { it.startsWith("Preview/parse/") && it.contains("host=blog.example.com") })
    }

    @Test
    fun nonUtf8RecordedHtmlStillParsesTitle() = runBlocking {
        val bytes = Fixtures.html("japanese.html").toByteArray(Charsets.UTF_8)
        val preview = fetcher(String(bytes, Charsets.UTF_8)).fetch(page, "test-agent")
        assertEquals("静かな朝の記録", preview.title)
    }
}
