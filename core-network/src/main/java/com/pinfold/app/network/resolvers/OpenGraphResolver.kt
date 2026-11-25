package com.pinfold.app.network.resolvers

import com.pinfold.app.network.model.Preview
import org.jsoup.Jsoup

class OpenGraphResolver(
    override val name: String = "open-graph",
    private val userAgentOverride: String? = null,
) : Resolver {
    override suspend fun resolve(url: String, context: ResolverContext): Preview? {
        val ua = userAgentOverride ?: context.userAgent
        val fetched = HtmlFetcher.get(url, context, ua) ?: return null
        val (html, finalUrl) = fetched
        return parse(html, finalUrl, name)
    }

    companion object {
        fun parse(html: String, finalUrl: String, source: String = "open-graph"): Preview {
            val doc = Jsoup.parse(html)

            fun meta(vararg keys: String): String {
                for (key in keys) {
                    val value = doc.select("meta[property=\"$key\"]").attr("content").ifEmpty {
                        doc.select("meta[name=\"$key\"]").attr("content")
                    }
                    if (value.isNotBlank()) return value
                }
                return ""
            }

            val title = meta("og:title", "twitter:title").ifBlank { doc.title().orEmpty() }
            val description = meta("og:description", "twitter:description", "description")
            val site = meta("og:site_name", "application-name")

            val images = linkedMapOf<String, Unit>()
            listOf(
                meta("og:image:secure_url"),
                meta("og:image"),
                meta("twitter:image:src"),
                meta("twitter:image"),
            ).forEach { if (it.isNotBlank()) images[it] = Unit }
            doc.select("link[rel=image_src]").attr("href").takeIf { it.isNotBlank() }?.let { images[it] = Unit }

            doc.select("script[type=application/ld+json]").forEach { script ->
                IMAGE_REGEX.findAll(script.data()).forEach { match ->
                    val candidate = match.groupValues[1].replace("\\/", "/").replace("\\u002F", "/")
                    if (candidate.startsWith("http")) images[candidate] = Unit
                }
            }

            doc.select("img[src]").take(12).forEach { img ->
                val src = img.attr("src")
                val abs = try {
                    java.net.URI(finalUrl).resolve(src).toString()
                } catch (_: Exception) {
                    src
                }
                if (abs.startsWith("http")) images[abs] = Unit
            }

            val resolved = images.keys.map { image ->
                try {
                    java.net.URI(finalUrl).resolve(image).toString()
                } catch (_: Exception) {
                    image
                }
            }.distinct()

            var author = ""
            var caption = description
            INSTAGRAM_TITLE.matchEntire(title)?.let {
                author = it.groupValues[1].trim()
                caption = it.groupValues[2].trim()
            }

            return Preview(
                url = finalUrl,
                title = title,
                description = description,
                siteName = site,
                author = author,
                caption = caption,
                imageUrls = resolved,
                faviconUrls = FaviconExtractor.collect(doc, finalUrl),
                html = html.take(16_000),
                source = source,
            )
        }

        private val IMAGE_REGEX = Regex("\"image\"\\s*:\\s*\\[?\\s*\"([^\"]+)\"", RegexOption.IGNORE_CASE)
        private val INSTAGRAM_TITLE = Regex("""^(.*?) on Instagram: "(.*)"$""", RegexOption.DOT_MATCHES_ALL)
    }
}
