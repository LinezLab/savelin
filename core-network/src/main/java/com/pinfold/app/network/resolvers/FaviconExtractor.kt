package com.pinfold.app.network.resolvers

import org.jsoup.nodes.Document
import java.net.URI
import java.util.Locale

/** Collects directly declared, host-policy-approved icon candidates ordered best first. */
object FaviconExtractor {
    fun collect(doc: Document, finalUrl: String): List<String> {
        val ranked = linkedMapOf<String, Int>()
        for (link in doc.select("link")) {
            val rels = relTokens(link.attr("rel"))
            val raw = link.attr("href").trim()
            if (raw.isEmpty()) continue
            val href = ShelfStyleResolver.makeAbsoluteUrl(raw, finalUrl)
            if (!FaviconHostPolicy.allowed(finalUrl, href)) continue
            val score = when {
                rels.any { it.contains("apple-touch-icon") } -> 30
                rels.any { it == "icon" } -> 20
                rels.any { it.contains("shortcut") } -> 10
                else -> continue
            }
            val area = link.attr("sizes").split(' ').mapNotNull { size ->
                val parts = size.lowercase(Locale.ROOT).split('x')
                if (parts.size == 2) (parts[0].toIntOrNull() ?: 0) * (parts[1].toIntOrNull() ?: 0) else 0
            }.maxOrNull() ?: 0
            ranked[href] = maxOf(ranked[href] ?: 0, score + area)
        }
        val icons = ranked.entries.sortedByDescending { it.value }.map { it.key }.toMutableList()
        if (FaviconHostPolicy.shouldAddIcoFallback(finalUrl)) {
            val uri = runCatching { URI.create(finalUrl) }.getOrNull()
            if (uri != null) {
                val fallback = "${uri.scheme}://${uri.authority}/favicon.ico"
                if (FaviconHostPolicy.allowed(finalUrl, fallback)) icons += fallback
            }
        }
        return icons.distinct().take(6)
    }

    private fun relTokens(raw: String): List<String> =
        raw.trim().lowercase(Locale.ROOT).split(Regex("""\s+""")).filter { it.isNotBlank() }
}
