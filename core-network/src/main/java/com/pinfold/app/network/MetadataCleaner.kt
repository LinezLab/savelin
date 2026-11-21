package com.pinfold.app.network

data class CleanedMetadata(
    val displayTitle: String,
    val authorHandle: String,
    val caption: String,
    val likesText: String,
    val commentsText: String,
    val postedAt: String,
    val hashtags: List<String>,
)

object MetadataCleaner {
    private val INSTAGRAM_DESCRIPTION = Regex(
        """([\d.,KkMm]+)\s+likes,\s*([\d.,KkMm]+)\s+comments\s*-\s*(.+?)\s+on\s+([A-Za-z]+\s+\d{1,2},\s*\d{4})\s*:\s*"([\s\S]*)"""",
    )
    private val HASHTAG = Regex("#[\\p{L}\\p{N}_]+")

    fun clean(
        title: String,
        description: String,
        author: String,
        siteName: String,
        platform: String,
        fallbackTitle: String = "",
    ): CleanedMetadata {
        val match = INSTAGRAM_DESCRIPTION.find(description)
        val likes = match?.groupValues?.get(1).orEmpty()
        val comments = match?.groupValues?.get(2).orEmpty()
        val handle = match?.groupValues?.get(3)?.trim().orEmpty().ifEmpty { author.trim() }
        val posted = match?.groupValues?.get(4).orEmpty()
        val caption = match?.groupValues?.get(5)?.trim().orEmpty().ifEmpty { description.trim() }

        val usableTitle = title.trim().takeIf {
            it.isNotBlank() && !it.equals(siteName.trim(), ignoreCase = true) && it.length > 2
        } ?: caption.lineSequence().firstOrNull()?.trim().orEmpty().take(80)

        val displayTitle = usableTitle.ifBlank {
            if (handle.isNotBlank()) "@${handle.removePrefix("@")}" else fallbackTitle
        }

        return CleanedMetadata(
            displayTitle = displayTitle,
            authorHandle = handle.removePrefix("@"),
            caption = caption,
            likesText = likes,
            commentsText = comments,
            postedAt = posted,
            hashtags = HASHTAG.findAll(caption).map { it.value }.distinct().toList(),
        )
    }
}
