package com.ludicindex.app.domain.model

/** A game as returned by IGDB search / popularity lists. Detail-only fields live in later phases. */
data class Game(
    val igdbId: Long,
    val name: String,
    val coverImageId: String? = null,
    val releaseDate: Long? = null,
    val rating: Double? = null,
    val summary: String? = null,
    val genres: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
)
