package com.pinfold.app.network.resolvers

import com.pinfold.app.network.MetadataCleaner
import com.pinfold.app.network.PlatformDetector
import com.pinfold.app.network.model.Preview
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Pure, offline extraction of every metadata family we understand. No network calls here: this can
 * be unit-tested with recorded HTML. Adult and non-adult pages take the exact same code path —
 * there is no classification, blocking or judging of content anywhere in the resolver.
 */
object MetadataParser {
    private const val SOURCE = "universal"
    private const val MAX_JSON_OBJECTS = 32
    private const val MAX_CONTENT_IMAGES = 24

    fun parseHtml(
        html: String,
        finalUrl: String,
        source: String = SOURCE,
        contentType: String = "text/html",
    ): Preview {
        val doc = Jsoup.parse(html, finalUrl)
        val ld = parseJsonLd(doc)

        val title = firstNonBlank(meta(doc, "og:title", "twitter:title"), ld.headline, ld.name, doc.title())
        val description = firstNonBlank(
            meta(doc, "og:description", "twitter:description", "description"),
            ld.description,
        )
        val siteName = firstNonBlank(
            meta(doc, "og:site_name", "application-name", "twitter:site"),
            ld.publishers.firstOrNull().orEmpty(),
            PlatformDetector.domain(finalUrl),
        )
        val publishedAt = firstNonBlank(
            meta(doc, "article:published_time", "datePublished", "date", "parsely-pub-date"),
            ld.datePublished,
            firstTimeElement(doc),
        )
        var author = firstNonBlank(
            meta(doc, "author", "article:author", "og:article:author", "twitter:creator"),
            ld.authors.firstOrNull().orEmpty(),
        )
        var caption = description
        INSTAGRAM_TITLE.matchEntire(title)?.let { match ->
            if (author.isBlank()) author = match.groupValues[1].trim()
            caption = match.groupValues[2].trim()
        }

        val images = collectImages(doc, finalUrl, ld.images)
        val rawTags = buildList {
            addAll(metas(doc, "article:tag"))
            addAll(meta(doc, "keywords", "article:section").split(','))
            addAll(ld.keywords)
        }
        val language = doc.select("html").firstOrNull()?.attr("lang")?.trim().orEmpty()
        val locale = meta(doc, "og:locale")

        return Preview(
            url = finalUrl,
            title = title,
            description = description,
            siteName = siteName,
            author = author,
            caption = caption,
            imageUrls = images,
            html = html.take(16_000),
            source = source,
            contentType = contentType,
            canonicalUrl = firstNonBlank(linkHref(doc, "canonical"), meta(doc, "og:url")),
            faviconUrls = collectFavicons(doc, finalUrl),
            language = language,
            locale = locale,
            publishedAt = publishedAt,
            tags = MetadataCleaner.cleanTags(rawTags),
        )
    }

    /** Used by tests to prove that a declared non-UTF-8 charset is honoured. */
    fun parseBytes(
        bytes: ByteArray,
        declaredCharset: String?,
        finalUrl: String,
        source: String = SOURCE,
        contentType: String = "text/html",
    ): Preview {
        val charset = runCatching { Charset.forName(declaredCharset ?: "UTF-8") }
            .getOrDefault(StandardCharsets.UTF_8)
        return parseHtml(String(bytes, charset), finalUrl, source, contentType)
    }

    fun manifestUrl(html: String, finalUrl: String): String {
        val doc = Jsoup.parse(html, finalUrl)
        for (link in doc.select("link")) {
            if (relTokens(link.attr("rel")).any { it == "manifest" }) {
                val href = absoluteUrl(finalUrl, link.attr("href"))
                if (isHttpUrl(href)) return href
            }
        }
        return ""
    }

    /** Largest icon declared by a web app manifest, empty when the manifest has none. */
    fun iconFromManifest(json: String, manifestUrl: String): String {
        val root = runCatching { Json.parseToJsonElement(json.trim()) }.getOrNull() as? JsonObject ?: return ""
        val icons = root["icons"] as? JsonArray ?: return ""
        var best = ""
        var bestArea = 0
        for (element in icons) {
            val obj = element as? JsonObject ?: continue
            val href = absoluteUrl(manifestUrl, firstNonBlank(jsonText(obj["src"]), jsonText(obj["url"])))
            if (!isHttpUrl(href)) continue
            val sizes = jsonText(obj["sizes"])
            val area = sizes.split(' ').mapNotNull { size ->
                val parts = size.lowercase(Locale.ROOT).split('x')
                if (parts.size == 2) {
                    val w = parts[0].toIntOrNull() ?: 0
                    val h = parts[1].toIntOrNull() ?: 0
                    w * h
                } else 0
            }.maxOrNull() ?: 0
            if (area >= bestArea) {
                bestArea = area
                best = href
            }
        }
        return best
    }

