package com.ludicindex.app.domain

/** Strips common list markers such as "- ", "* ", "1. " or "2) " from a pasted line. */
private val LIST_MARKER = Regex("""^\s*(?:[-*•·—]|\d+[.)、])\s*""")

/**
 * Turns pasted text into candidate titles: one per line, markers removed, blanks dropped,
 * duplicates (case-insensitive) collapsed while keeping the user's original order.
 */
fun parseGameTitles(text: String): List<String> {
    val seen = mutableSetOf<String>()
    return text.lineSequence()
        .map { it.replace(LIST_MARKER, "").trim() }
        .filter { it.isNotEmpty() }
        .filter { seen.add(it.lowercase()) }
        .toList()
}
