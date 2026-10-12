package com.ludicindex.app.domain.model

data class WishlistEntry(
    val id: Long,
    val igdbId: Long,
    val name: String,
    val coverImageId: String?,
    val releaseDate: Long?,
    val rating: Double?,
    val summary: String?,
    val status: EntryStatus = EntryStatus.WANT,
    val note: String = "",
    val addedAt: Long,
    val updatedAt: Long,
    val tags: List<Tag> = emptyList(),
)
