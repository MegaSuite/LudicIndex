package com.ludicindex.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "entry_tags",
    primaryKeys = ["entry_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = WishlistEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["tag_id"])],
)
data class EntryTagCrossRef(
    @ColumnInfo(name = "entry_id")
    val entryId: Long,
    @ColumnInfo(name = "tag_id")
    val tagId: Long,
)
