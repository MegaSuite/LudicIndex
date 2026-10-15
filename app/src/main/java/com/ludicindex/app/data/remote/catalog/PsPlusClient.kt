package com.ludicindex.app.data.remote.catalog

import kotlinx.serialization.json.Json

/**
 * Fetches a PlayStation Plus catalog. The PlayStation website exposes four lists; we use the game
 * catalog, the classics catalog and the monthly games.
 *
 * Verified live: `plus-games-list` ≈ 470 games, `plus-classics-list` ≈ 170, monthly ≈ a handful.
 */
class PsPlusClient(
    private val api: PsPlusApi,
    private val json: Json,
) : PsPlusCatalog {

    override suspend fun fetchCatalog(
        categoryList: String,
        locale: String,
    ): List<CatalogGame> = catalogCall {
        val raw = api.gamesList(locale, categoryList).string()
        val groups = json.decodeFromString<List<PsPlusCatalogGroup>>(raw)
        val seen = HashSet<String>()
        groups.asSequence()
            .flatMap { it.games }
            .mapNotNull { it.toCatalogGame() }
            .filter { seen.add(it.externalId) }
            .toList()
    }

    private fun PsPlusGame.toCatalogGame(): CatalogGame? {
        val id = productId?.takeIf { it.isNotBlank() } ?: conceptId?.toString() ?: return null
        val name = nameEn?.takeIf { it.isNotBlank() }
            ?: name?.takeIf { it.isNotBlank() }
            ?: return null
        return CatalogGame(
            externalId = id,
            title = name,
            coverUrl = imageUrl?.takeIf { it.isNotBlank() },
            platforms = device,
            releaseDate = parseIsoEpochSeconds(releaseDate),
        )
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://www.playstation.com"
        const val DEFAULT_LOCALE = "en-us"

        const val CATEGORY_GAME_CATALOG = "plus-games-list"
        const val CATEGORY_CLASSICS = "plus-classics-list"
        const val CATEGORY_MONTHLY = "plus-monthly-games-list"
    }
}
