package com.pinfold.app.network

import com.pinfold.app.network.model.Preview
import com.pinfold.app.network.resolvers.HtmlFetcher
import com.pinfold.app.network.resolvers.OpenGraphResolver
import com.pinfold.app.network.resolvers.PlatformFastPaths
import com.pinfold.app.network.resolvers.Resolver
import com.pinfold.app.network.resolvers.ResolverContext
import com.pinfold.app.network.resolvers.ShelfStyleResolver
import okhttp3.OkHttpClient
import java.net.URI

/**
 * Resolves a URL into metadata and image candidates.
 *
 * The Shelf-style direct fetch is the FIRST step ([ShelfStyleResolver]) for every host except
 * Instagram, which uses the original resolver chain first and skips Shelf entirely. When the
 * primary step produces a usable image the remaining resolvers are skipped. Otherwise the current
 * chain (platform fast paths, mobile/crawler Open Graph retry and the optional user proxy) runs and
 * its images are appended.
 *
 * All callbacks carry a step name, a fixed outcome and a host only — never a URL or a title.
 */
class PreviewFetcher(
    private val client: OkHttpClient,
    private val onHost: (host: String, purpose: String) -> Unit = { _, _ -> },
    private val onBreadcrumb: (step: String, outcome: String, host: String) -> Unit = { _, _, _ -> },
    private val primaryResolver: Resolver? = null,
    private val legacyResolvers: List<Resolver> = defaultLegacyResolvers(),
) {
    suspend fun fetch(rawUrl: String, userAgent: String, proxyTemplate: String = ""): Preview {
        val (url, context, host) = prepare(rawUrl, userAgent)
        val results = mutableListOf<Preview>()

        if (isInstagramHost(host)) {
            runLegacy(url, context, host, proxyTemplate, results)
        } else {
            val primary = resolveSafely(
                primaryResolver ?: ShelfStyleResolver(
                    onAttempt = { outcome -> onBreadcrumb("shelf-attempt", outcome, host) },
                ),
                url,
                context,
                "shelf-primary",
                host,
            )
            if (primary != null) results += primary

            if (primary == null || primary.imageUrls.isEmpty()) {
                runLegacy(url, context, host, proxyTemplate, results)
            }
        }
        return finalize(results, url)
    }

    /**
     * Re-runs only the current resolver chain. Used when the primary step supplied an image URL that
     * could not actually be downloaded or decoded, so the image can be retried from the old system.
     */
    suspend fun fetchFallback(rawUrl: String, userAgent: String, proxyTemplate: String = ""): Preview {
        val (url, context, host) = prepare(rawUrl, userAgent, "preview-fallback")
        val results = mutableListOf<Preview>()
        runLegacy(url, context, host, proxyTemplate, results)
        return finalize(results, url)
    }

    private suspend fun prepare(
        rawUrl: String,
        userAgent: String,
        purpose: String = "preview",
    ): Triple<String, ResolverContext, String> {
        val expanded = expandShortener(rawUrl.trim(), userAgent)
        val url = UrlNormalizer.normalize(expanded)
        val host = hostOf(url)
        onHost(host, purpose)
        return Triple(url, ResolverContext(client = client, userAgent = userAgent), host)
    }

    private suspend fun runLegacy(
        url: String,
        context: ResolverContext,
        host: String,
        proxyTemplate: String,
        results: MutableList<Preview>,
    ) {
        legacyResolvers.forEach { resolver ->
            resolveSafely(resolver, url, context, resolver.name, host)?.let(results::add)
        }
        if (results.none { it.hasImage } && proxyTemplate.isNotBlank() && proxyTemplate.contains("{url}")) {
            val proxyUrl = proxyTemplate.replace("{url}", java.net.URLEncoder.encode(url, "UTF-8"))
            val proxyHost = runCatching { URI.create(proxyUrl).host.orEmpty() }.getOrDefault("")
            onHost(proxyHost, "opt-in proxy")
            val proxyResolver = OpenGraphResolver(userAgentOverride = context.userAgent, name = "opt-in-proxy")
            resolveSafely(proxyResolver, proxyUrl, context, "opt-in-proxy", proxyHost)?.let(results::add)
        }
    }

    private suspend fun resolveSafely(
        resolver: Resolver,
        url: String,
        context: ResolverContext,
        step: String,
        host: String,
    ): Preview? {
        val result = runCatching { resolver.resolve(url, context) }.getOrNull()
        val outcome = when {
            result == null -> "failed"
            result.hasImage -> "ok-image"
            result.title.isNotBlank() || result.description.isNotBlank() -> "ok-metadata"
            else -> "empty"
        }
        onBreadcrumb(step, outcome, host)
        return result
    }

    private fun finalize(results: List<Preview>, url: String): Preview {
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
            faviconUrls = results.flatMap { it.faviconUrls }.distinct(),
            html = results.firstNonBlank { it.html },
            source = primary.source,
        )
    }

    private fun List<Preview>.firstNonBlank(selector: (Preview) -> String): String =
        firstOrNull { selector(it).isNotBlank() }?.let(selector).orEmpty()

    private fun hostOf(url: String): String = runCatching { URI.create(url).host.orEmpty() }.getOrDefault("")

    companion object {
        const val MOBILE_UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

        fun defaultLegacyResolvers(): List<Resolver> = listOf(
            PlatformFastPaths(),
            OpenGraphResolver(userAgentOverride = MOBILE_UA),
            OpenGraphResolver(userAgentOverride = "facebookexternalhit/1.1"),
        )

        internal fun isInstagramHost(host: String): Boolean {
            val normalized = host.trim().lowercase().removeSuffix(".")
            return normalized == "instagram.com" ||
                normalized == "www.instagram.com" ||
                normalized == "m.instagram.com"
        }
    }
}
