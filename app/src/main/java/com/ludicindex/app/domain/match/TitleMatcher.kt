package com.ludicindex.app.domain.match

import java.util.Locale

/**
 * Conservative title matching between platform catalogs and IGDB titles.
 *
 * Catalogs spell the same game differently: "Hollow Knight: Voidheart Edition", "Stardew Valley
 * PS4 & PS5", "Cyberpunk 2077\u2122". We normalize case/punctuation and strip a small, explicit set
 * of edition/platform suffixes. Anything more aggressive (dropping subtitles after ":") would merge
 * sequels such as "Hollow Knight" and "Hollow Knight: Silksong", so it is deliberately not done.
 */
object TitleMatcher {

    private val NON_ALNUM = Regex("[^\\p{L}\\p{N}]+")
    private val MULTI_SPACE = Regex("\\s+")
    private val MARKS = Regex("[\u2122\u00AE\u00A9]")

    /** Trailing platform markers such as "PS4 & PS5" that catalogs append to titles. */
    private val PLATFORM_TAIL = Regex(
        "(?i)[\\s\\u3000]*[-\u2013\u2014|]?\\s*" +
            "(ps5\\s*(&|/|and)\\s*ps4|ps4\\s*(&|/|and)\\s*ps5|ps5|ps4)\\s*\$",
    )

    /** Suffixes that never change which game a title refers to. */
    private val EDITION_SUFFIXES = listOf(
        "deluxe edition",
        "digital deluxe edition",
        "ultimate edition",
        "definitive edition",
        "complete edition",
        "gold edition",
        "standard edition",
        "enhanced edition",
        "special edition",
        "collector s edition",
        "collectors edition",
        "game of the year edition",
        "goty edition",
        "remastered",
        "remaster",
        "ps5 and ps4",
        "ps4 and ps5",
        "ps5",
        "ps4",
        "xbox series x s",
        "xbox one",
    )

    fun normalize(raw: String): String = raw
        .lowercase(Locale.ROOT)
        .replace(MARKS, " ")
        .replace("&", " and ")
        .replace(NON_ALNUM, " ")
        .trim()
        .replace(MULTI_SPACE, " ")

    /** Normalized title with trailing edition/platform noise removed. */
    fun matchKey(raw: String): String {
        var text = normalize(raw)
        var changed = true
        while (changed) {
            changed = false
            for (suffix in EDITION_SUFFIXES) {
                if (text.length > suffix.length && text.endsWith(" $suffix")) {
                    text = text.dropLast(suffix.length + 1).trim()
                    changed = true
                }
            }
        }
        return text
    }

    /**
     * A lightly cleaned title for an IGDB search: drops trailing "PS4 & PS5" markers (which the
     * catalogs append) and folds the full-width space, but keeps the real title and its case.
     */
    fun searchTitle(raw: String): String =
        raw.replace('\u3000', ' ').replace(PLATFORM_TAIL, "").trim()

    /**
     * Builds a lookup from match key to the IGDB id that owns it. Keys shared by more than one game
     * are dropped: an ambiguous match is worse than no match.
     */
    fun unambiguousIndex(entries: List<Pair<Long, String>>): Map<String, Long> {
        val buckets = HashMap<String, MutableList<Long>>()
        entries.forEach { (igdbId, title) ->
            val key = matchKey(title)
            if (key.isNotEmpty()) buckets.getOrPut(key) { mutableListOf() }.add(igdbId)
        }
        return buckets.filterValues { it.size == 1 }.mapValues { it.value.first() }
    }
}
