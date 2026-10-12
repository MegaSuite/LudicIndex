package com.ludicindex.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ludicindex.app.data.local.AppDatabase
import com.ludicindex.app.data.local.PresetTags
import com.ludicindex.app.data.local.dao.TagDao
import com.ludicindex.app.data.local.dao.WishlistDao
import com.ludicindex.app.data.backup.BackupItem
import com.ludicindex.app.data.backup.WishlistBackup
import com.ludicindex.app.data.local.entity.TagEntity
import com.ludicindex.app.domain.model.EntryStatus
import com.ludicindex.app.domain.model.Game
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class WishlistRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var wishlistDao: WishlistDao
    private lateinit var tagDao: TagDao
    private lateinit var repository: WishlistRepository
    private var clock = 1_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        wishlistDao = db.wishlistDao()
        tagDao = db.tagDao()
        repository = WishlistRepository(wishlistDao, tagDao) { clock }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addGameStoresTheEntryAndItsTags() = runTest {
        val added = repository.addGame(
            Game(igdbId = 1942, name = "The Witcher 3", releaseDate = 1_431_993_600, rating = 92.75),
            tagNames = listOf("PC", "Backlog", "pc"),
        )

        assertTrue(added)
        val entry = repository.observeEntries().first().single()
        assertEquals("The Witcher 3", entry.name)
        assertEquals(EntryStatus.WANT, entry.status)
        assertEquals(1_000L, entry.addedAt)
        // "pc" collapses into "PC" thanks to case-insensitive tag names.
        assertEquals(listOf("Backlog", "PC"), entry.tags.map { it.name }.sorted())
    }

    @Test
    fun addingTheSameGameTwiceIsRejected() = runTest {
        assertTrue(repository.addGame(Game(igdbId = 7, name = "Celeste")))
        assertFalse(repository.addGame(Game(igdbId = 7, name = "Celeste")))

        assertEquals(1, wishlistDao.count())
    }

    @Test
    fun ensurePresetTagsIsIdempotent() = runTest {
        repository.ensurePresetTags()
        repository.ensurePresetTags()

        assertEquals(PresetTags.ALL.size, tagDao.getAll().size)
    }

    @Test
    fun ensurePresetTagsRemovesUnusedPresetsFromOlderBuilds() = runTest {
        // A preset an older build used to ship, with nothing attached to it.
        tagDao.insert(TagEntity(name = "Backlog", isPreset = true, colorArgb = 1))

        repository.ensurePresetTags()

        assertNull(tagDao.getByName("Backlog"))
        assertEquals(PresetTags.ALL.size, tagDao.getAll().size)
    }

    @Test
    fun ensurePresetTagsKeepsStalePresetsThatAreStillInUse() = runTest {
        tagDao.insert(TagEntity(name = "Backlog", isPreset = true, colorArgb = 1))
        repository.addGame(Game(igdbId = 7, name = "Celeste"), tagNames = listOf("Backlog"))

        repository.ensurePresetTags()

        // Cleanup must never drop a tag somebody is actually using.
        assertNotNull(tagDao.getByName("Backlog"))
    }

    @Test
    fun setTagsReplacesTheExistingSelection() = runTest {
        repository.addGame(Game(igdbId = 7, name = "Celeste"), tagNames = listOf("PC"))
        clock = 2_000

        repository.setTags(7, listOf("Switch"))

        val entry = repository.observeEntries().first().single()
        assertEquals(listOf("Switch"), entry.tags.map { it.name })
    }

    @Test
    fun exportImportRoundTripPreservesEverything() = runTest {
        repository.addGame(
            Game(igdbId = 1942, name = "The Witcher 3", rating = 92.75),
            tagNames = listOf("PC", "Backlog"),
        )
        repository.setStatus(1942, EntryStatus.PLAYING)
        repository.setNote(1942, "act 2")

        val backup = repository.exportBackup()
        repository.clearWishlist()
        assertEquals(0, wishlistDao.count())

        val stats = repository.importBackup(backup)

        assertEquals(1, stats.added)
        assertEquals(0, stats.skipped)
        val entry = repository.observeEntries().first().single()
        assertEquals(EntryStatus.PLAYING, entry.status)
        assertEquals("act 2", entry.note)
        assertEquals(listOf("Backlog", "PC"), entry.tags.map { it.name }.sorted())
    }

    @Test
    fun importWithMergeUpdatesExistingEntries() = runTest {
        repository.addGame(Game(igdbId = 7, name = "Celeste"), tagNames = listOf("PC"))
        val backup = WishlistBackup(
            exportedAt = 5_000,
            items = listOf(
                BackupItem(
                    igdbId = 7,
                    name = "Celeste",
                    status = "PLAYED",
                    note = "100%",
                    addedAt = 1,
                    tags = listOf("Switch"),
                ),
            ),
        )

        val stats = repository.importBackup(backup, merge = true)

        assertEquals(0, stats.added)
        assertEquals(1, stats.updated)
        val entry = repository.observeEntries().first().single()
        assertEquals(EntryStatus.PLAYED, entry.status)
        assertEquals("100%", entry.note)
        assertEquals(listOf("Switch"), entry.tags.map { it.name })
    }

    @Test
    fun importWithoutMergeSkipsExistingEntries() = runTest {
        repository.addGame(Game(igdbId = 7, name = "Celeste"))
        val backup = WishlistBackup(
            exportedAt = 5_000,
            items = listOf(
                BackupItem(
                    igdbId = 7,
                    name = "Celeste",
                    addedAt = 1,
                ),
            ),
        )

        val stats = repository.importBackup(backup, merge = false)

        assertEquals(1, stats.skipped)
        assertEquals(0, stats.updated)
    }

    @Test
    fun removingByIgdbIdEmptiesTheWishlist() = runTest {
        repository.addGame(Game(igdbId = 7, name = "Celeste"))

        repository.removeByIgdbId(7)

        assertTrue(repository.observeEntries().first().isEmpty())
        assertFalse(repository.isInWishlist(7))
    }

    @Test
    fun observeTagsExposesPresetsAfterSeeding() = runTest {
        repository.ensurePresetTags()

        val tags = repository.observeTags().first()

        assertEquals(PresetTags.ALL.size, tags.size)
        assertTrue(tags.all { it.isPreset })
        assertTrue(tags.any { it.name == "PS5" })
    }

    @Test
    fun createTagRejectsBlanksAndDuplicates() = runTest {
        repository.ensurePresetTags()

        assertFalse(repository.createTag("   "))
        assertFalse(repository.createTag("PS5"))
        assertTrue(repository.createTag("限时免费"))
        assertTrue(repository.observeTags().first().any { it.name == "限时免费" })
    }

    @Test
    fun createTagIgnoresCaseWhenCheckingDuplicates() = runTest {
        repository.ensurePresetTags()

        assertFalse(repository.createTag("ps5"))
    }

    @Test
    fun deleteTagDetachesItFromEntriesButKeepsThem() = runTest {
        repository.addGame(Game(igdbId = 7, name = "Celeste"), tagNames = listOf("Backlog"))
        val tagId = repository.observeTags().first().first { it.name == "Backlog" }.id

        repository.deleteTag(tagId)

        val entries = repository.observeEntries().first()
        assertEquals(1, entries.size)
        assertTrue(entries.single().tags.isEmpty())
        assertTrue(repository.observeTags().first().isEmpty())
    }
}
