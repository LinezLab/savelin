package com.pinfold.app.network.resolvers

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup
import java.util.Locale

data class OEmbedData(
    val title: String,
    val authorName: String,
    val providerName: String,
    val thumbnailUrl: String,
)

/** Discovery and parsing of oEmbed as declared by the visited site itself — never a public proxy. */
object OEmbedParser {
    fun findJsonEndpoint(html: String, finalUrl: String): String {
        val doc = Jsoup.parse(html, finalUrl)
        for (link in doc.select("link")) {
            val rels = link.attr("rel").trim().lowercase(Locale.ROOT)
                .split(Regex("""\s+""")).filter { it.isNotBlank() }
            val type = link.attr("type").trim().lowercase(Locale.ROOT)
            if (rels.contains("alternate") && type.contains("application/json+oembed")) {
                val href = MetadataParser.absoluteUrl(finalUrl, link.attr("href"))
                if (MetadataParser.isHttpUrl(href)) return href
            }
        }
        return ""
    }

    fun parse(json: String): OEmbedData {
        val root = runCatching { Json.parseToJsonElement(json.trim()) }.getOrNull() as? JsonObject
            ?: return OEmbedData("", "", "", "")
        fun text(key: String): String = (root[key] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
        val thumbnails = root["thumbnail_url"] as? JsonPrimitive
        return OEmbedData(
            title = text("title"),
            authorName = text("author_name"),
            providerName = text("provider_name"),
            thumbnailUrl = thumbnails?.contentOrNull?.trim().takeUnless { it.isNullOrBlank() }.orEmpty(),
        )
    }
}
