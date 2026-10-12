package com.ludicindex.app.domain

import com.ludicindex.app.domain.model.WishlistEntry

enum class WishlistSort {
    ADDED_DESC,
    NAME_ASC,
    RELEASE_DATE_DESC,
    RATING_DESC,
}

data class WishlistFilter(
    val query: String = "",
    val tagIds: Set<Long> = emptySet(),
    /** true: entry must carry every selected tag. false: any selected tag matches. */
    val matchAllTags: Boolean = true,
    val sort: WishlistSort = WishlistSort.ADDED_DESC,
)

/**
 * Pure filtering/sorting for the wishlist screen. Kept free of Android types so it is unit-testable:
 * wishlists are small, so in-memory filtering beats a pile of dynamic SQL.
 */
fun List<WishlistEntry>.filterAndSort(filter: WishlistFilter): List<WishlistEntry> {
    val query = filter.query.trim()
    val filtered = filter { entry ->
        val matchesQuery = query.isEmpty() || entry.name.contains(query, ignoreCase = true)
        val entryTagIds = entry.tags.mapTo(mutableSetOf()) { it.id }
        val matchesTags = when {
            filter.tagIds.isEmpty() -> true
            filter.matchAllTags -> entryTagIds.containsAll(filter.tagIds)
            else -> filter.tagIds.any { it in entryTagIds }
        }
        matchesQuery && matchesTags
    }

    return when (filter.sort) {
        // id breaks ties so entries added in the same millisecond keep a stable order.
        WishlistSort.ADDED_DESC -> filtered.sortedWith(
            compareByDescending<WishlistEntry> { it.addedAt }.thenByDescending { it.id },
        )
        WishlistSort.NAME_ASC -> filtered.sortedWith(
            compareBy<WishlistEntry> { it.name.lowercase() }.thenByDescending { it.id },
        )
        WishlistSort.RELEASE_DATE_DESC -> filtered.sortedWith(
            nullsLastDescending<WishlistEntry, Long> { it.releaseDate }.then(compareByDescending { it.id }),
        )
        WishlistSort.RATING_DESC -> filtered.sortedWith(
            nullsLastDescending<WishlistEntry, Double> { it.rating }.then(compareByDescending { it.id }),
        )
    }
}

/** Descending order that keeps entries without a value at the bottom. */
private fun <T, R : Comparable<R>> nullsLastDescending(selector: (T) -> R?): Comparator<T> =
    compareByDescending<T> { selector(it) != null }.thenByDescending { selector(it) }
