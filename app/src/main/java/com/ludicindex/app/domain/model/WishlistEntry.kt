package com.ludicindex.app.domain.model

data class WishlistEntry(
    val id: Long,
    val igdbId: Long,
    val name: String,
    val coverImageId: String?,
    val releaseDate: Long?,
    val rating: Double?,
    val summary: String?,
    /** Store/platform this entry is tracked for, e.g. "Steam". Null when unspecified. */
    val platform: String? = null,
    val note: String = "",
    val addedAt: Long,
    val updatedAt: Long,
    val tags: List<Tag> = emptyList(),
)
