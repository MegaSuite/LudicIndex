package com.ludicindex.app.data.repository

import com.ludicindex.app.createTestDatabase
import com.ludicindex.app.data.local.AppDatabase
import com.ludicindex.app.data.local.dao.LibraryDao
import com.ludicindex.app.data.local.dao.WishlistDao
import com.ludicindex.app.data.local.entity.LibraryItemEntity
import com.ludicindex.app.data.local.entity.LibrarySourceEntity
import com.ludicindex.app.data.remote.catalog.CatalogException
import com.ludicindex.app.data.remote.catalog.CatalogGame
import com.ludicindex.app.data.remote.catalog.GamePassCatalog
import com.ludicindex.app.data.remote.catalog.PsPlusCatalog
import com.ludicindex.app.data.remote.catalog.PsPlusClient
import com.ludicindex.app.domain.model.Game
import com.ludicindex.app.domain.model.LibrarySourceKind
import com.ludicindex.app.domain.model.Platform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LibraryRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var libraryDao: LibraryDao
    private lateinit var wishlistDao: WishlistDao
    private var clock = 1_000L

    @Before
    fun setUp() {
        db = createTestDatabase()
        libraryDao = db.libraryDao()
        wishlistDao = db.wishlistDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun refreshStoresBothCatalogsAndMatchesTheWishlist() = runTest {
        WishlistRepository(wishlistDao, db.tagDao()) { clock }
            .addGame(Game(igdbId = 42, name = "Halo Infinite"))

        val repository = repository(
            gamePass = FakeGamePass(
                listOf(
                    CatalogGame("x1", "Halo Infinite"),
                    CatalogGame("x2", "Forza Horizon 5"),
                ),
            ),
            psPlus = FakePsPlus(
                mapOf(
                    PsPlusClient.CATEGORY_GAME_CATALOG to listOf(
                        CatalogGame("p1", "Halo Infinite", releaseDate = 1_600_000_000),
                        CatalogGame("p2", "Returnal"),
                    ),
                ),
            ),
        )

        val report = repository.refreshSubscriptions(market = "US", psLocale = "en-us")

        assertTrue(report.isComplete)
        assertEquals(4, report.gameCount)

        val rows = libraryDao.observeItemRows().first()
        assertEquals(4, rows.size)

        // "Halo Infinite" is on the wishlist, so both catalog copies carry its IGDB id.
        assertEquals(setOf(42L), repository.observeMatchedIgdbIds().first())

        // One Xbox source plus the three PS Plus catalogs; all marked as synced.
        val sources = libraryDao.observeSources().first()
        assertEquals(4, sources.size)
        assertTrue(sources.all { it.lastSyncedAt == clock })
    }

    @Test
    fun catalogTitlesNotOnTheWishlistStayUnmatched() = runTest {
        val repository = repository(
            gamePass = FakeGamePass(listOf(CatalogGame("x1", "Forza Horizon 5"))),
            psPlus = FakePsPlus(emptyMap()),
        )

        repository.refreshSubscriptions()

        val row = libraryDao.observeItemRows().first().single()
        assertNull(row.item.igdbId)
        assertTrue(libraryDao.observeMatchedIgdbIds().first().isEmpty())
    }

    @Test
    fun refreshingTwiceDoesNotDuplicateItemsOrSources() = runTest {
        val repository = repository(
            gamePass = FakeGamePass(listOf(CatalogGame("x1", "Alpha"))),
            psPlus = FakePsPlus(emptyMap()),
        )

        repository.refreshSubscriptions()
        repository.refreshSubscriptions()

        assertEquals(1, libraryDao.observeItemRows().first().size)
        // One Xbox source + three PS Plus catalogs; the second refresh must not add more.
        assertEquals(4, libraryDao.observeSources().first().size)
    }

    @Test
    fun aFailingPsCatalogIsReportedWhileXboxStillLoads() = runTest {
        val repository = repository(
            gamePass = FakeGamePass(listOf(CatalogGame("x1", "Alpha"))),
            psPlus = FailingPsPlus(),
        )

        val report = repository.refreshSubscriptions()

        assertFalse(report.isComplete)
        assertEquals(1, report.gameCount)
        assertTrue(report.failures.any { it.contains("PS Plus") })
        assertEquals(
            listOf("x1"),
            libraryDao.observeItemRows().first().map { it.item.externalId },
        )
    }

    @Test
    fun aLaterFailureKeepsPreviouslyDownloadedItems() = runTest {
        var failing = false
        val psPlus = object : PsPlusCatalog {
            override suspend fun fetchCatalog(categoryList: String, locale: String): List<CatalogGame> {
                if (failing) throw CatalogException.Network(IOException("boom"))
                return listOf(CatalogGame("p1", "Returnal"))
            }
        }
        val repository = repository(gamePass = FakeGamePass(emptyList()), psPlus = psPlus)

        repository.refreshSubscriptions()
        val before = libraryDao.observeItemRows().first().size

        failing = true
        val report = repository.refreshSubscriptions()

        assertFalse(report.isComplete)
        assertEquals(before, libraryDao.observeItemRows().first().size)
        assertTrue(before > 0)
    }

    @Test
    fun changingTheRegionReplacesTheCatalogInsteadOfDuplicatingIt() = runTest {
        val gamePass = object : GamePassCatalog {
            override suspend fun fetchCatalog(market: String): List<CatalogGame> =
                listOf(CatalogGame("x-$market", "Game $market"))
        }
        val repository = repository(gamePass = gamePass, psPlus = FakePsPlus(emptyMap()))

        repository.refreshSubscriptions(market = "US", psLocale = "en-us")
        repository.refreshSubscriptions(market = "HK", psLocale = "zh-hant-hk")

        // The Xbox source is reused (not duplicated) and its item replaced by the new region's.
        assertEquals(
            1,
            libraryDao.observeSources().first().count { it.platform == Platform.XBOX.name },
        )
        val items = libraryDao.observeItemRows().first()
        assertEquals(1, items.size)
        assertEquals("x-HK", items.single().item.externalId)
    }

    @Test
    fun staleRegionSuffixedSourcesFromEarlierBuildsAreCleanedUp() = runTest {
        // A source left behind by an older build whose key still included the region.
        val staleId = libraryDao.insertSource(
            LibrarySourceEntity(
                sourceKey = "subscription:xbox:gamepass:US",
                kind = LibrarySourceKind.SUBSCRIPTION.name,
                platform = Platform.XBOX.name,
                label = "Xbox Game Pass",
                region = "US",
                createdAt = 1L,
            ),
        )
        libraryDao.insertItems(
            listOf(
                LibraryItemEntity(
                    sourceId = staleId,
                    externalId = "old",
                    title = "Old Game",
                    addedAt = 1L,
                ),
            ),
        )

        val repository = repository(
            gamePass = FakeGamePass(listOf(CatalogGame("x1", "Alpha"))),
            psPlus = FakePsPlus(emptyMap()),
        )
        repository.refreshSubscriptions(market = "US", psLocale = "en-us")

        val keys = libraryDao.observeSources().first().map { it.sourceKey }
        assertFalse(keys.contains("subscription:xbox:gamepass:US"))
        assertTrue(libraryDao.observeItemRows().first().none { it.item.externalId == "old" })
    }

    private fun repository(
        gamePass: GamePassCatalog,
        psPlus: PsPlusCatalog,
    ) = LibraryRepository(libraryDao, wishlistDao, gamePass, psPlus) { clock }

    private class FakeGamePass(private val games: List<CatalogGame>) : GamePassCatalog {
        override suspend fun fetchCatalog(market: String): List<CatalogGame> = games
    }

    private class FakePsPlus(
        private val byCategory: Map<String, List<CatalogGame>>,
    ) : PsPlusCatalog {
        override suspend fun fetchCatalog(categoryList: String, locale: String): List<CatalogGame> =
            byCategory[categoryList] ?: emptyList()
    }

    private class FailingPsPlus : PsPlusCatalog {
        override suspend fun fetchCatalog(categoryList: String, locale: String): List<CatalogGame> =
            throw CatalogException.Network(IOException("boom"))
    }
}
