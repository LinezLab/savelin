package com.pinfold.app.network.resolvers

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UniversalResolverTest {
    private class Fake(val body: String, val contentType: String, val code: Int = 200)

    private fun client(routes: Map<String, Fake>, requested: MutableList<String> = mutableListOf()): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val url = chain.request().url.toString()
                requested += url
                val fake = routes[url] ?: Fake("", "text/plain", 404)
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(fake.code)
                    .message("OK")
                    .header("Content-Type", fake.contentType)
                    .body(fake.body.toByteArray(Charsets.UTF_8).toResponseBody())
                    .build()
            })
            .build()

    private fun resolve(url: String, routes: Map<String, Fake>, requested: MutableList<String> = mutableListOf()) =
        UniversalResolver().resolve(url, ResolverContext(client(routes, requested), "test-agent"))

    @Test
    fun directImageUsesItselfAsPreviewAndFilenameAsTitle() {
        val url = "https://img.example.com/photos/sunset_trip.jpg"
        val preview = resolve(url, mapOf(url to Fake("binary", "image/jpeg")))!!
        assertEquals("Sunset trip", preview.title)
        assertEquals(listOf(url), preview.imageUrls)
        assertEquals("Direct image", preview.description)
        assertEquals("image/jpeg", preview.contentType)
    }

    @Test
    fun pdfAndVideoUrlsGetFilenameTitleAndFallbackTile() {
        val pdf = "https://files.example.com/docs/annual_report.pdf"
        val pdfPreview = resolve(pdf, mapOf(pdf to Fake("binary", "application/pdf")))!!
        assertEquals("Annual report", pdfPreview.title)
        assertEquals("PDF document", pdfPreview.description)
        assertTrue(pdfPreview.imageUrls.isEmpty())

        val video = "https://media.example.com/clips/vacation_clip.mp4"
        val videoPreview = resolve(video, mapOf(video to Fake("binary", "video/mp4")))!!
        assertEquals("Vacation clip", videoPreview.title)
        assertEquals("Video", videoPreview.description)
        assertTrue(videoPreview.imageUrls.isEmpty())
    }

    @Test
    fun oEmbedDeclaredByVisitedSiteEnrichesTitleAuthorAndThumbnail() {
        val page = "https://media.example.com/watch/42"
        val endpoint = "https://media.example.com/oembed?url=https%3A%2F%2Fmedia.example.com%2Fwatch%2F42"
        val json = """{"title":"A quiet film","author_name":"Ada","thumbnail_url":"https://cdn.media.example.com/thumb.jpg"}"""
        val hosts = mutableListOf<String>()
        val routes = mapOf(
            page to Fake(Fixtures.html("oembed_page.html"), "text/html; charset=UTF-8"),
            endpoint to Fake(json, "application/json; charset=UTF-8"),
        )
        val context = ResolverContext(client(routes), "test-agent", onHost = { host, purpose -> hosts += "$purpose:$host" })
        val preview = UniversalResolver().resolve(page, context)!!

        assertEquals("A quiet film", preview.title)
        assertEquals("Ada", preview.author)
        assertEquals(listOf("https://cdn.media.example.com/thumb.jpg"), preview.imageUrls)
        assertTrue(hosts.contains("oEmbed:media.example.com"))
    }

    @Test
    fun oEmbedEndpointOnAnotherHostIsNeverFetched() {
        val page = "https://media.example.com/watch/7"
        val html = """<html><head><link rel="alternate" type="application/json+oembed" href="https://third.example.org/oembed"></head></html>"""
        val requested = mutableListOf<String>()
        val preview = resolve(page, mapOf(page to Fake(html, "text/html")), requested)!!
        assertFalse(requested.any { it.contains("third.example.org") })
        assertTrue(preview.imageUrls.isEmpty())
    }

    @Test
    fun manifestIconsBecomeFaviconCandidates() {
        val page = "https://site.example.com/article"
        val manifest = "https://site.example.com/app.webmanifest"
        val json = """{"name":"Site","icons":[{"src":"/icons/icon-192.png","sizes":"192x192"},{"src":"/icons/icon-512.png","sizes":"512x512"}]}"""
        val html = """<html><head><link rel="manifest" href="/app.webmanifest"></head></html>"""
        val preview = resolve(page, mapOf(page to Fake(html, "text/html"), manifest to Fake(json, "application/manifest+json")))!!
        assertEquals("https://site.example.com/icons/icon-512.png", preview.faviconUrls.first())
    }
}
