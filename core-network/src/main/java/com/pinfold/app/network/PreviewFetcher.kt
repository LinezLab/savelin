package com.pinfold.app.network

import com.pinfold.app.network.model.Preview
import com.pinfold.app.network.resolvers.HtmlFetcher
import com.pinfold.app.network.resolvers.OpenGraphResolver
import com.pinfold.app.network.resolvers.PlatformFastPaths
import com.pinfold.app.network.resolvers.ResolverContext
import okhttp3.OkHttpClient
import java.net.URI

class PreviewFetcher(
    private val client: OkHttpClient,
    private val onHost: (host: String, purpose: String) -> Unit = { _, _ -> },
) {
    suspend fun fetch(rawUrl: String, userAgent: String): Preview {
        val expanded = expandShortener(rawUrl.trim(), userAgent)
        val url = UrlNormalizer.normalize(expanded)
        val context = ResolverContext(client = client, userAgent = userAgent)
        onHost(hostOf(url), "preview")

        val results = mutableListOf<Preview>()
        PlatformFastPaths().resolve(url, context)?.let(results::add)
        OpenGraphResolver(userAgentOverride = MOBILE_UA).resolve(url, context)?.let(results::add)
        OpenGraphResolver(userAgentOverride = context.crawlerUserAgent).resolve(url, context)?.let(results::add)

        val merged = merge(results)
        return merged.copy(url = url, platform = merged.platform.ifBlank { PlatformDetector.platform(url) })
    }

    private fun expandShortener(rawUrl: String, userAgent: String): String {
        if (!UrlNormalizer.isShortener(rawUrl)) return rawUrl
        return runCatching {
            val context = ResolverContext(client = client, userAgent = userAgent)
            HtmlFetcher.get(rawUrl, context, userAgent)?.second ?: rawUrl
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
            source = primary.source,
        )
    }

    private fun List<Preview>.firstNonBlank(selector: (Preview) -> String): String =
        firstOrNull { selector(it).isNotBlank() }?.let(selector).orEmpty()

    private fun hostOf(url: String): String = runCatching { URI.create(url).host.orEmpty() }.getOrDefault("")

    private companion object {
        const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
