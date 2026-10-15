package com.ludicindex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.ColumnInfo
import com.ludicindex.app.data.local.entity.EntryTagCrossRef
import com.ludicindex.app.data.local.entity.WishlistEntryEntity
import com.ludicindex.app.data.local.relation.WishlistEntryWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Transaction
    @Query("SELECT * FROM wishlist_entries ORDER BY added_at DESC")
    fun observeAllWithTags(): Flow<List<WishlistEntryWithTags>>

    @Transaction
    @Query("SELECT * FROM wishlist_entries ORDER BY added_at DESC")
    suspend fun getAllWithTags(): List<WishlistEntryWithTags>

    @Transaction
    @Query("SELECT * FROM wishlist_entries WHERE igdb_id = :igdbId LIMIT 1")
    suspend fun getByIgdbId(igdbId: Long): WishlistEntryWithTags?

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_entries WHERE igdb_id = :igdbId)")
    suspend fun exists(igdbId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_entries WHERE igdb_id = :igdbId)")
    fun observeExists(igdbId: Long): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM wishlist_entries")
    suspend fun count(): Int

    @Query("SELECT igdb_id, name FROM wishlist_entries")
    suspend fun allKeys(): List<WishlistKey>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: WishlistEntryEntity): Long

    @Update
    suspend fun update(entry: WishlistEntryEntity)

    @Query("UPDATE wishlist_entries SET note = :note, updated_at = :updatedAt WHERE igdb_id = :igdbId")
    suspend fun updateNote(igdbId: Long, note: String, updatedAt: Long)

    @Query("UPDATE wishlist_entries SET platform = :platform, updated_at = :updatedAt WHERE igdb_id = :igdbId")
    suspend fun updatePlatform(igdbId: Long, platform: String?, updatedAt: Long)

    @Query("DELETE FROM wishlist_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    @Query("DELETE FROM wishlist_entries WHERE igdb_id = :igdbId")
    suspend fun deleteByIgdbId(igdbId: Long)

    @Query("DELETE FROM wishlist_entries")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagCrossRef(ref: EntryTagCrossRef)

    @Query("DELETE FROM entry_tags WHERE entry_id = :entryId")
    suspend fun clearTags(entryId: Long)

    @Query("SELECT COUNT(*) FROM entry_tags WHERE entry_id = :entryId")
    suspend fun countTags(entryId: Long): Int
}

/** Minimal projection used by the library matcher. */
data class WishlistKey(
    @ColumnInfo(name = "igdb_id") val igdbId: Long,
    @ColumnInfo(name = "name") val name: String,
)
