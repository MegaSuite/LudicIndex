package com.ludicindex.app.data.repository

import com.ludicindex.app.data.remote.FakeIgdbApi
import com.ludicindex.app.data.remote.FakeIgdbAuth
import com.ludicindex.app.data.remote.IgdbClient
import com.ludicindex.app.data.remote.RateLimiter
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositoryTest {

    private val api = FakeIgdbApi()
    private val repository = GameRepository(
        IgdbClient(
            api = api,
            auth = FakeIgdbAuth(),
            json = Json { ignoreUnknownKeys = true },
            rateLimiter = RateLimiter(minIntervalMillis = 0),
        ),
    )

    @Test
    fun popularRightNowRestoresThePopularityOrder() = runTest {
        api.enqueueJson(
            """
            [{"game_id":2,"value":0.9,"popularity_type":1},
             {"game_id":1,"value":0.5,"popularity_type":1}]
            """.trimIndent(),
        )
        // The batch endpoint answers in its own order on purpose.
        api.enqueueJson(
            """
            [{"id":1,"name":"One"},{"id":2,"name":"Two"}]
            """.trimIndent(),
        )

        val games = repository.popularRightNow(limit = 2)

        assertEquals(listOf("Two", "One"), games.map { it.name })
    }

    @Test
    fun popularRightNowFallsBackToHypesWhenPrimitivesAreEmpty() = runTest {
        api.enqueueJson("[]")
        api.enqueueJson("""[{"id":52189,"name":"Grand Theft Auto VI","hypes":1092}]""")

        val games = repository.popularRightNow(limit = 5)

        assertEquals(listOf("Grand Theft Auto VI"), games.map { it.name })
        assertEquals(2, api.calls.size)
        assertTrue(api.calls[1].body.contains("sort hypes desc"))
    }

    @Test
    fun searchExcludesEditionsAndNonStandaloneTypes() = runTest {
        api.enqueueJson("""[{"id":1,"name":"Hollow Knight"}]""")

        val games = repository.searchGames("hollow knight", limit = 30)

        assertEquals(listOf("Hollow Knight"), games.map { it.name })
        val body = api.calls.single().body
        assertTrue(body.contains("search \"hollow knight\""))
        // IGDB's documented way to drop "Deluxe Edition"-style duplicates.
        assertTrue(body.contains("version_parent = null"))
        assertTrue(body.contains("game_type != (1,3,5,7,13,14)"))
        // Filtering on game_type = 0 used to hide remasters like "Gears of War: Reloaded".
        assertFalse(body.contains("game_type = 0"))
    }

    @Test
    fun skipsTheNetworkForTooShortSearchTerms() = runTest {
        assertTrue(repository.searchGames("a").isEmpty())
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun mapsDtoFieldsOntoTheDomainModel() = runTest {
        api.enqueueJson(
            """
            [{"id":7,"name":"Celeste","total_rating":90.5,"first_release_date":1517184000,
              "cover":{"image_id":"co1"}, "genres":[{"name":"Platform"}],
              "platforms":[{"name":"PC (Microsoft Windows)"}],"summary":"climb"}]
            """.trimIndent(),
        )

        val game = repository.searchGames("celeste").single()

        assertEquals(7L, game.igdbId)
        assertEquals(90.5, game.rating!!, 0.001)
        assertEquals("co1", game.coverImageId)
        assertEquals(listOf("Platform"), game.genres)
        assertEquals(listOf("PC (Microsoft Windows)"), game.platforms)
        assertEquals("climb", game.summary)
    }
}
