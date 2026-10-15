package com.ludicindex.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ludicindex.app.data.local.dao.GameCacheDao
import com.ludicindex.app.data.local.dao.LibraryDao
import com.ludicindex.app.data.local.dao.TagDao
import com.ludicindex.app.data.local.dao.WishlistDao
import com.ludicindex.app.data.local.entity.EntryTagCrossRef
import com.ludicindex.app.data.local.entity.GameCacheEntity
import com.ludicindex.app.data.local.entity.LibraryItemEntity
import com.ludicindex.app.data.local.entity.LibrarySourceEntity
import com.ludicindex.app.data.local.entity.TagEntity
import com.ludicindex.app.data.local.entity.WishlistEntryEntity
import com.ludicindex.app.data.migration.MIGRATION_1_2
import com.ludicindex.app.data.migration.MIGRATION_2_3
import com.ludicindex.app.data.migration.MIGRATION_3_4

@Database(
    entities = [
        WishlistEntryEntity::class,
        TagEntity::class,
        EntryTagCrossRef::class,
        GameCacheEntity::class,
        LibrarySourceEntity::class,
        LibraryItemEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wishlistDao(): WishlistDao
    abstract fun tagDao(): TagDao
    abstract fun gameCacheDao(): GameCacheDao
    abstract fun libraryDao(): LibraryDao

    companion object {
        private const val NAME = "ludicindex.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
