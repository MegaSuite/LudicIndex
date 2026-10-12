package com.ludicindex.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ludicindex.app.WITCHER_DETAIL_JSON
import com.ludicindex.app.data.local.AppDatabase
import com.ludicindex.app.data.remote.FakeIgdbApi
import com.ludicindex.app.data.remote.FakeIgdbAuth
import com.ludicindex.app.data.remote.IgdbClient
import com.ludicindex.app.data.remote.RateLimiter
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GameDetailRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: GameDetailRepository
    private val api = FakeIgdbApi()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GameDetailRepository(
            client = IgdbClient(
                api = api,
                auth = FakeIgdbAuth(),
                json = json,
                rateLimiter = RateLimiter(minIntervalMillis = 0),
            ),
            cacheDao = db.gameCacheDao(),
            json = json,
            now = { 1_700_000_000_000L },
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun fetchReturnsTheMappedDetailAndCachesTheRawPayload() = runTest {
        api.enqueueJson(WITCHER_DETAIL_JSON)

        val detail = repository.fetch(1942)

        assertEquals("The Witcher 3: Wild Hunt", detail!!.name)
        val cached = db.gameCacheDao().get(1942)
        assertNotNull(cached)
        // Raw payload, so fields we did not map are still recoverable later.
        assertTrue(cached!!.payloadJson.contains("language_supports"))
        assertEquals(1_700_000_000L, cached.fetchedAt)
    }

    @Test
    fun cachedReadsBackWithoutTouchingTheNetwork() = runTest {
        api.enqueueJson(WITCHER_DETAIL_JSON)
        repository.fetch(1942)
        val callsAfterFetch = api.calls.size

        val cached = repository.cached(1942)

        assertEquals("The Witcher 3: Wild Hunt", cached!!.name)
        assertEquals(listOf("Elden Ring", "Dragon Age: Inquisition"), cached.similarGames.map { it.name })
        assertEquals(callsAfterFetch, api.calls.size)
    }

    @Test
    fun fetchAlsoLoadsRelatedContentAndCachesIt() = runTest {
        api.enqueueJson(WITCHER_DETAIL_JSON)
        api.enqueueJson(
            """[{"id":100,"name":"Blood and Wine","game_type":2,"parent_game":1942,
                 "cover":{"image_id":"co1"}}]""",
        )

        val detail = repository.fetch(1942)

        assertEquals(listOf("资料片"), detail!!.relatedGroups.map { it.title })
        assertEquals(listOf("Blood and Wine"), detail.relatedGroups.single().games.map { it.name })

        val callsAfterFetch = api.calls.size
        val cached = repository.cached(1942)

        assertEquals(listOf("资料片"), cached!!.relatedGroups.map { it.title })
        assertEquals(callsAfterFetch, api.calls.size)
    }

    @Test
    fun aFailingRelatedContentCallStillReturnsTheDetail() = runTest {
        api.enqueueJson(WITCHER_DETAIL_JSON)
        api.enqueueIoFailure()

        val detail = repository.fetch(1942)

        assertEquals("The Witcher 3: Wild Hunt", detail!!.name)
        assertTrue(detail.relatedGroups.isEmpty())
    }

    @Test
    fun cachedIsNullBeforeAnythingIsFetched() = runTest {
        assertNull(repository.cached(1942))
    }

    @Test
    fun fetchReturnsNullWhenIgbdHasNoSuchGame() = runTest {
        api.enqueueJson("[]")

        assertNull(repository.fetch(999_999))
    }

    @Test
    fun aFailedFetchLeavesThePreviousCacheIntact() = runTest {
        api.enqueueJson(WITCHER_DETAIL_JSON)
        repository.fetch(1942)

        api.enqueueIoFailure()
        runCatching { repository.fetch(1942) }

        assertNotNull(repository.cached(1942))
    }
}
