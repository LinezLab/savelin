package com.pinfold.app.network.resolvers

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.Charset

class HtmlFetcherTest {
    @Test
    fun httpCharsetWinsOverMarkup() {
        val html = "<meta charset=Shift_JIS><title>静かな朝</title>"
        val bytes = html.toByteArray(Charset.forName("UTF-8"))
        val resource = FetchedResource(bytes, "https://example.com/", "text/html; charset=UTF-8", 200)
        assertEquals(html, HtmlFetcher.decode(resource))
    }

    @Test
    fun quotedHttpCharsetIsHonoured() {
        val html = "<meta charset=Shift_JIS><title>静かな朝</title>"
        val bytes = html.toByteArray(Charset.forName("UTF-8"))
        val resource = FetchedResource(bytes, "https://example.com/", "text/html; charset=\"UTF-8\"", 200)
        assertEquals(html, HtmlFetcher.decode(resource))
    }

    @Test
    fun httpEquivMetaCharsetIsUsedWhenHeaderHasNone() {
        val html = "<meta http-equiv=content-type content='text/html; charset=Shift_JIS'><title>日本</title>"
        val bytes = html.toByteArray(Charset.forName("Shift_JIS"))
        val resource = FetchedResource(bytes, "https://example.com/", "text/html", 200)
        assertEquals(html, HtmlFetcher.decode(resource))
    }

    @Test
    fun unknownCharsetFallsBackToUtf8() {
        val html = "<title>Hello</title>"
        val resource = FetchedResource(html.toByteArray(Charsets.UTF_8), "https://example.com/", "text/html; charset=not-a-charset", 200)
        assertEquals(html, HtmlFetcher.decode(resource))
    }
}
