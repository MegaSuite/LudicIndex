package com.ludicindex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ludicindex.app.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY is_preset DESC, name ASC")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY is_preset DESC, name ASC")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Removes presets we no longer ship. Tags still attached to an entry are left alone, so this can
     * never discard something the user actually uses.
     */
    @Query(
        """
        DELETE FROM tags
        WHERE is_preset = 1
          AND name NOT IN (:activePresetNames)
          AND id NOT IN (SELECT tag_id FROM entry_tags)
        """,
    )
    suspend fun deleteUnusedPresets(activePresetNames: List<String>)
}
