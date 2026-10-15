package com.ludicindex.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wishlist_entries",
    indices = [Index(value = ["igdb_id"], unique = true)],
)
data class WishlistEntryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "igdb_id")
    val igdbId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "cover_image_id")
    val coverImageId: String? = null,
    @ColumnInfo(name = "release_date")
    val releaseDate: Long? = null,
    @ColumnInfo(name = "rating")
    val rating: Double? = null,
    @ColumnInfo(name = "summary")
    val summary: String? = null,
    @ColumnInfo(name = "platform")
    val platform: String? = null,
    @ColumnInfo(name = "note")
    val note: String = "",
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
