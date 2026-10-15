package com.ludicindex.app.data.remote.catalog

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class GamePassClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun fetchCatalogJoinsTheSiglListWithProductDetails() = runTest {
        val client = GamePassClient(FakeGamePassApi(SIGLS_JSON, PRODUCTS_JSON), json)

        val games = client.fetchCatalog("US")

        assertEquals(listOf("A", "B"), games.map { it.externalId })
        val first = games.first()
        assertEquals("Alpha", first.title)
        assertEquals("https://img/a.jpg", first.coverUrl)
        assertEquals(listOf("Xbox Series X|S", "PC"), first.platforms)
        assertEquals(Instant.parse("2020-01-02T00:00:00Z").epochSecond, first.releaseDate)
    }

    @Test
    fun malformedResponseBecomesACatalogParseFailure() = runTest {
        val client = GamePassClient(FakeGamePassApi("not json", PRODUCTS_JSON), json)

        val error = runCatching { client.fetchCatalog("US") }.exceptionOrNull()

        assertTrue(error is CatalogException.Parse)
    }

    private class FakeGamePassApi(
        private val sigls: String,
        private val products: String,
    ) : GamePassApi {
        override suspend fun sigls(siglId: String, language: String, market: String): ResponseBody =
            sigls.toResponseBody(JSON)

        override suspend fun products(
            language: String,
            market: String,
            hydration: String,
            body: RequestBody,
        ): ResponseBody = products.toResponseBody(JSON)

        private companion object {
            private val JSON = "application/json".toMediaType()
        }
    }

    private companion object {
        /** The first element describes the list; the rest carry the product ids. */
        private const val SIGLS_JSON = """
            [
              {"siglId":"f6f1f99f","title":"All Console Games"},
              {"id":"A"},
              {"id":"B"}
            ]
        """

        private const val PRODUCTS_JSON = """
            {
              "Products": {
                "A": {
                  "StoreId": "A",
                  "ProductTitle": "Alpha",
                  "ImageHero": {"URI": "https://img/a.jpg", "Width": 3840, "Height": 2160},
                  "OriginalReleaseDate": "2020-01-02T00:00:00Z",
                  "XboxConsoleGenCompatible": ["ConsoleGen9"],
                  "AllowedPlatforms": ["Windows.Desktop", "Windows.Xbox"]
                },
                "B": {
                  "StoreId": "B",
                  "ProductTitle": "Beta",
                  "ImageTile": {"URI": "https://img/b.jpg"},
                  "XboxConsoleGenCompatible": ["ConsoleGen8"]
                }
              },
              "InvalidIds": []
            }
        """
    }
}
