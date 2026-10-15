package com.ludicindex.app.data.remote.catalog

import kotlinx.serialization.Serializable

/** The response is an A-Z list of groups, each holding its games. */
@Serializable
internal data class PsPlusCatalogGroup(
    val catalogKey: String? = null,
    val count: Int? = null,
    val games: List<PsPlusGame> = emptyList(),
)

@Serializable
internal data class PsPlusGame(
    val conceptId: Long? = null,
    val name: String? = null,
    val nameEn: String? = null,
    val conceptUrl: String? = null,
    val imageUrl: String? = null,
    val genre: List<String> = emptyList(),
    val device: List<String> = emptyList(),
    val releaseDate: String? = null,
    val productId: String? = null,
    val streamingSupported: Boolean? = null,
)
