package com.ludicindex.app.domain.website

import java.net.URI

/**
 * IGDB website types, grouped so the detail page can tell a store link from a social one the way
 * the IGDB site does.
 *
 * The documented `websites.type` enum only covers ids 1–19 (official / wiki / social / Steam /
 * Epic / GOG / Itch / app stores / Bluesky). IGDB has since moved `type` onto a `website_types`
 * table and added store types beyond 19 — PlayStation Store, Xbox Store, Nintendo eShop … — with
 * ids that are not documented. So classification is done **by the link's host first** and falls back
 * to the type map: a `store.playstation.com` link is a store no matter which id IGDB gives it.
 */
enum class WebsiteCategory(val label: String) {
    OFFICIAL("官方"),
    STORE("游戏平台 / 商店"),
    SOCIAL("社交 / 社区"),
    WIKI("资料 / 百科"),
    OTHER("其他"),
}

private data class WebsiteTypeInfo(val label: String, val category: WebsiteCategory)

/** Documented ids from the IGDB website-type enum. */
private val WEBSITE_TYPES: Map<Int, WebsiteTypeInfo> = mapOf(
    1 to WebsiteTypeInfo("官网", WebsiteCategory.OFFICIAL),
    2 to WebsiteTypeInfo("Fandom", WebsiteCategory.WIKI),
    3 to WebsiteTypeInfo("Wikipedia", WebsiteCategory.WIKI),
    4 to WebsiteTypeInfo("Facebook", WebsiteCategory.SOCIAL),
    5 to WebsiteTypeInfo("Twitter / X", WebsiteCategory.SOCIAL),
    6 to WebsiteTypeInfo("Twitch", WebsiteCategory.SOCIAL),
    8 to WebsiteTypeInfo("Instagram", WebsiteCategory.SOCIAL),
    9 to WebsiteTypeInfo("YouTube", WebsiteCategory.SOCIAL),
    10 to WebsiteTypeInfo("App Store", WebsiteCategory.STORE),
    11 to WebsiteTypeInfo("iPad", WebsiteCategory.STORE),
    12 to WebsiteTypeInfo("Google Play", WebsiteCategory.STORE),
    13 to WebsiteTypeInfo("Steam", WebsiteCategory.STORE),
    14 to WebsiteTypeInfo("Reddit", WebsiteCategory.SOCIAL),
    15 to WebsiteTypeInfo("Itch.io", WebsiteCategory.STORE),
    16 to WebsiteTypeInfo("Epic Games", WebsiteCategory.STORE),
    17 to WebsiteTypeInfo("GOG", WebsiteCategory.STORE),
    18 to WebsiteTypeInfo("Discord", WebsiteCategory.SOCIAL),
    19 to WebsiteTypeInfo("Bluesky", WebsiteCategory.SOCIAL),
)

private data class HostRule(
    val domains: List<String>,
    val label: String,
    val category: WebsiteCategory,
)

/** Host rules; matched with `host == domain || host.endsWith(".$domain")`. */
private val HOST_RULES: List<HostRule> = listOf(
    // Stores / game platforms
    HostRule(listOf("playstation.com", "sony.com"), "PlayStation Store", WebsiteCategory.STORE),
    HostRule(listOf("xbox.com", "microsoft.com", "live.com"), "Xbox Store", WebsiteCategory.STORE),
    HostRule(listOf("nintendo.com", "nintendo-europe.com"), "Nintendo eShop", WebsiteCategory.STORE),
    HostRule(listOf("steampowered.com", "steamcommunity.com"), "Steam", WebsiteCategory.STORE),
    HostRule(listOf("epicgames.com"), "Epic Games", WebsiteCategory.STORE),
    HostRule(listOf("gog.com"), "GOG", WebsiteCategory.STORE),
    HostRule(listOf("itch.io"), "Itch.io", WebsiteCategory.STORE),
    HostRule(listOf("apps.apple.com", "itunes.apple.com"), "App Store", WebsiteCategory.STORE),
    HostRule(listOf("play.google.com"), "Google Play", WebsiteCategory.STORE),
    HostRule(listOf("ubisoft.com", "ubisoftconnect.com"), "Ubisoft Store", WebsiteCategory.STORE),
    HostRule(listOf("ea.com"), "EA", WebsiteCategory.STORE),
    HostRule(listOf("battle.net", "blizzard.com"), "Battle.net", WebsiteCategory.STORE),
    HostRule(listOf("rockstargames.com"), "Rockstar", WebsiteCategory.STORE),
    HostRule(listOf("bandainamcoent.com"), "Bandai Namco", WebsiteCategory.STORE),
    HostRule(listOf("square-enix.com", "squareenix.com"), "Square Enix", WebsiteCategory.STORE),
    // Social / community
    HostRule(listOf("facebook.com", "fb.com"), "Facebook", WebsiteCategory.SOCIAL),
    HostRule(listOf("twitter.com", "x.com"), "Twitter / X", WebsiteCategory.SOCIAL),
    HostRule(listOf("twitch.tv"), "Twitch", WebsiteCategory.SOCIAL),
    HostRule(listOf("instagram.com"), "Instagram", WebsiteCategory.SOCIAL),
    HostRule(listOf("youtube.com", "youtu.be"), "YouTube", WebsiteCategory.SOCIAL),
    HostRule(listOf("reddit.com"), "Reddit", WebsiteCategory.SOCIAL),
    HostRule(listOf("discord.gg", "discord.com"), "Discord", WebsiteCategory.SOCIAL),
    HostRule(listOf("bsky.app"), "Bluesky", WebsiteCategory.SOCIAL),
    HostRule(listOf("tumblr.com"), "Tumblr", WebsiteCategory.SOCIAL),
    HostRule(listOf("linkedin.com"), "LinkedIn", WebsiteCategory.SOCIAL),
    HostRule(listOf("pinterest.com"), "Pinterest", WebsiteCategory.SOCIAL),
    HostRule(listOf("vk.com"), "VK", WebsiteCategory.SOCIAL),
    HostRule(listOf("telegram.me", "t.me"), "Telegram", WebsiteCategory.SOCIAL),
    // Reference
    HostRule(listOf("wikipedia.org"), "Wikipedia", WebsiteCategory.WIKI),
    HostRule(listOf("fandom.com", "wikia.com"), "Fandom", WebsiteCategory.WIKI),
)

fun websiteCategory(type: Int?, url: String? = null): WebsiteCategory =
    WEBSITE_TYPES[type]?.category ?: hostRule(url)?.category ?: WebsiteCategory.OTHER

/**
 * Friendly link label: the known type name, else the matched host's brand, else the bare host
 * (e.g. `ign.com`). It is never a bare "链接".
 */
fun websiteLabel(type: Int?, url: String? = null): String =
    WEBSITE_TYPES[type]?.label ?: hostRule(url)?.label ?: hostOf(url) ?: "网站"

private fun hostRule(url: String?): HostRule? {
    val host = hostOf(url) ?: return null
    return HOST_RULES.firstOrNull { rule ->
        rule.domains.any { domain -> host == domain || host.endsWith(".$domain") }
    }
}

private fun hostOf(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val host = runCatching { URI(url).host }.getOrNull()?.lowercase() ?: return null
    return host.removePrefix("www.").takeIf { it.isNotBlank() }
}
