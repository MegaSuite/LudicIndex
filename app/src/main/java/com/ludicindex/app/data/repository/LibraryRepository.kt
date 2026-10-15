package com.ludicindex.app.data.repository

import com.ludicindex.app.data.local.dao.LibraryDao
import com.ludicindex.app.data.local.dao.WishlistDao
import com.ludicindex.app.data.local.entity.LibraryItemEntity
import com.ludicindex.app.data.local.entity.LibrarySourceEntity
import com.ludicindex.app.data.remote.catalog.CatalogException
import com.ludicindex.app.data.remote.catalog.CatalogGame
import com.ludicindex.app.data.remote.catalog.GamePassCatalog
import com.ludicindex.app.data.remote.catalog.GamePassClient
import com.ludicindex.app.data.remote.catalog.PsPlusCatalog
import com.ludicindex.app.data.remote.catalog.PsPlusClient
import com.ludicindex.app.domain.match.TitleMatcher
import com.ludicindex.app.domain.model.LibraryGame
import com.ludicindex.app.domain.model.LibraryItem
import com.ludicindex.app.domain.model.LibrarySource
import com.ludicindex.app.domain.model.LibrarySourceKind
import com.ludicindex.app.domain.model.MatchKind
import com.ludicindex.app.domain.model.Platform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What a refresh managed to do, so the UI can report partial failures instead of going silent. */
data class RefreshReport(
    val gameCount: Int,
    val failures: List<String>,
) {
    val isComplete: Boolean get() = failures.isEmpty()
}

/**
 * Owns the game-library tables. Today the only sources are subscription catalogs (Xbox Game Pass,
 * PS Plus); the model is generic so owned platform accounts can be added later.
 *
 * Catalogs are matched to the local wishlist by normalized title — no extra network calls — and the
 * matches are stored on `library_items.igdb_id`, ready for the wishlist badges.
 */
