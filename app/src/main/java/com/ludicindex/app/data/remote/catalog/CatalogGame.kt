package com.ludicindex.app.data.remote.catalog

/** A game as exposed by a platform catalog, before it is stored locally. */
data class CatalogGame(
    val externalId: String,
    val title: String,
    val coverUrl: String? = null,
    val platforms: List<String> = emptyList(),
    val releaseDate: Long? = null,
    val leavingAt: Long? = null,
)
