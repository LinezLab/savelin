package com.pinfold.app.network.resolvers

import com.pinfold.app.network.model.Preview
import java.net.URI
import java.util.Locale

internal const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

/** True only for hosts whose desktop pages expose usable Open Graph metadata. */
internal fun isDesktopSiteHost(url: String): Boolean {
    val host = runCatching { URI.create(url.trim()).host.orEmpty() }
        .getOrDefault("").lowercase(Locale.ROOT).trimEnd('.')
    val normalized = host.removePrefix("www.").removePrefix("m.")
    return normalized == "play.google.com" || normalized == "apkmirror.com"
}

class HostAwareDesktopResolver(
    private val fetch: (url: String, context: ResolverContext, agent: String) -> Pair<String, String>? =
        { url, context, agent -> HtmlFetcher.get(url, context, agent) },
) : Resolver {
    override val name = "desktop-site"

    override suspend fun resolve(url: String, context: ResolverContext): Preview? {
        if (!isDesktopSiteHost(url)) return null
        val (html, finalUrl) = runCatching { fetch(url, context, DESKTOP_UA) }.getOrNull() ?: return null
        return OpenGraphResolver.parse(html, finalUrl, name)
    }
}
