package com.pinfold.app.network.resolvers

import com.pinfold.app.network.resolvers.ContentClassifier.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentClassifierTest {
    @Test
    fun classifiesByMimeTypeFirst() {
        assertEquals(Kind.HTML, ContentClassifier.kind("https://example.com/", "text/html; charset=UTF-8"))
        assertEquals(Kind.IMAGE, ContentClassifier.kind("https://example.com/a", "image/webp"))
        assertEquals(Kind.MEDIA, ContentClassifier.kind("https://example.com/a", "video/mp4"))
        assertEquals(Kind.DOCUMENT, ContentClassifier.kind("https://example.com/a", "application/pdf"))
    }

    @Test
    fun fallsBackToExtensionForOctetStream() {
        assertEquals(Kind.IMAGE, ContentClassifier.kind("https://example.com/photos/sunset.jpg", "application/octet-stream"))
        assertEquals(Kind.DOCUMENT, ContentClassifier.kind("https://example.com/files/report.pdf", ""))
        assertEquals(Kind.MEDIA, ContentClassifier.kind("https://example.com/media/clip.mp4", ""))
        assertEquals(Kind.UNKNOWN, ContentClassifier.kind("https://example.com/no-extension", ""))
    }

    @Test
    fun summariesDescribeDirectAndFallbackContent() {
        assertEquals("Direct image", ContentClassifier.summary(Kind.IMAGE))
        assertEquals("PDF document", ContentClassifier.summary(Kind.DOCUMENT, "https://example.com/a.pdf"))
        assertEquals("Video", ContentClassifier.summary(Kind.MEDIA, "https://example.com/a.mp4"))
        assertEquals("Audio", ContentClassifier.summary(Kind.MEDIA, "https://example.com/a.mp3"))
    }
}
