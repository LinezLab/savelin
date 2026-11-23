package com.pinfold.app.network.model

data class Preview(
    val url: String,
    val title: String = "",
    val description: String = "",
    val siteName: String = "",
    val platform: String = "",
    val author: String = "",
    val caption: String = "",
    val imageUrls: List<String> = emptyList(),
    val html: String = "",
    val source: String = "",
    /** Raw Content-Type (or guessed type for extension-only URLs), e.g. text/html, image/jpeg. */
    val contentType: String = "",
    val canonicalUrl: String = "",
    /** Direct-from-site icon candidates, ordered best first; never a third-party favicon service. */
    val faviconUrls: List<String> = emptyList(),
    val language: String = "",
    val locale: String = "",
    val publishedAt: String = "",
    /** Cleaned article:tag / keywords / hashtag candidates. */
    val tags: List<String> = emptyList(),
) {
    val hasImage: Boolean get() = imageUrls.isNotEmpty()
}
