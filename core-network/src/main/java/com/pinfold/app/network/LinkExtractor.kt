package com.pinfold.app.network

object LinkExtractor {
    private val URL_REGEX = Regex("""(?:https?://|www\.)[^\s<>\]）】"]+""", RegexOption.IGNORE_CASE)
    private val TRAILING = Regex("""[.,;:!?)\]}'"”’】）】]+$""")

    fun extract(text: String?): List<String> {
        if (text.isNullOrBlank()) return emptyList()
        return URL_REGEX.findAll(text)
            .map { match -> clean(match.value) }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun clean(raw: String): String {
        var value = raw.trim().replace(TRAILING, "")
        if (value.startsWith("www.", ignoreCase = true)) value = "https://$value"
        return value
    }
}
