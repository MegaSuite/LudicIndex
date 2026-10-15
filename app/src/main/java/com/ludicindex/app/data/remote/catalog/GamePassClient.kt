package com.ludicindex.app.data.remote.catalog

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Fetches the Xbox Game Pass console catalog.
 *
 * Public endpoints, no login: a SIGL list gives product ids, then the products endpoint returns
 * details (title, art, devices, release date). Verified live against `catalog.gamepass.com`.
 */
class GamePassClient(
    private val api: GamePassApi,
    private val json: Json,
) : GamePassCatalog {

    override suspend fun fetchCatalog(market: String): List<CatalogGame> {
        val language = languageFor(market)
        val ids = fetchIds(language, market)
        if (ids.isEmpty()) return emptyList()
        val products = fetchProducts(ids, language, market)
        val seen = HashSet<String>()
        return ids.mapNotNull { products[it]?.toCatalogGame() }.filter { seen.add(it.externalId) }
    }

    private suspend fun fetchIds(language: String, market: String): List<String> = catalogCall {
        val raw = api.sigls(SIGL_ALL_CONSOLE_GAMES, language, market).string()
        json.decodeFromString<List<GamePassSiglEntry>>(raw).mapNotNull { it.id }
    }

    private suspend fun fetchProducts(
        ids: List<String>,
        language: String,
        market: String,
    ): Map<String, GamePassProduct> {
        val all = LinkedHashMap<String, GamePassProduct>()
        ids.chunked(BATCH_SIZE).forEach { chunk ->
            val body = json
                .encodeToString(GamePassProductsRequest.serializer(), GamePassProductsRequest(chunk))
                .toRequestBody(JSON_MEDIA_TYPE)
            val raw = catalogCall { api.products(language, market, HYDRATION, body).string() }
            all += catalogCall { json.decodeFromString<GamePassProductsResponse>(raw).products }
        }
        return all
    }

    private fun GamePassProduct.toCatalogGame(): CatalogGame? {
        val id = storeId?.takeIf { it.isNotBlank() } ?: return null
        val name = title?.takeIf { it.isNotBlank() } ?: return null
        return CatalogGame(
            externalId = id,
            title = name,
            coverUrl = (hero ?: tile ?: poster)?.uri?.takeIf { it.isNotBlank() },
            platforms = deviceLabels(),
            releaseDate = parseIsoEpochSeconds(releaseDate),
        )
    }

    private fun GamePassProduct.deviceLabels(): List<String> = buildList {
        if (consoleGen?.contains("ConsoleGen9") == true) add("Xbox Series X|S")
        if (consoleGen?.contains("ConsoleGen8") == true) add("Xbox One")
        if (allowedPlatforms?.contains("Windows.Desktop") == true) add("PC")
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://catalog.gamepass.com"
        const val DEFAULT_MARKET = "US"

        /** "All Console Games" — the whole console Game Pass library. */
        private const val SIGL_ALL_CONSOLE_GAMES = "f6f1f99f-9b49-4ccd-b3bf-4d9767a77f5e"
        private const val HYDRATION = "MobileDetailsForConsole"
        private const val BATCH_SIZE = 100

        private fun languageFor(market: String): String = "en-${market.lowercase()}"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
