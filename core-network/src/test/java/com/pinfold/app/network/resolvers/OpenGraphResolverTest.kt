package com.pinfold.app.network.resolvers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenGraphResolverTest {
    @Test
    fun parsesInstagramTitleIntoAuthorAndCaption() {
        val html = """
            <html><head>
            <meta property="og:title" content="Ada Lovelace on Instagram: &quot;Hello from the archive!&quot;">
            <meta property="og:description" content="Hello from the archive!">
            <meta property="og:image" content="https://cdn.example.com/photo.jpg?sig=abc">
            <meta property="og:site_name" content="Instagram">
            </head><body></body></html>
        """.trimIndent()
        val preview = OpenGraphResolver.parse(html, "https://www.instagram.com/p/ABC", "test")
        assertEquals("Ada Lovelace", preview.author)
        assertEquals("Hello from the archive!", preview.caption)
        assertEquals("Instagram", preview.siteName)
        assertTrue(preview.imageUrls.first().startsWith("https://cdn.example.com/photo.jpg"))
    }

    @Test
    fun resolvesRelativeImagesAgainstFinalUrl() {
        val html = """<html><head><meta property="og:image" content="/img/cover.png"></head></html>"""
        val preview = OpenGraphResolver.parse(html, "https://example.com/article", "test")
        assertEquals("https://example.com/img/cover.png", preview.imageUrls.first())
    }

    @Test
    fun collectsJsonLdImagesAndIgnoresNonHttp() {
        val html = """
            <html><head><script type="application/ld+json">
            {"@type":"Article","image":"https:\/\/cdn.example.com\/ld.jpg"}
            </script></head></html>
        """.trimIndent()
        val preview = OpenGraphResolver.parse(html, "https://example.com/a", "test")
        assertTrue(preview.imageUrls.contains("https://cdn.example.com/ld.jpg"))
    }
}
