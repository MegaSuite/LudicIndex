package com.ludicindex.app.data.local.relation

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.ludicindex.app.data.local.entity.LibraryItemEntity

/** A library item joined with the platform/label of the source it came from. */
data class LibraryItemRow(
    @Embedded val item: LibraryItemEntity,
    @ColumnInfo(name = "source_platform") val sourcePlatform: String,
    @ColumnInfo(name = "source_label") val sourceLabel: String,
)
