package com.pinfold.app.network

import com.pinfold.app.network.model.Preview
import com.pinfold.app.network.resolvers.FetchedResource
import com.pinfold.app.network.resolvers.HtmlFetcher
import com.pinfold.app.network.resolvers.PlatformFastPaths
import com.pinfold.app.network.resolvers.ResolverContext
import com.pinfold.app.network.resolvers.UniversalResolver
import okhttp3.OkHttpClient
import java.net.URI

class PreviewFetcher(
    private val client: OkHttpClient,
    private val onHost: (host: String, purpose: String) -> Unit = { _, _ -> },
    private val onIssue: (scope: String, what: String, why: String) -> Unit = { _, _, _ -> },
    /** Offline/tests can feed recorded bytes; production leaves it null and uses [HtmlFetcher]. */
    private val fetchOverride: ((url: String, context: ResolverContext, userAgent: String) -> FetchedResource?)? = null,
) {
    suspend fun fetch(rawUrl: String, userAgent: String, proxyTemplate: String = ""): Preview {
        val expanded = expandShortener(rawUrl.trim(), userAgent)
        val url = UrlNormalizer.normalize(expanded)
        val context = context(userAgent)
        val host = hostOf(url)
        onHost(host, "preview")

        val results = mutableListOf<Preview>()
        PlatformFastPaths().resolve(url, context)?.let(results::add)
        UniversalResolver().resolve(url, context)?.let(results::add)

        if (results.none { it.hasImage } && proxyTemplate.isNotBlank() && proxyTemplate.contains("{url}")) {
            val proxyUrl = proxyTemplate.replace("{url}", java.net.URLEncoder.encode(url, "UTF-8"))
            onHost(runCatching { URI.create(proxyUrl).host.orEmpty() }.getOrDefault(""), "opt-in proxy")
            UniversalResolver().resolve(proxyUrl, context)?.let(results::add)
        }

        if (results.isEmpty()) {
            onIssue("Preview", "parse", "no metadata extracted host=$host")
        }
        val merged = merge(results)
        return merged.copy(url = url, platform = merged.platform.ifBlank { PlatformDetector.platform(url) })
    }

    private fun context(userAgent: String): ResolverContext = ResolverContext(
        client = client,
        userAgent = userAgent,
        onHost = onHost,
        onIssue = onIssue,
        fetch = fetchOverride ?: { url, ctx, ua -> HtmlFetcher.fetch(url, ctx, ua) },
    )

    private fun expandShortener(rawUrl: String, userAgent: String): String {
        if (!UrlNormalizer.isShortener(rawUrl)) return rawUrl
        return runCatching {
            HtmlFetcher.get(rawUrl, context(userAgent), userAgent)?.second ?: rawUrl
        }.getOrDefault(rawUrl)
    }

    private fun merge(results: List<Preview>): Preview {
        if (results.isEmpty()) return Preview(url = "")
        val primary = results.firstOrNull { it.hasImage } ?: results.first()
        return primary.copy(
            title = results.firstNonBlank { it.title },
            description = results.firstNonBlank { it.description },
            siteName = results.firstNonBlank { it.siteName },
            platform = results.firstNonBlank { it.platform },
            author = results.firstNonBlank { it.author },
            caption = results.firstNonBlank { it.caption },
            imageUrls = results.flatMap { it.imageUrls }.distinct(),
            html = results.firstNonBlank { it.html },
            contentType = results.firstNonBlank { it.contentType },
            canonicalUrl = results.firstNonBlank { it.canonicalUrl },
            faviconUrls = results.flatMap { it.faviconUrls }.distinct(),
            language = results.firstNonBlank { it.language },
            locale = results.firstNonBlank { it.locale },
            publishedAt = results.firstNonBlank { it.publishedAt },
            tags = MetadataCleaner.cleanTags(results.flatMap { it.tags }),
            source = primary.source,
        )
    }

    private fun List<Preview>.firstNonBlank(selector: (Preview) -> String): String =
        firstOrNull { selector(it).isNotBlank() }?.let(selector).orEmpty()

    private fun hostOf(url: String): String = runCatching { URI.create(url).host.orEmpty() }.getOrDefault("")
}
