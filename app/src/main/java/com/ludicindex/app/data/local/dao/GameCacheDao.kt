package com.ludicindex.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ludicindex.app.data.local.entity.GameCacheEntity

@Dao
interface GameCacheDao {

    @Query("SELECT * FROM game_cache WHERE igdb_id = :igdbId LIMIT 1")
    suspend fun get(igdbId: Long): GameCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: GameCacheEntity)

    @Query("DELETE FROM game_cache")
    suspend fun clear()
}
