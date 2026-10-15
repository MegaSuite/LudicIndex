package com.ludicindex.app.domain.model

/** How the games in a source were obtained. */
enum class LibrarySourceKind {
    /** Games the user owns on a linked platform account. */
    OWNED_ACCOUNT,

    /** Games playable while a subscription (Xbox Game Pass / PS Plus) is active. */
    SUBSCRIPTION;

    companion object {
        fun fromKey(key: String): LibrarySourceKind? = entries.firstOrNull { it.name == key }
    }
}

/**
 * One source of games: a linked platform account, or a subscription catalog.
 *
 * [key] is a stable, human-readable identity used to upsert the row without duplicating it
 * (e.g. `subscription:xbox:gamepass:US`). It exists because SQLite treats NULLs as distinct in a
 * unique index, which would let the same subscription be inserted twice.
 */
data class LibrarySource(
    val id: Long,
    val key: String,
    val kind: LibrarySourceKind,
    val platform: Platform,
    val label: String,
    val externalAccountId: String? = null,
    val region: String? = null,
    val enabled: Boolean = true,
    val lastSyncedAt: Long? = null,
    val createdAt: Long,
)
