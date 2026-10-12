package com.ludicindex.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)],
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    // NOCASE keeps "PC" and "pc" from becoming two different tags.
    @ColumnInfo(name = "name", collate = ColumnInfo.NOCASE)
    val name: String,
    @ColumnInfo(name = "is_preset")
    val isPreset: Boolean = false,
    @ColumnInfo(name = "color_argb")
    val colorArgb: Int,
)
