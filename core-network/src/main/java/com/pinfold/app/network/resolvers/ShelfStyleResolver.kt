package com.pinfold.app.network.resolvers

import com.pinfold.app.network.model.Preview
import okio.Buffer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.math.min

sealed interface ShelfFetchOutcome {
    data class Success(val html: String, val finalUrl: String) : ShelfFetchOutcome
    data class Failure(val reason: String) : ShelfFetchOutcome
}

fun interface ShelfDocumentFetcher {
    fun fetch(url: String, agent: String): ShelfFetchOutcome
}

/** Independent implementation of Shelf's primary link-preview method. See docs/RECON-SHELF.md. */
class ShelfStyleResolver(
    private val documentFetcher: ShelfDocumentFetcher = OkHttpShelfDocumentFetcher(),
    private val onAttempt: (outcome: String) -> Unit = {},
) : Resolver {
    override val name = "shelf-primary"

    override suspend fun resolve(url: String, context: ResolverContext): Preview? {
        var last: Preview? = null
        USER_AGENTS.forEachIndexed { index, agent ->
            val outcome = runCatching { documentFetcher.fetch(url, agent) }
                .getOrElse { ShelfFetchOutcome.Failure("exception") }
            when (outcome) {
                is ShelfFetchOutcome.Failure -> onAttempt("ua${index + 1}-${outcome.reason}")
                is ShelfFetchOutcome.Success -> {
                    val parsed = parse(outcome.html, outcome.finalUrl, name)
                    val usable = (parsed.title.isNotBlank() && parsed.title != "null") ||
                        (parsed.description.isNotBlank() && parsed.description != "null") ||
                        parsed.imageUrls.isNotEmpty()
                    if (usable) {
                        onAttempt("ua${index + 1}-ok")
                        return parsed
                    }
                    onAttempt("ua${index + 1}-empty-metadata")
                    last = parsed
                }
            }
        }
        return last
    }

    companion object {
        private val USER_AGENTS = arrayOf(
            "WhatsApp/2.24.8.76 A",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.45 Safari/537.36",
        )

        fun parse(html: String, finalUrl: String, source: String = "shelf-primary"): Preview {
            val doc = Jsoup.parse(html, finalUrl)

            fun meta(selector: String): String = doc.select(selector).attr("content").trim()

            val title = meta("meta[property=og:title]").ifBlank { doc.title().orEmpty() }.trim()
            val description = meta("meta[property=og:description], meta[name=description]")
            val siteName = meta("meta[property=og:site_name]")
            val imageUrl = selectImage(doc, finalUrl)

            return Preview(
                url = finalUrl,
                title = title,
                description = description,
                siteName = siteName,
                imageUrls = if (imageUrl.isBlank()) emptyList() else listOf(imageUrl),
                html = html.take(16_000),
                source = source,
            )
        }

        internal fun selectImage(doc: Document, baseUrl: String): String {
            val candidates = listOf(
                doc.select("meta[property=og:image]").attr("content"),
                doc.select("meta[name=twitter:image]").attr("content"),
                doc.select("meta[itemprop=image]").attr("content"),
                doc.select("meta[name=image]").attr("content"),
                doc.select("link[rel=image_src]").attr("href"),
                doc.select("link[rel=apple-touch-icon]").attr("href"),
                doc.select("link[rel=icon]").attr("href"),
                doc.select("img[src][width][height]").firstOrNull { img ->
                    (img.attr("width").toIntOrNull() ?: 0) >= 200 &&
                        (img.attr("height").toIntOrNull() ?: 0) >= 200
                }?.attr("src").orEmpty(),
            )
            return candidates.firstOrNull { it.isNotBlank() }
                ?.let { makeAbsoluteUrl(it.trim(), baseUrl) }
                .orEmpty()
        }

        internal fun makeAbsoluteUrl(url: String, baseUrl: String): String = runCatching {
            when {
                url.startsWith("http", ignoreCase = true) -> url
                url.startsWith("//") -> "${URI.create(baseUrl).scheme}:$url"
                url.startsWith("/") -> {
                    val base = URI.create(baseUrl)
                    "${base.scheme}://${base.authority}$url"
                }
                else -> {
                    val base = URI.create(baseUrl)
                    val basePath = base.path?.substringBeforeLast('/', "").orEmpty()
                    "${base.scheme}://${base.authority}$basePath/$url"
                }
            }
        }.getOrDefault(url)
    }
}

class OkHttpShelfDocumentFetcher : ShelfDocumentFetcher {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .addInterceptor(redirectCap())
        .build()

    override fun fetch(url: String, agent: String): ShelfFetchOutcome = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", agent)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use ShelfFetchOutcome.Failure("http-error")
            val contentType = response.header("Content-Type").orEmpty()
            if (!contentType.contains("text/html", ignoreCase = true)) return@use ShelfFetchOutcome.Failure("not-html")
            val body = response.body ?: return@use ShelfFetchOutcome.Failure("empty-body")
            val buffer = Buffer()
            val source = body.source()
            while (!source.exhausted() && buffer.size < MAX_BODY_BYTES) {
                if (source.read(buffer, min(4096L, MAX_BODY_BYTES - buffer.size)) == -1L) break
            }
            if (buffer.size == 0L) ShelfFetchOutcome.Failure("empty-body")
            else ShelfFetchOutcome.Success(buffer.readUtf8(), response.request.url.toString())
        }
    }.getOrElse { ShelfFetchOutcome.Failure(it.javaClass.simpleName) }

    private fun redirectCap(): Interceptor = Interceptor { chain ->
        val response: Response = chain.proceed(chain.request())
        var redirects = 0
        var prior = response.priorResponse
        while (prior != null) {
            redirects++
            prior = prior.priorResponse
        }
        if (redirects > MAX_REDIRECTS) {
            response.close()
            throw IOException("Too many redirects")
        }
        response
    }

    companion object {
        const val MAX_BODY_BYTES = 512L * 1024
        const val MAX_REDIRECTS = 5
    }
}
