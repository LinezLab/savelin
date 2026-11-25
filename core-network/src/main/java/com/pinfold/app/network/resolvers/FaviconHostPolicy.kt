package com.pinfold.app.network.resolvers

import java.net.URI
import java.util.Locale

/**
 * Host policy shared by favicon extraction, storage and display. Instagram serves its real icons
 * from static.cdninstagram.com, a separate registrable domain, so a plain same-site check rejects
 * every page-declared icon. Only Instagram pages are allowed to use the explicit CDN allowlist.
 */
object FaviconHostPolicy {
    /** Lowercases a URL host and strips leading www./m. labels so all three layers share a key. */
    fun normalizeHost(url: String): String {
        var host = runCatching { URI.create(url).host.orEmpty() }.getOrDefault("").lowercase(Locale.ROOT)
        while (host.startsWith("www.") || host.startsWith("m.")) {
            host = host.removePrefix("www.").removePrefix("m.")
        }
        return host
    }

    fun isInstagramPage(host: String): Boolean {
        var normalized = host.lowercase(Locale.ROOT)
        while (normalized.startsWith("www.") || normalized.startsWith("m.")) {
            normalized = normalized.removePrefix("www.").removePrefix("m.")
        }
        return normalized == "instagram.com" || normalized.endsWith(".instagram.com")
    }

    /** Same-site candidates are always allowed; non-same-site only via the Instagram CDN allowlist. */
    fun allowed(pageUrl: String, iconUrl: String): Boolean {
        val pageHost = normalizeHost(pageUrl)
        val iconHost = normalizeHost(iconUrl)
        if (pageHost.isBlank() || iconHost.isBlank()) return false
        val scheme = runCatching { URI.create(iconUrl).scheme?.lowercase(Locale.ROOT) }.getOrNull()
        if (scheme != "http" && scheme != "https") return false
        if (pageHost == iconHost || pageHost.endsWith(".$iconHost") || iconHost.endsWith(".$pageHost")) return true
        if (!isInstagramPage(pageHost)) return false
        return iconHost == "cdninstagram.com" || iconHost.endsWith(".cdninstagram.com") ||
            iconHost == "fbcdn.net" || iconHost.endsWith(".fbcdn.net")
    }

    /** Instagram's /favicon.ico returns the HTML app shell, so never synthesize that fallback. */
    fun shouldAddIcoFallback(pageUrl: String): Boolean = !isInstagramPage(normalizeHost(pageUrl))
}
