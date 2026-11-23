package com.pinfold.app.network.resolvers

import org.junit.Assert.assertEquals
import org.junit.Test

class OEmbedParserTest {
    private val pageUrl = "https://media.example.com/watch/42"

    @Test
    fun discoversJsonEndpointDeclaredByTheVisitedSite() {
        val expected = "https://media.example.com/oembed?url=https%3A%2F%2Fmedia.example.com%2Fwatch%2F42"
        assertEquals(expected, OEmbedParser.findJsonEndpoint(Fixtures.html("oembed_page.html"), pageUrl))
    }

    @Test
    fun parsesRecordedOEmbedJson() {
        val json = """
            {"type":"video","title":"A quiet film","author_name":"Ada","provider_name":"Media Example","thumbnail_url":"https://cdn.media.example.com/thumb.jpg"}
        """.trimIndent()
        val data = OEmbedParser.parse(json)
        assertEquals("A quiet film", data.title)
        assertEquals("Ada", data.authorName)
        assertEquals("Media Example", data.providerName)
        assertEquals("https://cdn.media.example.com/thumb.jpg", data.thumbnailUrl)
    }

    @Test
    fun ignoresMalformedJsonAndMissingLinks() {
        assertEquals("", OEmbedParser.parse("not json"))
        assertEquals("", OEmbedParser.findJsonEndpoint("<html></html>", pageUrl))
    }
}
