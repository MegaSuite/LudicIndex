package com.ludicindex.app.ui.format

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** IGDB ratings are 0-100; the website shows them as a 0-10 score with one decimal. */
fun formatRating(rating: Double?): String? =
    rating?.takeIf { it > 0.0 }?.let { String.format(Locale.US, "%.1f", it / 10.0) }

fun formatReleaseYear(epochSeconds: Long?): String? = epochSeconds?.let {
    runCatching { Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).year.toString() }
        .getOrNull()
}

fun formatReleaseDate(epochSeconds: Long?): String? = epochSeconds?.let {
    runCatching {
        val date = Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate()
        String.format(Locale.US, "%04d-%02d-%02d", date.year, date.monthValue, date.dayOfMonth)
    }.getOrNull()
}

/** 5491 -> "5,491" */
fun formatCount(value: Int?): String? =
    value?.takeIf { it > 0 }?.let { String.format(Locale.US, "%,d", it) }
