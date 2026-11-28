package com.pinfold.app.network.image

import com.pinfold.app.network.resolvers.FaviconHostPolicy
import java.net.URI
import java.util.Locale

/**
 * Decide which remote images may be downloaded for a page and with which request headers.
 * Instagram media lives on signed, separate CDN hosts that reject generic OkHttp requests.
 */
object RemoteImageHostPolicy {
    private const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    private const val INSTAGRAM_REFERER = "https://www.instagram.com/"

    fun isInstagramPage(pageUrl: String): Boolean =
        FaviconHostPolicy.isInstagramPage(FaviconHostPolicy.normalizeHost(pageUrl))

    /** Same-site images are always allowed; Instagram media only via its real CDN hosts. */
    fun allows(pageUrl: String, imageUrl: String): Boolean {
        val pageHost = FaviconHostPolicy.normalizeHost(pageUrl)
        val imageHost = FaviconHostPolicy.normalizeHost(imageUrl)
        if (pageHost.isBlank() || imageHost.isBlank()) return false
        val scheme = runCatching { URI.create(imageUrl).scheme?.lowercase(Locale.ROOT) }.getOrNull()
        if (scheme != "http" && scheme != "https") return false
        if (pageHost == imageHost || pageHost.endsWith(".$imageHost") || imageHost.endsWith(".$pageHost")) return true
        if (!isInstagramPage(pageUrl)) return false
        return isInstagramCdn(imageHost)
    }

    /** Signed Instagram CDN URLs need a browser UA and an Instagram referer. */
    fun headers(pageUrl: String, imageUrl: String): Map<String, String> {
        if (!isInstagramPage(pageUrl) || !isInstagramCdn(FaviconHostPolicy.normalizeHost(imageUrl))) return emptyMap()
        return mapOf("User-Agent" to BROWSER_UA, "Referer" to INSTAGRAM_REFERER)
    }

    private fun isInstagramCdn(host: String): Boolean =
        host == "cdninstagram.com" || host.endsWith(".cdninstagram.com") ||
            host == "fbcdn.net" || host.endsWith(".fbcdn.net")
}
