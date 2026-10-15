package com.ludicindex.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A source of games: a linked platform account or a subscription catalog (Xbox Game Pass, PS Plus).
 * `source_key` is a stable identity used to upsert without duplicating rows.
 */
@Entity(
    tableName = "library_sources",
    indices = [Index(value = ["source_key"], unique = true)],
)
data class LibrarySourceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "source_key")
    val sourceKey: String,
    @ColumnInfo(name = "kind")
    val kind: String,
    @ColumnInfo(name = "platform")
    val platform: String,
    @ColumnInfo(name = "label")
    val label: String,
    @ColumnInfo(name = "external_account_id")
    val externalAccountId: String? = null,
    @ColumnInfo(name = "region")
    val region: String? = null,
    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
