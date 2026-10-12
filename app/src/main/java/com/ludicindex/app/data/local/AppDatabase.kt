package com.ludicindex.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ludicindex.app.data.local.dao.GameCacheDao
import com.ludicindex.app.data.local.dao.TagDao
import com.ludicindex.app.data.local.dao.WishlistDao
import com.ludicindex.app.data.local.entity.EntryTagCrossRef
import com.ludicindex.app.data.local.entity.GameCacheEntity
import com.ludicindex.app.data.local.entity.TagEntity
import com.ludicindex.app.data.local.entity.WishlistEntryEntity

@Database(
    entities = [
        WishlistEntryEntity::class,
        TagEntity::class,
        EntryTagCrossRef::class,
        GameCacheEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wishlistDao(): WishlistDao
    abstract fun tagDao(): TagDao
    abstract fun gameCacheDao(): GameCacheDao

    companion object {
        private const val NAME = "ludicindex.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, NAME).build()
    }
}
