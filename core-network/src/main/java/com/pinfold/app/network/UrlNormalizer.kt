package com.pinfold.app.network

import java.net.URI

object UrlNormalizer {
    private val TRACKING_EXACT = setOf(
        "fbclid", "gclid", "gclsrc", "igsh", "igshid", "si", "ref", "ref_src", "ref_url",
        "mibextid", "spm", "scm", "utm", "mc_cid", "mc_eid", "vero_id", "oly_enc_id",
        "s_kwcid", "dclid", "msclkid", "twclid", "tt_from", "source", "share_id",
    )
    private val TRACKING_PREFIXES = listOf("utm_", "mc_", "pk_", "hsa_", "dsm_")
    private val SHORTENERS = setOf("t.co", "bit.ly", "tinyurl.com", "goo.gl", "ow.ly", "buff.ly")

    fun normalize(input: String): String {
        val raw = input.trim()
        val uri = runCatching { URI.create(raw) }.getOrNull() ?: return raw
        if (uri.scheme == null || uri.host == null) return raw

        val scheme = uri.scheme.lowercase()
        val host = normalizeHost(uri.host.lowercase())
        val port = if (uri.port in 80..65535 && !isDefaultPort(scheme, uri.port)) ":${uri.port}" else ""
        val path = if (uri.path.isNullOrEmpty()) "" else uri.path.removeSuffix("/").ifEmpty { "/" }
        val query = uri.query?.let { filterQuery(it) }.orEmpty()
        val built = "$scheme://$host$port$path${if (query.isEmpty()) "" else "?$query"}"
        return built
    }

    fun isShortener(url: String): Boolean =
        runCatching { URI.create(url).host?.lowercase()?.removePrefix("www.") }.getOrNull() in SHORTENERS

    fun hasTracking(input: String): Boolean = normalize(input) != input.trim()

    private fun isDefaultPort(scheme: String, port: Int) =
        (scheme == "http" && port == 80) || (scheme == "https" && port == 443)

    private fun normalizeHost(host: String): String = when {
        host.startsWith("m.") -> "www.${host.removePrefix("m.")}"
        else -> host
    }

    internal fun filterQuery(query: String): String = query.split('&')
        .filter { part ->
            if (part.isEmpty()) return@filter false
            val key = part.substringBefore('=').lowercase()
            key !in TRACKING_EXACT && TRACKING_PREFIXES.none { key.startsWith(it) }
        }
        .joinToString("&")
}
