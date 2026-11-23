package com.pinfold.app.network.resolvers

import com.pinfold.app.network.model.Preview

/** The main privacy-first "provider": talks only to the site the user saved. */
class UniversalResolver(
    override val name: String = "universal",
    private val mobileUserAgent: String = MOBILE_UA,
) : Resolver {

    override suspend fun resolve(url: String, context: ResolverContext): Preview? {
        val first = attempt(url, context, mobileUserAgent) ?: return null
        val retryNeeded = first.statusCode == 403 || first.statusCode == 429 || !meaningful(first.preview)
        val chosen = if (retryNeeded && context.crawlerUserAgent != mobileUserAgent) {
            attempt(url, context, context.crawlerUserAgent) ?: first
        } else {
            first
        }
        val base = chosen.preview ?: return null
        val withOEmbed = enrichOEmbed(base, chosen.html, context)
        val withTitle = if (withOEmbed.title.isBlank()) {
            withOEmbed.copy(title = MetadataParser.fallbackTitle(withOEmbed.url))
        } else withOEmbed
        return enrichFavicon(withTitle, chosen.html, context)
    }

    private fun attempt(url: String, context: ResolverContext, userAgent: String): Attempt? {
        val resource = context.fetch(url, context, userAgent) ?: return null
        val contentType = resource.contentType.substringBefore(';').trim()
        val kind = ContentClassifier.kind(resource.finalUrl, contentType)
        if (kind == ContentClassifier.Kind.IMAGE || kind == ContentClassifier.Kind.MEDIA ||
            kind == ContentClassifier.Kind.DOCUMENT
        ) {
            val direct = Preview(
                url = resource.finalUrl,
                title = MetadataParser.fallbackTitle(resource.finalUrl),
                description = ContentClassifier.summary(kind, resource.finalUrl),
                imageUrls = if (kind == ContentClassifier.Kind.IMAGE) listOf(resource.finalUrl) else emptyList(),
                source = name,
                contentType = contentType,
                canonicalUrl = resource.finalUrl,
            )
            return Attempt(resource, direct, "", resource.statusCode)
        }
        val html = HtmlFetcher.decode(resource)
        val preview = MetadataParser.parseHtml(html, resource.finalUrl, name, contentType)
        return Attempt(resource, preview, html, resource.statusCode)
    }

    private fun meaningful(preview: Preview?): Boolean {
        if (preview == null) return false
        return preview.title.isNotBlank() || preview.description.isNotBlank() || preview.imageUrls.isNotEmpty() ||
            preview.html.contains("application/ld+json", ignoreCase = true)
    }

    private fun enrichOEmbed(preview: Preview, html: String, context: ResolverContext): Preview {
        if (html.isBlank()) return preview
        val endpoint = OEmbedParser.findJsonEndpoint(html, preview.url)
        if (endpoint.isBlank() || !MetadataParser.sameSite(endpoint, preview.url)) return preview
        context.onHost(MetadataParser.hostOf(endpoint), "oEmbed")
        val resource = context.fetch(endpoint, context.copy(maxBodyBytes = 64L * 1024), context.userAgent)
            ?: return preview
        if (resource.statusCode !in 200..299) return preview
        val data = OEmbedParser.parse(HtmlFetcher.decode(resource))
        if (data.title.isBlank() && data.thumbnailUrl.isBlank() && data.authorName.isBlank()) return preview
        val thumbnail = MetadataParser.absoluteUrl(endpoint, data.thumbnailUrl)
        val images = buildList {
            if (MetadataParser.isHttpUrl(thumbnail)) add(thumbnail)
            addAll(preview.imageUrls)
        }.distinct()
        return preview.copy(
            title = preview.title.ifBlank { data.title },
            author = preview.author.ifBlank { data.authorName },
            siteName = preview.siteName.ifBlank { data.providerName },
            imageUrls = images,
        )
    }

    private fun enrichFavicon(preview: Preview, html: String, context: ResolverContext): Preview {
        if (html.isBlank()) return preview
        val manifest = MetadataParser.manifestUrl(html, preview.url)
        if (manifest.isBlank() || !MetadataParser.sameSite(manifest, preview.url)) return preview
        context.onHost(MetadataParser.hostOf(manifest), "manifest")
        val resource = context.fetch(manifest, context.copy(maxBodyBytes = 32L * 1024), context.userAgent)
            ?: return preview
        if (resource.statusCode !in 200..299) return preview
        val icon = MetadataParser.iconFromManifest(HtmlFetcher.decode(resource), resource.finalUrl)
        if (icon.isBlank() || !MetadataParser.sameSite(icon, preview.url)) return preview
        return preview.copy(faviconUrls = (listOf(icon) + preview.faviconUrls).distinct().take(6))
    }

    private data class Attempt(
        val resource: FetchedResource,
        val preview: Preview?,
        val html: String,
        val statusCode: Int,
    )

    private companion object {
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
    }
}
