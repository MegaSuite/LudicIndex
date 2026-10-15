package com.ludicindex.app.domain.model

/** How confident we are that a catalog entry maps to an IGDB game. */
enum class MatchKind {
    /** Not matched to anything. */
    NONE,

    /** Matched by normalized title (may be wrong; the UI should not over-promise). */
    TITLE,

    /** Matched by an exact external id bridge. */
    EXACT;

    companion object {
        fun fromKey(key: String): MatchKind = entries.firstOrNull { it.name == key } ?: NONE
    }
}

/**
 * One game inside a [LibrarySource]. [igdbId] is filled in by the matcher against the local
 * wishlist, so a catalog entry never has to be resolved to IGDB over the network.
 */
data class LibraryItem(
    val id: Long,
    val sourceId: Long,
    val externalId: String,
    val title: String,
    val coverUrl: String? = null,
    val platforms: List<String> = emptyList(),
    val releaseDate: Long? = null,
    val igdbId: Long? = null,
    val matchKind: MatchKind = MatchKind.NONE,
    val ownedAt: Long? = null,
    val leavingAt: Long? = null,
    val addedAt: Long,
)

/** A [LibraryItem] plus the source it came from, which is what the UI actually renders. */
data class LibraryGame(
    val item: LibraryItem,
    val platform: Platform,
    val sourceLabel: String,
)
