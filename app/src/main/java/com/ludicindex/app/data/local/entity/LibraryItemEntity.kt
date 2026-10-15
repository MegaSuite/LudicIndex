package com.ludicindex.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One game inside a [LibrarySourceEntity]. `platforms` is a comma-separated list of display labels
 * (PS4/PS5, Xbox Series X|S, PC…) because the catalogs expose different device vocabularies.
 */
@Entity(
    tableName = "library_items",
    foreignKeys = [
        ForeignKey(
            entity = LibrarySourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["source_id"]),
        Index(value = ["igdb_id"]),
        Index(value = ["source_id", "external_id"], unique = true),
    ],
)
data class LibraryItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "source_id")
    val sourceId: Long,
    @ColumnInfo(name = "external_id")
    val externalId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,
    @ColumnInfo(name = "platforms")
    val platforms: String = "",
    @ColumnInfo(name = "release_date")
    val releaseDate: Long? = null,
    @ColumnInfo(name = "igdb_id")
    val igdbId: Long? = null,
    @ColumnInfo(name = "match_kind")
    val matchKind: String = "NONE",
    @ColumnInfo(name = "owned_at")
    val ownedAt: Long? = null,
    @ColumnInfo(name = "leaving_at")
    val leavingAt: Long? = null,
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
)
