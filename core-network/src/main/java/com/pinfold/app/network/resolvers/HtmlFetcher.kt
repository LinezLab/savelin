package com.pinfold.app.network.resolvers

import okhttp3.Request
import okio.Buffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.math.min

/** A bounded, charset-aware HTTP response. Body is always capped by [ResolverContext.maxBodyBytes]. */
data class FetchedResource(
    val bytes: ByteArray,
    val finalUrl: String,
    val contentType: String,
    val statusCode: Int,
)

object HtmlFetcher {
    fun get(url: String, context: ResolverContext, userAgent: String = context.userAgent): Pair<String, String>? {
        val resource = fetch(url, context, userAgent) ?: return null
        if (resource.statusCode !in 200..299) return null
        return decode(resource) to resource.finalUrl
    }

    fun fetch(url: String, context: ResolverContext, userAgent: String = context.userAgent): FetchedResource? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/json,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en")
            .build()
        context.client.newCall(request).execute().use { response ->
            val body = response.body ?: return null
            val source = body.source()
            val buffer = Buffer()
            while (!source.exhausted() && buffer.size < context.maxBodyBytes) {
                val chunk = source.readByteArray(min(4096L, context.maxBodyBytes - buffer.size))
                if (chunk.isEmpty()) break
                buffer.write(chunk)
            }
            FetchedResource(
                bytes = buffer.readByteArray(),
                finalUrl = response.request.url.toString(),
                contentType = response.header("Content-Type").orEmpty(),
                statusCode = response.code,
            )
        }
    }.getOrNull()

    /**
     * Charset precedence: HTTP Content-Type, then in-document <meta charset>/<meta http-equiv>, then UTF-8.
     * The meta sniff is done on the raw Latin-1 view so non-ASCII bytes cannot disturb the ASCII markup.
     */
    fun decode(resource: FetchedResource): String {
        val declared = charsetFromContentType(resource.contentType)
            ?: charsetFromMarkup(resource.bytes)
            ?: "UTF-8"
        val charset = runCatching { Charset.forName(declared) }.getOrDefault(StandardCharsets.UTF_8)
        return String(resource.bytes, charset)
    }

    private fun charsetFromContentType(contentType: String): String? =
        CHARSET_PARAM.find(contentType)?.groupValues?.get(1)?.trim('"', '\'')

    private fun charsetFromMarkup(bytes: ByteArray): String? {
        val head = String(bytes, 0, minOf(bytes.size, 4096), StandardCharsets.ISO_8859_1)
        return META_CHARSET.find(head)?.groupValues?.get(1)
            ?: META_HTTP_EQUIV.find(head)?.groupValues?.get(1)
    }

    private val CHARSET_PARAM = Regex("""charset\s*=\s*["']?([^;"'\s]+)""", RegexOption.IGNORE_CASE)
    private val META_CHARSET = Regex("""<meta[^>]*?charset\s*=\s*["']?([A-Za-z0-9._:-]+)""", RegexOption.IGNORE_CASE)
    private val META_HTTP_EQUIV = Regex(
        """<meta[^>]*?http-equiv\s*=\s*["']?content-type["']?[^>]*?charset\s*=\s*["']?([A-Za-z0-9._:-]+)""",
        RegexOption.IGNORE_CASE,
    )
}
