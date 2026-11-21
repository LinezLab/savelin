package com.pinfold.app.network.resolvers

import com.pinfold.app.network.model.Preview
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import java.net.URI

class PlatformFastPaths : Resolver {
    override val name = "platform-fast-path"

    override suspend fun resolve(url: String, context: ResolverContext): Preview? {
        val host = runCatching { URI.create(url).host?.lowercase()?.removePrefix("www.") }.getOrNull()
            ?: return null
        return when {
            host == "youtube.com" || host == "m.youtube.com" || host == "youtu.be" -> youtube(url, host)
            host == "reddit.com" || host == "old.reddit.com" || host == "www.reddit.com" -> reddit(url, context)
            else -> null
        }
    }

    private fun youtube(url: String, host: String): Preview? {
        val id = when {
            host == "youtu.be" -> url.substringAfter("youtu.be/").substringBefore('?').substringBefore('/')
            url.contains("/shorts/") -> url.substringAfter("/shorts/").substringBefore('?').substringBefore('/')
            url.contains("/embed/") -> url.substringAfter("/embed/").substringBefore('?').substringBefore('/')
            else -> Regex("[?&]v=([\\w-]+)").find(url)?.groupValues?.get(1).orEmpty()
        }
        if (id.isBlank() || !id.matches(Regex("[\\w-]{6,}"))) return null
        return Preview(
            url = url,
            title = "",
            platform = "YouTube",
            imageUrls = listOf(
                "https://img.youtube.com/vi/$id/maxresdefault.jpg",
                "https://img.youtube.com/vi/$id/hqdefault.jpg",
            ),
            source = name,
        )
    }

    private suspend fun reddit(url: String, context: ResolverContext): Preview? {
        val jsonUrl = if (url.endsWith(".json")) url else "$url.json"
        val fetched = HtmlFetcher.get(jsonUrl, context, context.userAgent) ?: return null
        val text = fetched.first
        val root = runCatching { Json.parseToJsonElement(text) }.getOrNull() ?: return null
        val listing = (root as? JsonArray)?.firstOrNull() as? JsonObject
        val listingData = listing?.get("data") as? JsonObject
        val children = listingData?.get("children") as? JsonArray
        val child = children?.firstOrNull() as? JsonObject
        val post = child?.get("data") as? JsonObject ?: return null

        val title = post["title"]?.asText().orEmpty()
        val selftext = post["selftext"]?.asText().orEmpty()
        val images = mutableListOf<String>()
        val previewImages = post["preview"] as? JsonObject
        val imagesArray = previewImages?.get("images") as? JsonArray
        imagesArray?.forEach { image ->
            val source = (image as? JsonObject)?.get("source") as? JsonObject
            source?.get("url")?.asText()?.takeIf { it.isNotBlank() }?.let { images += it }
        }
        post["url_overridden_by_dest"]?.asText()?.takeIf { it.containsImageExtension() }?.let { images += it }

        return Preview(
            url = url,
            title = title,
            description = selftext,
            platform = "Reddit",
            imageUrls = images.distinct(),
            source = name,
        )
    }

    private fun kotlinx.serialization.json.JsonElement.asText(): String =
        (this as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull.orEmpty()

    private fun String.containsImageExtension() =
        listOf(".jpg", ".jpeg", ".png", ".webp").any { contains(it, ignoreCase = true) }
}
