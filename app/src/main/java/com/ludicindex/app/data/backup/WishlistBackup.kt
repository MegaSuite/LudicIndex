package com.ludicindex.app.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WishlistBackup(
    @SerialName("schema_version")
    val schemaVersion: Int = SCHEMA_VERSION,
    @SerialName("app")
    val app: String = APP_ID,
    @SerialName("exported_at")
    val exportedAt: Long,
    @SerialName("items")
    val items: List<BackupItem> = emptyList(),
) {
    companion object {
        const val SCHEMA_VERSION = 1
        const val APP_ID = "LudicIndex"
    }
}

@Serializable
data class BackupItem(
    @SerialName("igdb_id")
    val igdbId: Long,
    @SerialName("name")
    val name: String,
    @SerialName("cover_image_id")
    val coverImageId: String? = null,
    @SerialName("release_date")
    val releaseDate: Long? = null,
    @SerialName("rating")
    val rating: Double? = null,
    @SerialName("summary")
    val summary: String? = null,
    @SerialName("status")
    val status: String = "WANT",
    @SerialName("note")
    val note: String = "",
    @SerialName("added_at")
    val addedAt: Long,
    @SerialName("tags")
    val tags: List<String> = emptyList(),
)

data class ImportStats(
    val added: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val tagsCreated: Int = 0,
) {
    val total: Int get() = added + updated + skipped
}
