package com.ludicindex.app.data.remote

import com.ludicindex.app.data.prefs.IgdbCredentials
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IgdbClientTest {

    private val api = FakeIgdbApi()
    private val auth = FakeIgdbAuth()
    private val json = Json { ignoreUnknownKeys = true }

    private fun client() = IgdbClient(
        api = api,
        auth = auth,
        json = json,
        rateLimiter = RateLimiter(minIntervalMillis = 0),
    )

    @Test
    fun sendsClientIdBearerAndApicalypseBody() = runTest {
        api.enqueueJson("[]")

        client().ping()

        val call = api.calls.single()
        assertEquals("https://api.igdb.com/v4/games", call.url)
        assertEquals("test-client", call.clientId)
        assertEquals("Bearer token-1", call.bearer)
        assertEquals("fields id; limit 1;", call.body)
    }

    @Test
    fun retriesExactlyOnceWithAFreshTokenAfter401() = runTest {
        api.enqueueHttpError(401)
        api.enqueueJson("[]")

        client().ping()

        assertEquals(2, api.calls.size)
        assertEquals("Bearer token-1", api.calls[0].bearer)
        assertEquals("Bearer token-2", api.calls[1].bearer)
        assertEquals(1, auth.refreshCount)
    }

    @Test
    fun surfacesInvalidCredentialsWhenTheRetryAlsoFails() = runTest {
        api.enqueueHttpError(401)
        api.enqueueHttpError(401)

        val error = runCatching { client().ping() }.exceptionOrNull()

        assertTrue(error is IgdbException.InvalidCredentials)
    }

    @Test
    fun maps400ToBadRequestUsingTheIgbdCause() = runTest {
        api.enqueueHttpError(
            400,
            """{"title":"Invalid Field","status":400,"cause":"Invalid field name: 'popularity'"}""",
        )

        val error = runCatching { client().ping() }.exceptionOrNull()

        assertTrue(error is IgdbException.BadRequest)
        assertTrue((error as IgdbException.BadRequest).message!!.contains("Invalid field name"))
    }

    @Test
    fun maps429ToRateLimited() = runTest {
        api.enqueueHttpError(429)

        val error = runCatching { client().ping() }.exceptionOrNull()

        assertTrue(error is IgdbException.RateLimited)
    }

    @Test
    fun maps500ToServerError() = runTest {
        api.enqueueHttpError(503)

        val error = runCatching { client().ping() }.exceptionOrNull()

        assertTrue(error is IgdbException.ServerError)
        assertEquals(503, (error as IgdbException.ServerError).code)
    }

    @Test
    fun mapsIoFailuresToNetworkErrors() = runTest {
        api.enqueueIoFailure()

        val error = runCatching { client().ping() }.exceptionOrNull()

        assertTrue(error is IgdbException.Network)
    }

    @Test
    fun refusesToCallTheApiWithoutCredentials() = runTest {
        auth.credentials = null

        val error = runCatching { client().ping() }.exceptionOrNull()

        assertTrue(error is IgdbException.MissingCredentials)
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun buildsTheRelatedContentQuery() = runTest {
        api.enqueueJson(
            """[{"id":39034,"name":"AC Origins - Deluxe Edition","cover":{"image_id":"co23oa"},
                 "version_parent":28540,"game_type":0,"first_release_date":1509062400}]""".trimIndent(),
        )

        val related = client().relatedGames(28540)

        val call = api.calls.single()
        assertEquals("https://api.igdb.com/v4/games", call.url)
        // One OR query covers both parent_game children and version_parent editions.
        assertTrue(call.body.contains("where parent_game = 28540 | version_parent = 28540;"))
        assertTrue(call.body.contains("sort first_release_date asc;"))
        assertEquals(listOf("AC Origins - Deluxe Edition"), related.map { it.name })
        assertEquals(28540L, related.single().versionParent)
    }

    @Test
    fun buildsThePopularityPrimitivesQuery() = runTest {
        api.enqueueJson("[]")

        client().popularityPrimitives(IgdbClient.POPULARITY_TYPE_VISITS, limit = 24)

        val call = api.calls.single()
        assertEquals("https://api.igdb.com/v4/popularity_primitives", call.url)
        assertEquals(
            "fields game_id,value,popularity_type; where popularity_type = 1; sort value desc; limit 24;",
            call.body,
        )
    }

    @Test
    fun parsesGamesFromTheGamesEndpoint() = runTest {
        api.enqueueJson(
            """
            [
              {"id":1942,"name":"The Witcher 3: Wild Hunt","total_rating":92.75,
               "cover":{"id":480513,"image_id":"coaarl"},"genres":[{"id":12,"name":"Role-playing (RPG)"}]}
            ]
            """.trimIndent(),
        )

        val games = client().gamesByIds(listOf(1942))

        assertEquals(1, games.size)
        assertEquals("The Witcher 3: Wild Hunt", games.single().name)
        assertEquals("coaarl", games.single().cover?.imageId)
    }

    @Test
    fun decodedDetailKeepsNestedCollections() = runTest {
        api.enqueueJson(
            """
            [{"id":1942,"name":"The Witcher 3","screenshots":[{"id":1,"image_id":"sctj8u"}],
              "involved_companies":[{"company":{"id":50,"name":"WB Games"},"publisher":true,"developer":false}],
              "similar_games":[{"id":119133,"name":"Elden Ring"}]}]
            """.trimIndent(),
        )

        val game = client().gameDetail(1942)!!

        assertEquals("sctj8u", game.screenshots.single().imageId)
        assertEquals("WB Games", game.involvedCompanies.single().company?.name)
        assertEquals("Elden Ring", game.similarGames.single().name)
    }
}
