package com.pinfold.app.network

import java.net.URI

object PlatformDetector {
    private val RULES = listOf(
        "youtube.com" to "YouTube",
        "youtu.be" to "YouTube",
        "instagram.com" to "Instagram",
        "tiktok.com" to "TikTok",
        "twitter.com" to "X",
        "x.com" to "X",
        "facebook.com" to "Facebook",
        "fb.me" to "Facebook",
        "linkedin.com" to "LinkedIn",
        "pinterest.com" to "Pinterest",
        "pin.it" to "Pinterest",
        "reddit.com" to "Reddit",
        "redd.it" to "Reddit",
        "github.com" to "GitHub",
        "medium.com" to "Medium",
        "substack.com" to "Substack",
        "wikipedia.org" to "Wikipedia",
        "soundcloud.com" to "SoundCloud",
        "spotify.com" to "Spotify",
        "twitch.tv" to "Twitch",
        "vimeo.com" to "Vimeo",
    )

    fun platform(url: String): String {
        val host = runCatching { URI.create(url).host?.lowercase() }.getOrNull() ?: return "Web"
        return RULES.firstOrNull { host == it.first || host.endsWith(".${it.first}") }?.second ?: "Web"
    }

    fun domain(url: String): String =
        runCatching { URI.create(url).host?.lowercase()?.removePrefix("www.") }.getOrNull() ?: ""

    fun siteName(url: String): String = domain(url).substringBefore('.').replaceFirstChar { it.uppercase() }
}
