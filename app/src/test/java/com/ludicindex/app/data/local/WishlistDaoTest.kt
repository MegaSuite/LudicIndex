package com.ludicindex.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ludicindex.app.data.local.dao.TagDao
import com.ludicindex.app.data.local.dao.WishlistDao
import com.ludicindex.app.data.local.entity.EntryTagCrossRef
import com.ludicindex.app.data.local.entity.TagEntity
import com.ludicindex.app.data.local.entity.WishlistEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
class WishlistDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var wishlistDao: WishlistDao
    private lateinit var tagDao: TagDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        wishlistDao = db.wishlistDao()
        tagDao = db.tagDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertThenReadBackThroughTheTagRelation() = runTest {
        val entryId = wishlistDao.insert(entry(igdbId = 1942, name = "The Witcher 3"))
        val tagId = tagDao.insert(TagEntity(name = "PC", isPreset = true, colorArgb = 1))
        wishlistDao.insertTagCrossRef(EntryTagCrossRef(entryId, tagId))

        val loaded = wishlistDao.getByIgdbId(1942)

        assertNotNull(loaded)
        assertEquals("The Witcher 3", loaded!!.entry.name)
        assertEquals(listOf("PC"), loaded.tags.map { it.name })
    }

    @Test
    fun duplicateIgdbIdIsRejectedByTheUniqueIndex() = runBlocking {
        wishlistDao.insert(entry(igdbId = 7, name = "Celeste"))

        val failure = runCatching {
            wishlistDao.insert(entry(igdbId = 7, name = "Celeste"))
        }.exceptionOrNull()

        assertTrue(failure is SQLiteConstraintException)
    }

    @Test
    fun tagNamesAreCaseInsensitiveUnique() = runTest {
        val first = tagDao.insert(TagEntity(name = "PC", isPreset = false, colorArgb = 1))
        val second = tagDao.insert(TagEntity(name = "pc", isPreset = false, colorArgb = 2))

        assertEquals(-1L, second)
        assertEquals(first, tagDao.getByName("pC")?.id)
        assertEquals(1, tagDao.getAll().size)
    }

    @Test
    fun deletingAnEntryRemovesItsLinksButKeepsTheTags() = runTest {
        val entryId = wishlistDao.insert(entry(igdbId = 1, name = "A"))
        val tagId = tagDao.insert(TagEntity(name = "Backlog", isPreset = false, colorArgb = 1))
        wishlistDao.insertTagCrossRef(EntryTagCrossRef(entryId, tagId))

        wishlistDao.deleteById(entryId)

        assertEquals(0, wishlistDao.count())
        assertEquals(0, wishlistDao.countTags(entryId))
        assertEquals("Backlog", tagDao.getByName("Backlog")?.name)
    }

    @Test
    fun deletingATagRemovesLinksButKeepsTheEntry() = runTest {
        val entryId = wishlistDao.insert(entry(igdbId = 1, name = "A"))
        val tagId = tagDao.insert(TagEntity(name = "Backlog", isPreset = false, colorArgb = 1))
        wishlistDao.insertTagCrossRef(EntryTagCrossRef(entryId, tagId))

        tagDao.deleteById(tagId)

        assertEquals(1, wishlistDao.count())
        assertNull(tagDao.getByName("Backlog"))
    }

    @Test
    fun noteUpdatesPersist() = runTest {
        wishlistDao.insert(entry(igdbId = 1, name = "A"))

        wishlistDao.updateNote(1, "finished it", updatedAt = 600)

        val loaded = wishlistDao.getByIgdbId(1)!!.entry
        assertEquals("finished it", loaded.note)
        assertEquals(600L, loaded.updatedAt)
    }

    @Test
    fun observeEmitsNewestFirst() = runTest {
        wishlistDao.insert(entry(igdbId = 1, name = "Older", addedAt = 100))
        wishlistDao.insert(entry(igdbId = 2, name = "Newer", addedAt = 200))

        val rows = wishlistDao.observeAllWithTags().first()

        assertEquals(listOf("Newer", "Older"), rows.map { it.entry.name })
    }

    private fun entry(
        igdbId: Long,
        name: String,
        addedAt: Long = 1_000,
    ) = WishlistEntryEntity(
        igdbId = igdbId,
        name = name,
        addedAt = addedAt,
        updatedAt = addedAt,
    )
}
