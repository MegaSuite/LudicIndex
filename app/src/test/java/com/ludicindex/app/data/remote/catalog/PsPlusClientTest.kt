package com.ludicindex.app.data.remote.catalog

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PsPlusClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun fetchCatalogFlattensGroupsAndPrefersTheEnglishName() = runTest {
        val client = PsPlusClient(FakePsPlusApi(CATALOG_JSON), json)

        val games = client.fetchCatalog(PsPlusClient.CATEGORY_GAME_CATALOG, "en-us")

        assertEquals(listOf("UP3864-CUSA09267_00-AHATINTIMEPS4000", "UP0001-PPSA04716_00-ANNO1800PS000000"), games.map { it.externalId })
        assertEquals("A Hat in Time", games[0].title)
        assertEquals(listOf("PS4"), games[0].platforms)
        assertEquals(Instant.parse("2017-12-05T05:00:00Z").epochSecond, games[0].releaseDate)
    }

    @Test
    fun aGameWithoutAProductIdFallsBackToTheConceptId() = runTest {
        val client = PsPlusClient(FakePsPlusApi(CONCEPT_ONLY_JSON), json)

        val games = client.fetchCatalog(PsPlusClient.CATEGORY_CLASSICS, "en-us")

        assertEquals("10011762", games.single().externalId)
        assertEquals("Alone in the Dark 2 (1996)", games.single().title)
    }

    @Test
    fun malformedResponseBecomesACatalogParseFailure() = runTest {
        val client = PsPlusClient(FakePsPlusApi("<html>nope</html>"), json)

        val error = runCatching { client.fetchCatalog("plus-games-list") }.exceptionOrNull()

        assertTrue(error is CatalogException.Parse)
    }

    private class FakePsPlusApi(private val body: String) : PsPlusApi {
        override suspend fun gamesList(locale: String, categoryList: String): ResponseBody =
            body.toResponseBody("application/json".toMediaType())
    }

    private companion object {
        private const val CATALOG_JSON = """
            [
              {"catalogKey":"A","count":1,"games":[
                {"conceptId":228903,"name":"A Hat in Time","nameEn":"A Hat in Time",
                 "imageUrl":"https://img/a.png","genre":["ADVENTURE"],"device":["PS4"],
                 "releaseDate":"2017-12-05T05:00:00Z",
                 "productId":"UP3864-CUSA09267_00-AHATINTIMEPS4000"}
              ]},
              {"catalogKey":"B","count":1,"games":[
                {"conceptId":10003143,"name":"Anno 1800 中文名","nameEn":"Anno 1800",
                 "imageUrl":"https://img/b.png","device":["PS5"],
                 "releaseDate":"2023-03-16T04:00:00Z",
                 "productId":"UP0001-PPSA04716_00-ANNO1800PS000000"}
              ]}
            ]
        """

        private const val CONCEPT_ONLY_JSON = """
            [
              {"catalogKey":"A","count":1,"games":[
                {"conceptId":10011762,"name":"Alone in the Dark 2 (1996)",
                 "nameEn":"Alone in the Dark 2 (1996)","device":["PS4","PS5"]}
              ]}
            ]
        """
    }
}
