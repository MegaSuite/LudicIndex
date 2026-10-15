package com.ludicindex.app.data.remote.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The SIGL response is an array whose first element describes the list and whose remaining elements
 * carry the product ids. Only `id` is read here, so the header entry simply yields null.
 */
@Serializable
internal data class GamePassSiglEntry(val id: String? = null)

/** `ImageTile` / `ImagePoster` / `ImageHero` are `{ URI, Width, Height }` objects. */
@Serializable
internal data class GamePassImage(
    @SerialName("URI") val uri: String? = null,
)

@Serializable
internal data class GamePassProduct(
    @SerialName("StoreId") val storeId: String? = null,
    @SerialName("ProductTitle") val title: String? = null,
    @SerialName("ProductType") val productType: String? = null,
    @SerialName("ImageHero") val hero: GamePassImage? = null,
    @SerialName("ImageTile") val tile: GamePassImage? = null,
    @SerialName("ImagePoster") val poster: GamePassImage? = null,
    @SerialName("OriginalReleaseDate") val releaseDate: String? = null,
    @SerialName("XboxConsoleGenCompatible") val consoleGen: List<String>? = null,
    @SerialName("AllowedPlatforms") val allowedPlatforms: List<String>? = null,
)

@Serializable
internal data class GamePassProductsResponse(
    @SerialName("Products") val products: Map<String, GamePassProduct> = emptyMap(),
)

@Serializable
internal data class GamePassProductsRequest(
    @SerialName("Products") val products: List<String>,
)
