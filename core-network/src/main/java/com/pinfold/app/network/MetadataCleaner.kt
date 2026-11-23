package com.pinfold.app.network

import java.util.Locale

data class CleanedMetadata(
    val displayTitle: String,
    val authorHandle: String,
    val caption: String,
    val likesText: String,
    val commentsText: String,
    val postedAt: String,
    val hashtags: List<String>,
    /** Union of article:tag / keywords / hashtags, normalised for the tag table. */
    val tags: List<String>,
)

object MetadataCleaner {
    private val INSTAGRAM_DESCRIPTION = Regex(
        """(?:([\d.,KkMm]+)\s+likes,\s*([\d.,KkMm]+)\s+comments\s*-\s*)?""" +
            """(.+?)\s+on\s+([A-Za-z]+\s+\d{1,2},?\s*\d{4}|\d{1,2}\s+[A-Za-z]+\s+\d{4})\s*:\s*(["“])([\s\S]*)(["”])\s*\.?\s*$""",
    )
    private val HASHTAG = Regex("""#[\p{L}\p{N}_]+""")
    private val PUNCTUATION_ONLY = Regex("""^[\s.\-–—·•,;:!?…"“”']+$""")
    private const val MAX_TAGS = 8

    /** Drops dot-only lines, collapses blank runs and removes trailing hashtag-only lines. */
    fun cleanForDisplay(caption: String): String {
        val lines = caption.lineSequence()
            .map { it.trimEnd() }
            .filterNot { it.isBlank() || PUNCTUATION_ONLY.matches(it.trim()) }
            .toList()
        var end = lines.size
        while (end > 0 && lines[end - 1].let { line -> line.startsWith("#") && HASHTAG.findAll(line).count() >= 2 }) {
            end--
        }
        val result = lines.take(end).joinToString("\n")
        return result.replace(Regex("""\n{3,}"""), "\n\n")
    }

    /**
     * article:tag, keywords and hashtags all arrive as messy free text. Keep 2–30 char labels,
     * drop URLs and punctuation-only noise, deduplicate case-insensitively and cap at eight.
     */
    fun cleanTags(raw: Collection<String>): List<String> {
        val seen = HashSet<String>()
        val out = mutableListOf<String>()
        for (entry in raw) {
            for (part in entry.split(',', ';')) {
                val cleaned = part.trim().trimStart('#').trim().replace(Regex("""\s+"""), " ")
                if (cleaned.length !in 2..30) continue
                if (cleaned.contains("://") || cleaned.startsWith("www.", ignoreCase = true)) continue
                if (PUNCTUATION_ONLY.matches(cleaned)) continue
                if (seen.add(cleaned.lowercase(Locale.ROOT))) out += cleaned
                if (out.size >= MAX_TAGS) return out
            }
        }
        return out
    }

    fun clean(
        title: String,
        description: String,
        author: String,
        siteName: String,
        platform: String,
        fallbackTitle: String = "",
        contentTags: List<String> = emptyList(),
    ): CleanedMetadata {
        val match = INSTAGRAM_DESCRIPTION.find(description)
        val likes = match?.groupValues?.get(1).orEmpty()
        val comments = match?.groupValues?.get(2).orEmpty()
        val handle = match?.groupValues?.get(3)?.trim().orEmpty().ifEmpty { author.trim() }
        val posted = match?.groupValues?.get(4).orEmpty()
        val rawCaption = match?.groupValues?.get(6)?.trim().orEmpty()
            ?.trim('"', '“', '”', '\'')
            ?.trim()
            .orEmpty()
            .ifEmpty { description.trim() }
        val caption = cleanForDisplay(rawCaption)

        val captionFirstLine = caption.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
            .orEmpty()
            .let { it.replace(HASHTAG, "").trim() }
            .take(80)

        val usableTitle = if (match != null && captionFirstLine.isNotBlank()) {
            captionFirstLine
        } else {
            title.trim().takeIf {
                it.isNotBlank() && !it.equals(siteName.trim(), ignoreCase = true) && it.length > 2
            }.orEmpty()
        }

        val displayTitle = usableTitle.ifBlank {
            if (handle.isNotBlank()) "@${handle.removePrefix("@")}" else fallbackTitle
        }
        val hashtags = HASHTAG.findAll(caption).map { it.value }.distinct().toList()

        return CleanedMetadata(
            displayTitle = displayTitle,
            authorHandle = handle.removePrefix("@"),
            caption = caption,
            likesText = likes,
            commentsText = comments,
            postedAt = posted,
            hashtags = hashtags,
            tags = cleanTags(contentTags + hashtags),
        )
    }
}