    fun fallbackTitle(url: String): String {
        val path = runCatching { URI.create(url).path.orEmpty() }.getOrDefault("")
        val segment = path.trim('/').split('/').lastOrNull { it.isNotBlank() }.orEmpty()
        val decoded = runCatching { URLDecoder.decode(segment, "UTF-8") }.getOrDefault(segment)
        val human = decoded.substringBeforeLast('.')
            .replace(Regex("""[._+-]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
        if (human.length < 2) return PlatformDetector.siteName(url)
        return human.substring(0, 1).uppercase(Locale.ROOT) + human.substring(1)
    }

    internal fun absoluteUrl(base: String, raw: String): String {
        val value = raw.trim()
        if (value.isEmpty()) return ""
        return runCatching { URI.create(base).resolve(value).toString() }.getOrDefault(value)
    }

    internal fun isHttpUrl(url: String): Boolean {
        val scheme = runCatching { URI.create(url).scheme?.lowercase(Locale.ROOT) }.getOrNull() ?: return false
        return scheme == "http" || scheme == "https"
    }

    internal fun hostOf(url: String): String =
        runCatching { URI.create(url).host?.lowercase(Locale.ROOT)?.removePrefix("www.") }.getOrDefault("")

    internal fun sameSite(a: String, b: String): Boolean {
        val hostA = hostOf(a)
        val hostB = hostOf(b)
        return hostA.isNotBlank() && hostB.isNotBlank() &&
            (hostA == hostB || hostA.endsWith(".$hostB") || hostB.endsWith(".$hostA"))
    }

    private fun firstNonBlank(vararg values: String): String = values.firstOrNull { it.isNotBlank() }.orEmpty()

    private fun meta(doc: Document, vararg keys: String): String {
        val wanted = keys.map { it.lowercase(Locale.ROOT) }.toSet()
        for (element in doc.select("meta")) {
            val key = element.attr("property").ifBlank { element.attr("name") }.lowercase(Locale.ROOT)
            if (key !in wanted) continue
            val value = element.attr("content").trim()
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun metas(doc: Document, key: String): List<String> {
        val wanted = key.lowercase(Locale.ROOT)
        val out = mutableListOf<String>()
        for (element in doc.select("meta")) {
            val actual = element.attr("property").ifBlank { element.attr("name") }.lowercase(Locale.ROOT)
            if (actual != wanted) continue
            val value = element.attr("content").trim()
            if (value.isNotBlank()) out += value
        }
        return out
    }

    private fun relTokens(raw: String): List<String> =
        raw.trim().lowercase(Locale.ROOT).split(Regex("""\s+""")).filter { it.isNotBlank() }

    private fun linkHref(doc: Document, rel: String): String {
        for (link in doc.select("link")) {
            if (relTokens(link.attr("rel")).contains(rel)) {
                val href = absoluteUrl(doc.location(), link.attr("href"))
                if (href.isNotBlank()) return href
            }
        }
        return ""
    }

    private fun firstTimeElement(doc: Document): String =
        doc.select("time[datetime]").firstOrNull()?.attr("datetime")?.trim().orEmpty()

    private fun collectFavicons(doc: Document, finalUrl: String): List<String> {
        val ranked = linkedMapOf<String, Int>()
        for (link in doc.select("link")) {
            val rels = relTokens(link.attr("rel"))
            val href = absoluteUrl(finalUrl, link.attr("href"))
            if (!isHttpUrl(href) || !sameSite(href, finalUrl)) continue
            val score = when {
                rels.any { it.contains("apple-touch-icon") } -> 30
                rels.any { it == "icon" } -> 20
                rels.any { it.contains("shortcut") } -> 10
                else -> continue
            }
            val sizes = link.attr("sizes")
            val area = sizes.split(' ').mapNotNull { size ->
                val parts = size.lowercase(Locale.ROOT).split('x')
                if (parts.size == 2) (parts[0].toIntOrNull() ?: 0) * (parts[1].toIntOrNull() ?: 0) else 0
            }.maxOrNull() ?: 0
            ranked[href] = maxOf(ranked[href] ?: 0, score + area)
        }
        val icons = ranked.entries.sortedByDescending { it.value }.map { it.key }.toMutableList()
        val uri = runCatching { URI.create(finalUrl) }.getOrNull()
        if (uri != null) {
            val fallback = "${uri.scheme}://${uri.authority}/favicon.ico"
            if (isHttpUrl(fallback)) icons += fallback
        }
        return icons.distinct().take(6)
    }

    private fun collectImages(doc: Document, finalUrl: String, jsonLdImages: List<String>): List<String> {
        val candidates = mutableListOf<ImageCandidate>()
        val ogWidth = meta(doc, "og:image:width").toIntOrNull() ?: 0
        val ogHeight = meta(doc, "og:image:height").toIntOrNull() ?: 0
        addCandidate(candidates, finalUrl, meta(doc, "og:image:secure_url"), 100, ogWidth, ogHeight)
        addCandidate(candidates, finalUrl, meta(doc, "og:image"), 95, ogWidth, ogHeight)
        addCandidate(candidates, finalUrl, meta(doc, "twitter:image:src", "twitter:image"), 85, 0, 0)
        addCandidate(candidates, finalUrl, linkHref(doc, "image_src"), 75, 0, 0)
        jsonLdImages.forEach { addCandidate(candidates, finalUrl, it, 65, 0, 0) }

        for (img in doc.select("img").take(MAX_CONTENT_IMAGES)) {
            val src = img.attr("src")
                .ifBlank { img.attr("data-src") }
                .ifBlank { img.attr("data-original") }
                .ifBlank { img.attr("srcset").substringBefore(',').trim().substringBefore(' ') }
            val width = img.attr("width").toIntOrNull() ?: 0
            val height = img.attr("height").toIntOrNull() ?: 0
            addCandidate(candidates, finalUrl, src, 40, width, height)
        }

        val best = linkedMapOf<String, ImageCandidate>()
        candidates.sortedWith(compareByDescending<ImageCandidate> { it.score }.thenByDescending { it.area })
            .forEach { if (!best.containsKey(it.url)) best[it.url] = it }
        return best.keys.toList()
    }

    private fun addCandidate(
        out: MutableList<ImageCandidate>,
        finalUrl: String,
        raw: String,
        score: Int,
        width: Int,
        height: Int,
    ) {
        val url = absoluteUrl(finalUrl, raw)
        if (!isHttpUrl(url) || isRejectedImage(url, width, height)) return
        out += ImageCandidate(url, score, if (width > 0 && height > 0) width * height else 0)
    }

    private fun isRejectedImage(url: String, width: Int, height: Int): Boolean {
        if ((width in 1..47) || (height in 1..47)) return true
        val lower = url.lowercase(Locale.ROOT)
        return listOf("1x1", "pixel", "spacer", "tracking", "beacon", "blank.gif", "transparent.gif")
            .any { lower.contains(it) }
    }

    private data class ImageCandidate(val url: String, val score: Int, val area: Int)

    private data class JsonLdData(
        val headline: String,
        val name: String,
        val description: String,
        val images: List<String>,
        val authors: List<String>,
        val datePublished: String,
        val keywords: List<String>,
        val publishers: List<String>,
    )

    private fun parseJsonLd(doc: Document): JsonLdData {
        val objects = mutableListOf<JsonObject>()
        for (script in doc.select("script[type=application/ld+json]")) {
            val element = runCatching { Json.parseToJsonElement(script.data().trim()) }.getOrNull() ?: continue
            collectJsonObjects(element, objects)
        }
        fun aggregate(vararg fields: String): String =
            objects.map { obj ->
                fields.firstNotNullOfOrNull { field -> jsonText(obj[field]).takeIf { it.isNotBlank() } }.orEmpty()
            }.firstOrNull { it.isNotBlank() }.orEmpty()

        return JsonLdData(
            headline = aggregate("headline", "name"),
            name = aggregate("name", "headline"),
            description = aggregate("description", "abstract"),
            images = objects.flatMap { obj -> jsonImages(obj["image"]) + jsonImages(obj["thumbnailUrl"]) }.distinct(),
            authors = objects.flatMap { obj -> jsonTexts(obj["author"]) + jsonTexts(obj["creator"]) }.distinct(),
            datePublished = aggregate("datePublished", "dateCreated"),
            keywords = objects.flatMap { obj -> jsonTexts(obj["keywords"]) }.distinct(),
            publishers = objects.flatMap { obj -> jsonTexts(obj["publisher"]) }.distinct(),
        )
    }

    private fun collectJsonObjects(element: JsonElement, out: MutableList<JsonObject>) {
        if (out.size >= MAX_JSON_OBJECTS) return
        when (element) {
            is JsonObject -> {
                out += element
                val graph = element["@graph"]
                if (graph is JsonArray) graph.forEach { collectJsonObjects(it, out) }
            }
            is JsonArray -> element.forEach { collectJsonObjects(it, out) }
            else -> Unit
        }
    }

    private fun jsonText(element: JsonElement?): String = (element as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()

    private fun jsonTexts(element: JsonElement?): List<String> = when (element) {
        is JsonPrimitive -> listOf(jsonText(element)).filter { it.isNotBlank() }
        is JsonArray -> element.map { jsonText(it) }.filter { it.isNotBlank() }
        is JsonObject -> listOf(jsonText(element["name"]), jsonText(element["@id"])).filter { it.isNotBlank() }
        else -> emptyList()
    }

    private fun jsonImages(element: JsonElement?): List<String> = when (element) {
        is JsonPrimitive -> listOf(jsonText(element)).filter { it.isNotBlank() }
        is JsonArray -> element.flatMap { jsonImages(it) }
        is JsonObject -> listOf(element["url"], element["contentUrl"], element["@id"], element["image"])
            .flatMap { jsonImages(it) }
        else -> emptyList()
    }

    private val INSTAGRAM_TITLE = Regex("""^(.*?) on Instagram: "(.*)"$""", RegexOption.DOT_MATCHES_ALL)
}
