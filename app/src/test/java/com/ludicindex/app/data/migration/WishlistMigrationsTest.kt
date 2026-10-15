package com.ludicindex.app.data.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises `MIGRATION_2_3` (drop the wishlist `status` column) on a real SQLite database, with
 * foreign keys enabled — the risky part, because `entry_tags` cascades from `wishlist_entries`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WishlistMigrationsTest {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(1) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    },
                )
                .build(),
        )
        db = helper.writableDatabase
        db.execSQL("PRAGMA foreign_keys = ON")
        createV2Schema()
    }

    @After
    fun tearDown() = helper.close()

    @Test
    fun dropsStatusAndKeepsEntriesAndTagLinks() {
        seed()

        MIGRATION_2_3.migrate(db)

        assertEquals(1, count("wishlist_entries"))
        assertEquals(1, count("entry_tags"))
        db.query("SELECT name, note FROM wishlist_entries").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("The Witcher 3", cursor.getString(0))
            assertEquals("act 2", cursor.getString(1))
        }
        assertFalse(columns("wishlist_entries").contains("status"))
    }

    @Test
    fun tagLinksStillCascadeAfterTheParentTableIsRebuilt() {
        seed()

        MIGRATION_2_3.migrate(db)
        db.execSQL("DELETE FROM wishlist_entries WHERE id = 1")

        assertEquals(0, count("entry_tags"))
    }

    @Test
    fun migratingThroughToV4AddsANullablePlatformColumn() {
        seed()

        MIGRATION_2_3.migrate(db)
        MIGRATION_3_4.migrate(db)

        assertTrue(columns("wishlist_entries").contains("platform"))
        db.query("SELECT platform FROM wishlist_entries WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertTrue(cursor.isNull(0))
        }
    }

    private fun seed() {
        db.execSQL(
            "INSERT INTO wishlist_entries (id, igdb_id, name, status, note, added_at, updated_at) " +
                "VALUES (1, 1942, 'The Witcher 3', 'PLAYING', 'act 2', 100, 200)",
        )
        db.execSQL("INSERT INTO tags (id, name, is_preset, color_argb) VALUES (1, 'PC', 1, 1)")
        db.execSQL("INSERT INTO entry_tags (entry_id, tag_id) VALUES (1, 1)")
    }

    private fun createV2Schema() {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `wishlist_entries` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `igdb_id` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, `cover_image_id` TEXT, `release_date` INTEGER, `rating` REAL, " +
                "`summary` TEXT, `status` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                "`added_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_wishlist_entries_igdb_id` " +
                "ON `wishlist_entries` (`igdb_id`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL COLLATE NOCASE, `is_preset` INTEGER NOT NULL, " +
                "`color_argb` INTEGER NOT NULL)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `entry_tags` (`entry_id` INTEGER NOT NULL, " +
                "`tag_id` INTEGER NOT NULL, PRIMARY KEY(`entry_id`, `tag_id`), " +
                "FOREIGN KEY(`entry_id`) REFERENCES `wishlist_entries`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`tag_id`) REFERENCES `tags`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_entry_tags_tag_id` ON `entry_tags` (`tag_id`)")
    }

    private fun count(table: String): Int = db.query("SELECT COUNT(*) FROM `$table`").use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    private fun columns(table: String): List<String> =
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
        }
}
