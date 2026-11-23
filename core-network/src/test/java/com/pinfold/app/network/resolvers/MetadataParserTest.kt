package com.pinfold.app.network.resolvers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.Charset

class MetadataParserTest {
    private val blogUrl = "https://blog.example.com/2026/11/silent-mornings"

    @Test
    fun extractsEveryMetadataFamilyFromGenericBlog() {
        val preview = MetadataParser.parseHtml(Fixtures.html("generic_blog.html"), blogUrl)

        assertEquals("A Field Guide to Silent Mornings", preview.title)
        assertEquals("Notes on quiet routines and slow coffee.", preview.description)
        assertEquals("The Quiet Journal", preview.siteName)
        assertEquals("Ada Lovelace", preview.author)
        assertEquals("2026-11-20T08:00:00Z", preview.publishedAt)
        assertEquals("en", preview.language)
        assertEquals("en_US", preview.locale)
        assertEquals(blogUrl, preview.canonicalUrl)
        assertEquals("https://blog.example.com/img/morning.jpg", preview.imageUrls.first())
        assertFalse(preview.imageUrls.any { it.contains("tiny.gif") })
    }

    @Test
    fun extractsTagsFromArticleKeywordsAndJsonLd() {
        val preview = MetadataParser.parseHtml(Fixtures.html("generic_blog.html"), blogUrl)
        val expected = listOf("Mindfulness", "slow living", "coffee", "mornings", "routines", "journaling", "quiet")
        expected.forEach { assertTrue("missing tag $it in ${preview.tags}", preview.tags.contains(it)) }
        assertTrue(preview.tags.size <= 8)
    }

    @Test
    fun choosesLargestSameSiteIconsFirstAndRejectsInvalidHrefs() {
        val preview = MetadataParser.parseHtml(
            Fixtures.html("favicons.html"),
            "https://blog.example.com/article",
        )
        assertEquals(
            listOf(
                "https://blog.example.com/apple180.png",
                "https://blog.example.com/fav32.png",
                "https://blog.example.com/fav16.png",
                "https://blog.example.com/favicon.ico",
            ),
            preview.faviconUrls,
        )
    }

    @Test
    fun decodesShiftJisBytesAndParsesJapaneseMetadata() {
        val html = Fixtures.html("japanese.html")
        val bytes = html.toByteArray(Charset.forName("Shift_JIS"))
        val resource = FetchedResource(bytes, "https://news.example.jp/article", "text/html; charset=Shift_JIS", 200)

        val decoded = HtmlFetcher.decode(resource)
        val preview = MetadataParser.parseHtml(decoded, resource.finalUrl)
        assertEquals("静かな朝の記録", preview.title)
        assertEquals("静かな朝のルーティンについて。", preview.description)
        assertEquals("ja", preview.language)
        assertEquals("ja_JP", preview.locale)
        assertTrue(preview.imageUrls.first().endsWith("/img/朝.jpg"))

        assertEquals("静かな朝の記録", MetadataParser.parseBytes(bytes, "Shift_JIS", resource.finalUrl).title)
    }

    @Test
    fun htmlCharsetIsUsedWhenHttpHeaderHasNone() {
        val bytes = Fixtures.html("japanese.html").toByteArray(Charset.forName("Shift_JIS"))
        val resource = FetchedResource(bytes, "https://news.example.jp/article", "text/html", 200)
        assertEquals("静かな朝の記録", MetadataParser.parseHtml(HtmlFetcher.decode(resource), resource.finalUrl).title)
    }
}
