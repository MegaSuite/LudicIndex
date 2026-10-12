package com.ludicindex.app.ui.format

/** IGDB website type ids (the enum-to-table migration keeps the numbering). */
private val WEBSITE_TYPE_LABELS = mapOf(
    1 to "官网",
    2 to "Wikia",
    3 to "Wikipedia",
    4 to "Facebook",
    5 to "Twitter",
    6 to "Twitch",
    8 to "Instagram",
    9 to "YouTube",
    10 to "App Store",
    11 to "iPad",
    12 to "Android",
    13 to "Steam",
    14 to "Reddit",
    15 to "Itch.io",
    16 to "Epic Games",
    17 to "GOG",
    18 to "Discord",
)

fun websiteTypeLabel(type: Int?): String =
    type?.let { WEBSITE_TYPE_LABELS[it] } ?: "链接"
