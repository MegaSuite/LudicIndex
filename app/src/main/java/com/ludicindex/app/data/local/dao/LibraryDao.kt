package com.ludicindex.app.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ludicindex.app.data.local.entity.LibraryItemEntity
import com.ludicindex.app.data.local.entity.LibrarySourceEntity
import com.ludicindex.app.data.local.relation.LibraryItemRow
import kotlinx.coroutines.flow.Flow

/** Read/write access to the game-library tables (subscription catalogs today, owned accounts later). */
@Dao
interface LibraryDao {

    @Query("SELECT * FROM library_sources ORDER BY platform, id")
    fun observeSources(): Flow<List<LibrarySourceEntity>>

    @Query("SELECT * FROM library_sources WHERE source_key = :key LIMIT 1")
    suspend fun getSource(key: String): LibrarySourceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSource(source: LibrarySourceEntity): Long

    @Query("UPDATE library_sources SET last_synced_at = :timestamp WHERE id = :sourceId")
    suspend fun markSynced(sourceId: Long, timestamp: Long)

    @Query("UPDATE library_sources SET label = :label, region = :region WHERE id = :sourceId")
    suspend fun updateSourceMeta(sourceId: Long, label: String, region: String)

    /** Drops subscription sources that are no longer produced (e.g. old region-suffixed keys). */
    @Query("DELETE FROM library_sources WHERE kind = 'SUBSCRIPTION' AND source_key NOT IN (:keys)")
    suspend fun deleteSourcesNotIn(keys: List<String>)

    @Query(
        "SELECT library_items.*, library_sources.platform AS source_platform, " +
            "library_sources.label AS source_label " +
            "FROM library_items " +
            "INNER JOIN library_sources ON library_sources.id = library_items.source_id " +
            "ORDER BY library_items.title COLLATE NOCASE",
    )
    fun observeItemRows(): Flow<List<LibraryItemRow>>

    /** Wishlist entry ids that already sit in a subscription catalog (drives the XGP/PS+ badges). */
    @Query("SELECT DISTINCT igdb_id FROM library_items WHERE igdb_id IS NOT NULL")
    fun observeMatchedIgdbIds(): Flow<List<Long>>

    @Query("SELECT id, title FROM library_items WHERE igdb_id IS NULL")
    suspend fun unmatchedItems(): List<UnmatchedItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<LibraryItemEntity>)

    @Query("DELETE FROM library_items WHERE source_id = :sourceId")
    suspend fun deleteItemsForSource(sourceId: Long)

    @Query("UPDATE library_items SET igdb_id = :igdbId, match_kind = :matchKind WHERE id = :itemId")
    suspend fun setMatch(itemId: Long, igdbId: Long, matchKind: String)

    @Transaction
    suspend fun replaceItems(sourceId: Long, items: List<LibraryItemEntity>) {
        deleteItemsForSource(sourceId)
        insertItems(items)
    }
}

/** Minimal projection used when re-running the title matcher. */
data class UnmatchedItem(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "title") val title: String,
)
