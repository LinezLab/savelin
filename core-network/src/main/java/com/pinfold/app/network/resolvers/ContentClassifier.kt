package com.pinfold.app.network.resolvers

import java.net.URI
import java.util.Locale

/**
 * Chooses how a fetched URL is interpreted. MIME type wins over the URL extension; the extension is
 * only a fallback for servers that answer with application/octet-stream or no Content-Type at all.
 * Classification is purely technical: nothing here inspects subject matter or blocks a site.
 */
object ContentClassifier {
    enum class Kind { HTML, IMAGE, MEDIA, DOCUMENT, UNKNOWN }

    fun kind(url: String, contentType: String = ""): Kind {
        val mime = contentType.substringBefore(';').trim().lowercase(Locale.ROOT)
        if (mime.isNotBlank()) {
            when {
                mime == "text/html" || mime == "application/xhtml+xml" -> return Kind.HTML
                mime.startsWith("image/") -> return Kind.IMAGE
                mime.startsWith("video/") || mime.startsWith("audio/") -> return Kind.MEDIA
                mime == "application/pdf" || mime == "application/epub+zip" ||
                    mime == "application/rtf" || mime == "text/rtf" ||
                    mime.startsWith("application/vnd.openxmlformats") ||
                    mime == "application/msword" || mime == "application/vnd.ms-excel" ||
                    mime == "application/vnd.ms-powerpoint" ||
                    mime == "text/plain" || mime == "text/csv" -> return Kind.DOCUMENT
                mime == "application/octet-stream" || mime == "binary/octet-stream" -> return fromExtension(url)
                mime.contains("json") || mime.contains("xml") || mime.startsWith("text/") -> return Kind.UNKNOWN
            }
        }
        return fromExtension(url)
    }

    fun summary(kind: Kind, url: String = ""): String = when (kind) {
        Kind.IMAGE -> "Direct image"
        Kind.MEDIA -> if (isVideo(url)) "Video" else "Audio"
        Kind.DOCUMENT -> if (extension(url) == "pdf") "PDF document" else "Document"
        Kind.HTML, Kind.UNKNOWN -> ""
    }

    private fun fromExtension(url: String): Kind = when (extension(url)) {
        "html", "htm", "xhtml", "shtml", "php", "asp", "aspx" -> Kind.HTML
        "jpg", "jpeg", "png", "gif", "webp", "avif", "heic", "heif", "svg", "bmp" -> Kind.IMAGE
        "mp4", "m4v", "webm", "mkv", "mov", "avi", "m4a", "mp3", "aac", "ogg", "wav", "flac" -> Kind.MEDIA
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "rtf", "txt", "epub", "csv" -> Kind.DOCUMENT
        else -> Kind.UNKNOWN
    }

    private fun isVideo(url: String): Boolean = extension(url) in setOf(
        "mp4", "m4v", "webm", "mkv", "mov", "avi",
    )

    private fun extension(url: String): String {
        val path = runCatching { URI.create(url).path.orEmpty() }.getOrDefault("")
        val value = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        return if (value.length <= 8) value else ""
    }
}