class LibraryRepository(
    private val libraryDao: LibraryDao,
    private val wishlistDao: WishlistDao,
    private val gamePass: GamePassCatalog,
    private val psPlus: PsPlusCatalog,
    private val now: () -> Long = System::currentTimeMillis,
) {

    fun observeSources(): Flow<List<LibrarySource>> =
        libraryDao.observeSources().map { rows -> rows.map { it.toDomain() } }

    fun observeGames(): Flow<List<LibraryGame>> =
        libraryDao.observeItemRows().map { rows ->
            rows.map { row ->
                LibraryGame(
                    item = row.item.toDomain(),
                    platform = Platform.fromKey(row.sourcePlatform) ?: Platform.XBOX,
                    sourceLabel = row.sourceLabel,
                )
            }
        }

    /** Wishlist games that are already playable through a subscription. */
    fun observeMatchedIgdbIds(): Flow<Set<Long>> =
        libraryDao.observeMatchedIgdbIds().map { it.toSet() }

    /**
     * Drops sources this build no longer produces. Runs on tab open so duplicates left behind by
     * older builds (whose keys included the region) disappear without a manual refresh.
     */
    suspend fun pruneStaleSources() {
        libraryDao.deleteSourcesNotIn(ACTIVE_KEYS)
    }

    /**
     * Refreshes every subscription catalog. A single failing source does not abort the others: the
     * previous items for that source are kept and the failure is reported.
     */
    suspend fun refreshSubscriptions(
        market: String = GamePassClient.DEFAULT_MARKET,
        psLocale: String = PsPlusClient.DEFAULT_LOCALE,
    ): RefreshReport {
        val failures = mutableListOf<String>()
        var total = 0

        val xbox = sync(
            key = XBOX_KEY,
            platform = Platform.XBOX,
            label = "Xbox Game Pass",
            region = market.uppercase(),
        ) { gamePass.fetchCatalog(market) }
        total += xbox.count
        xbox.error?.let { failures += "Xbox Game Pass：${it.message}" }

        PS_CATALOGS.forEach { (key, label, category) ->
            val result = sync(
                key = psKey(key),
                platform = Platform.PSN,
                label = label,
                region = psLocale,
            ) { psPlus.fetchCatalog(category, psLocale) }
            total += result.count
            result.error?.let { failures += "$label：${it.message}" }
        }

        // Sources keys are region-independent and stable, so changing the region on an existing
        // source *replaces* its items. Old region-suffixed sources from earlier builds are dropped
        // here — otherwise every region the user tried would leave a second copy of the catalog.
        libraryDao.deleteSourcesNotIn(ACTIVE_KEYS)

        rematchWishlist()
        return RefreshReport(gameCount = total, failures = failures)
    }

    private suspend fun sync(
        key: String,
        platform: Platform,
        label: String,
        region: String,
        fetch: suspend () -> List<CatalogGame>,
    ): SyncOutcome {
        val sourceId = ensureSource(key, platform, label, region)
        return try {
            val games = fetch()
            val timestamp = now()
            libraryDao.replaceItems(sourceId, games.map { it.toEntity(sourceId, timestamp) })
            libraryDao.markSynced(sourceId, timestamp)
            SyncOutcome(games.size, null)
        } catch (e: CatalogException) {
            // Keep whatever was downloaded before rather than blanking the tab on a hiccup.
            SyncOutcome(0, e)
        }
    }

    private suspend fun ensureSource(
        key: String,
        platform: Platform,
        label: String,
        region: String,
    ): Long {
        libraryDao.getSource(key)?.let { existing ->
            if (existing.label != label || existing.region != region) {
                libraryDao.updateSourceMeta(existing.id, label, region)
            }
            return existing.id
        }
        val inserted = libraryDao.insertSource(
            LibrarySourceEntity(
                sourceKey = key,
                kind = LibrarySourceKind.SUBSCRIPTION.name,
                platform = platform.name,
                label = label,
                region = region,
                enabled = true,
                lastSyncedAt = null,
                createdAt = now(),
            ),
        )
        if (inserted > 0) return inserted
        return libraryDao.getSource(key)?.id ?: error("无法创建游戏库来源：$key")
    }

    /** Re-derives `igdb_id` for every unmatched catalog item from the current wishlist. */
    private suspend fun rematchWishlist() {
        val index = TitleMatcher.unambiguousIndex(
            wishlistDao.allKeys().map { it.igdbId to it.name },
        )
        if (index.isEmpty()) return
        libraryDao.unmatchedItems().forEach { item ->
            val igdbId = index[TitleMatcher.matchKey(item.title)] ?: return@forEach
            libraryDao.setMatch(item.id, igdbId, MatchKind.TITLE.name)
        }
    }

    private fun CatalogGame.toEntity(sourceId: Long, timestamp: Long) = LibraryItemEntity(
        sourceId = sourceId,
        externalId = externalId,
        title = title,
        coverUrl = coverUrl,
        platforms = platforms.joinToString(","),
        releaseDate = releaseDate,
        igdbId = null,
        matchKind = MatchKind.NONE.name,
        ownedAt = null,
        leavingAt = leavingAt,
        addedAt = timestamp,
    )

    private fun LibrarySourceEntity.toDomain() = LibrarySource(
        id = id,
        key = sourceKey,
        kind = LibrarySourceKind.fromKey(kind) ?: LibrarySourceKind.SUBSCRIPTION,
        platform = Platform.fromKey(platform) ?: Platform.XBOX,
        label = label,
        externalAccountId = externalAccountId,
        region = region,
        enabled = enabled,
        lastSyncedAt = lastSyncedAt,
        createdAt = createdAt,
    )

    private fun LibraryItemEntity.toDomain() = LibraryItem(
        id = id,
        sourceId = sourceId,
        externalId = externalId,
        title = title,
        coverUrl = coverUrl,
        platforms = platforms.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        releaseDate = releaseDate,
        igdbId = igdbId,
        matchKind = MatchKind.fromKey(matchKind),
        ownedAt = ownedAt,
        leavingAt = leavingAt,
        addedAt = addedAt,
    )

    private class SyncOutcome(val count: Int, val error: CatalogException?)

    companion object {
        /** Region-independent, so switching region updates this source instead of adding one. */
        private const val XBOX_KEY = "subscription:xbox:gamepass"

        private fun psKey(category: String) = "subscription:psn:$category"

        private val PS_CATALOGS = listOf(
            Triple("game-catalog", "PS Plus 游戏目录", PsPlusClient.CATEGORY_GAME_CATALOG),
            Triple("classics", "PS Plus 经典目录", PsPlusClient.CATEGORY_CLASSICS),
            Triple("monthly", "PS Plus 每月游戏", PsPlusClient.CATEGORY_MONTHLY),
        )

        /** Every source key this build produces; anything else is stale and removed on refresh. */
        private val ACTIVE_KEYS: List<String> = listOf(XBOX_KEY) + PS_CATALOGS.map { psKey(it.first) }
    }
}
