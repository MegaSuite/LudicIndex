package com.ludicindex.app.data.remote.catalog

/** Fetches the Xbox Game Pass catalog for a store market. */
interface GamePassCatalog {
    suspend fun fetchCatalog(market: String = GamePassClient.DEFAULT_MARKET): List<CatalogGame>
}

/** Fetches one PlayStation Plus catalog list. */
interface PsPlusCatalog {
    suspend fun fetchCatalog(
        categoryList: String,
        locale: String = PsPlusClient.DEFAULT_LOCALE,
    ): List<CatalogGame>
}
