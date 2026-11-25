package com.pinfold.app.network.resolvers

import com.pinfold.app.network.model.Preview
import java.net.URI
import java.util.Locale

/**
 * Redlib is a public, privacy-oriented Reddit front end. The app fetches the page itself and
 * parses it on-device: no third-party preview API, key or SDK is involved. Public instances and
 * their URLs rotate, so this small list is best-effort — a dead instance is skipped and the next
 * one is tried. Only the host (never the URL or title) reaches breadcrumb logging, which is done
 * by [com.pinfold.app.network.PreviewFetcher].
 */
internal val REDLIB_INSTANCES = listOf(
    "https://redlib.catsarch.com",
    "https://redlib.perennialte.ch",
    "https://redlib.privacyredirect.com",
)

internal fun isRedditUrl(url: String): Boolean {
    val uri = runCatching { URI.create(url.trim()) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    if (scheme != "http" && scheme != "https") return false
    val host = normalizeRedditHost(uri.host.orEmpty())
    return host == "reddit.com" || host == "redd.it"
}

/** Maps a Reddit link onto a Redlib instance, preserving path and query. Non-Reddit links are unchanged. */
internal fun redlibUrl(original: String, instance: String): String {
    val uri = runCatching { URI.create(original.trim()) }.getOrNull() ?: return original
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    if (scheme != "http" && scheme != "https") return original
    val host = normalizeRedditHost(uri.host.orEmpty())
    if (host != "reddit.com" && host != "redd.it") return original
    val path = uri.rawPath.orEmpty().trimStart('/')
    if (path.isBlank()) return original
    val first = path.substringBefore('/')
    val mapped = when {
        host == "redd.it" -> "comments/$path"
        first == "r" || first == "user" || first == "comments" || first == "p" -> path
        else -> "comments/$path"
    }
    val base = instance.trimEnd('/')
    val query = uri.rawQuery?.takeIf { it.isNotBlank() }
    return if (query.isNullOrEmpty()) "$base/$mapped" else "$base/$mapped?$query"
}

private fun normalizeRedditHost(host: String): String {
    var result = host.lowercase(Locale.ROOT).trimEnd('.')
    while (true) {
        val stripped = when {
            result.startsWith("www.") -> result.removePrefix("www.")
            result.startsWith("old.") -> result.removePrefix("old.")
            result.startsWith("m.") -> result.removePrefix("m.")
            else -> return result
        }
        result = stripped
    }
}

class RedlibResolver(
    private val instances: List<String> = REDLIB_INSTANCES,
    private val fetch: (url: String, context: ResolverContext, agent: String) -> Pair<String, String>? =
        { url, context, agent -> HtmlFetcher.get(url, context, agent) },
) : Resolver {
    override val name = "redlib"

    override suspend fun resolve(url: String, context: ResolverContext): Preview? {
        if (!isRedditUrl(url)) return null
        for (instance in instances) {
            val target = redlibUrl(url, instance)
            val (html, finalUrl) = runCatching { fetch(target, context, DESKTOP_UA) }.getOrNull() ?: continue
            val parsed = OpenGraphResolver.parse(html, finalUrl, name)
            if (parsed.title.isNotBlank() || parsed.imageUrls.isNotEmpty()) {
                return parsed.copy(url = url, platform = "Reddit", source = name)
            }
        }
        return null
    }
}
