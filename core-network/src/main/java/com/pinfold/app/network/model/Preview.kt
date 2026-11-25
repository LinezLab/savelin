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
    val faviconUrls: List<String> = emptyList(),
    val html: String = "",
    val source: String = "",
) {
    val hasImage: Boolean get() = imageUrls.isNotEmpty()
}
