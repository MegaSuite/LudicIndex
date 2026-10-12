package com.ludicindex.app.data.remote

import com.ludicindex.app.data.prefs.SettingsStore
import com.ludicindex.app.data.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * End-to-end check against the real IGDB API. Skipped unless IGDB_CLIENT_ID / IGDB_CLIENT_SECRET
 * are present in the environment, so credentials never live in the repository.
 *
 *   IGDB_CLIENT_ID=... IGDB_CLIENT_SECRET=... ./gradlew :app:testDebugUnitTest \
 *       --tests '*IgdbLiveIntegrationTest*'
 */
class IgdbLiveIntegrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val clientId = System.getenv("IGDB_CLIENT_ID")?.takeIf { it.isNotBlank() }
    private val clientSecret = System.getenv("IGDB_CLIENT_SECRET")?.takeIf { it.isNotBlank() }

    private lateinit var repository: GameRepository
    private lateinit var client: IgdbClient

    @Before
    fun setUp() {
        assumeTrue(
            "IGDB_CLIENT_ID / IGDB_CLIENT_SECRET not set — skipping the live integration test",
            clientId != null && clientSecret != null,
        )

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
        val converter = json.asConverterFactory("application/json".toMediaType())

        val store = SettingsStore(
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            ) { File(temporaryFolder.root, "live.preferences_pb") },
        )
        runBlocking { store.saveCredentials(clientId!!, clientSecret!!) }

        val igdbApi = Retrofit.Builder()
            .baseUrl(IgdbClient.DEFAULT_BASE_URL + "/")
            .client(okHttp)
            .addConverterFactory(converter)
            .build()
            .create(IgdbApi::class.java)

        val twitchAuthApi = Retrofit.Builder()
            .baseUrl("https://id.twitch.tv/")
            .client(okHttp)
            .addConverterFactory(converter)
            .build()
            .create(TwitchAuthApi::class.java)

        client = IgdbClient(
            api = igdbApi,
            auth = TwitchTokenProvider(store, twitchAuthApi),
            json = json,
            rateLimiter = RateLimiter(),
        )
        repository = GameRepository(client)
    }

    @Test
    fun testConnectionSucceedsWithRealCredentials() = runBlocking {
        repository.testConnection()
    }

    @Test
    fun popularRightNowReturnsRankedGamesWithCovers() = runBlocking {
        val games = repository.popularRightNow(limit = 24)

        assertTrue("expected a non-empty ranking", games.isNotEmpty())
        assertTrue("ids must be distinct", games.map { it.igdbId }.toSet().size == games.size)
        assertTrue("every game needs a name", games.all { it.name.isNotBlank() })
        assertTrue("most games should have a cover", games.count { it.coverImageId != null } > games.size / 2)
    }

    @Test
    fun searchFindsAKnownGame() = runBlocking {
        val games = repository.searchGames("hollow knight", limit = 10)

        assertTrue(games.isNotEmpty())
        assertTrue(games.any { it.name.contains("Hollow Knight", ignoreCase = true) })
    }

    /**
     * Regression: this is a remaster (game_type 9). Filtering search on `game_type = 0` used to
     * hide it even though IGDB's own site lists it.
     */
    @Test
    fun searchFindsRemastersAndRemakes() = runBlocking {
        val games = repository.searchGames("Gears of War Reloaded", limit = 10)

        assertTrue(
            "expected Gears of War: Reloaded, got ${games.map { it.name }}",
            games.any { it.name.contains("Gears of War: Reloaded", ignoreCase = true) },
        )
    }

    /**
     * Regression: "Deluxe Edition" style duplicates carry version_parent, so they must not clutter
     * search results (they belong in the detail page's Editions section instead).
     */
    @Test
    fun searchDoesNotReturnEditionDuplicates() = runBlocking {
        val games = repository.searchGames("Assassin's Creed Origins", limit = 10)

        assertTrue(games.any { it.name == "Assassin's Creed Origins" })
        assertTrue(
            "edition duplicates leaked into search: ${games.map { it.name }}",
            games.none { it.name.contains("Deluxe Edition", ignoreCase = true) },
        )
    }

    @Test
    fun relatedContentComesBackForGamesThatHaveIt() = runBlocking {
        // The Witcher 3 has both kinds of relation: expansions/DLCs via parent_game and
        // Deluxe/GOTY Editions via version_parent.
        val related = client.relatedGames(1942)

        assertTrue("expected related games", related.isNotEmpty())
        assertTrue("expected parent_game children", related.any { it.parentGame != null })
        assertTrue("expected version_parent editions", related.any { it.versionParent != null })
        assertTrue(related.none { it.id == 1942L })
    }

    @Test
    fun detailQueryUsesOnlyFieldsIgbdStillSupports() = runBlocking {
        // If IGDB drops one of the fields we request, this call fails with a 400 BadRequest.
        val witcher = repository.getGame(1942)

        assertNotNull(witcher)
        assertEquals("The Witcher 3: Wild Hunt", witcher!!.name)
        assertTrue(witcher.genres.isNotEmpty())
    }
}
