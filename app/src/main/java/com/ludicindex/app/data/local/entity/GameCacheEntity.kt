package com.ludicindex.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Raw IGDB detail payload, kept so the detail screen still works offline. */
@Entity(tableName = "game_cache")
data class GameCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "igdb_id")
    val igdbId: Long,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "fetched_at")
    val fetchedAt: Long,
)
