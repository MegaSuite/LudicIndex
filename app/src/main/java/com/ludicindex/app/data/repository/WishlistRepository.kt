package com.ludicindex.app.data.repository

import com.ludicindex.app.data.backup.BackupItem
import com.ludicindex.app.data.backup.ImportStats
import com.ludicindex.app.data.backup.WishlistBackup
import com.ludicindex.app.data.local.PresetTags
import com.ludicindex.app.data.local.dao.TagDao
import com.ludicindex.app.data.local.dao.WishlistDao
import com.ludicindex.app.data.local.entity.EntryTagCrossRef
import com.ludicindex.app.data.local.entity.TagEntity
import com.ludicindex.app.data.local.entity.WishlistEntryEntity
import com.ludicindex.app.data.local.relation.WishlistEntryWithTags
import com.ludicindex.app.domain.model.EntryStatus
import com.ludicindex.app.domain.model.Game
import com.ludicindex.app.domain.model.Tag
import com.ludicindex.app.domain.model.WishlistEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WishlistRepository(
    private val wishlistDao: WishlistDao,
    private val tagDao: TagDao,
    private val now: () -> Long = System::currentTimeMillis,
) {

    fun observeEntries(): Flow<List<WishlistEntry>> =
        wishlistDao.observeAllWithTags().map { rows -> rows.map { it.toDomain() } }

    fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** @return true when a new tag was created; false when the name is blank or already used. */
    suspend fun createTag(name: String, colorArgb: Int = DEFAULT_TAG_COLOR): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (tagDao.getByName(trimmed) != null) return false
        return tagDao.insert(
            TagEntity(name = trimmed, isPreset = false, colorArgb = colorArgb),
        ) > 0
    }

    /** Removing a tag detaches it from every entry but leaves the entries themselves. */
    suspend fun deleteTag(tagId: Long) = tagDao.deleteById(tagId)

    suspend fun ensurePresetTags() {
        PresetTags.ALL.forEach { preset ->
            tagDao.insert(
                TagEntity(name = preset.name, isPreset = true, colorArgb = preset.colorArgb),
            )
        }
        // Drop presets that were shipped by an older build, but only the unused ones.
        val names = PresetTags.ALL.map { it.name }
        if (names.isNotEmpty()) {
            tagDao.deleteUnusedPresets(names)
        }
    }

    suspend fun isInWishlist(igdbId: Long): Boolean = wishlistDao.exists(igdbId)

    /** Drives the detail page's add/remove button. */
    fun observeIsInWishlist(igdbId: Long): Flow<Boolean> = wishlistDao.observeExists(igdbId)

    /** @return false when the game is already on the wishlist. */
    suspend fun addGame(game: Game, tagNames: List<String> = emptyList()): Boolean {
        if (wishlistDao.exists(game.igdbId)) return false
        val timestamp = now()
        val entryId = wishlistDao.insert(
            WishlistEntryEntity(
                igdbId = game.igdbId,
                name = game.name,
                coverImageId = game.coverImageId,
                releaseDate = game.releaseDate,
                rating = game.rating,
                summary = game.summary,
                status = EntryStatus.WANT,
                note = "",
                addedAt = timestamp,
                updatedAt = timestamp,
            ),
        )
        attachTags(entryId, tagNames)
        return true
    }

    suspend fun removeByIgdbId(igdbId: Long) = wishlistDao.deleteByIgdbId(igdbId)

    suspend fun setStatus(igdbId: Long, status: EntryStatus) =
        wishlistDao.updateStatus(igdbId, status, now())

    suspend fun setNote(igdbId: Long, note: String) = wishlistDao.updateNote(igdbId, note, now())

    suspend fun setTags(igdbId: Long, tagNames: List<String>) {
        val existing = wishlistDao.getByIgdbId(igdbId) ?: return
        wishlistDao.clearTags(existing.entry.id)
        attachTags(existing.entry.id, tagNames)
    }

    suspend fun clearWishlist() = wishlistDao.clear()

    suspend fun exportBackup(): WishlistBackup {
        val rows = wishlistDao.getAllWithTags()
        return WishlistBackup(
            exportedAt = now(),
            items = rows.map { row ->
                BackupItem(
                    igdbId = row.entry.igdbId,
                    name = row.entry.name,
                    coverImageId = row.entry.coverImageId,
                    releaseDate = row.entry.releaseDate,
                    rating = row.entry.rating,
                    summary = row.entry.summary,
                    status = row.entry.status.name,
                    note = row.entry.note,
                    addedAt = row.entry.addedAt,
                    tags = row.tags.map { it.name }.sorted(),
                )
            },
        )
    }

    /**
     * @param merge true: existing entries keep their place but take the backup's status/note/tags.
     *              false: existing entries are skipped.
     */
    suspend fun importBackup(backup: WishlistBackup, merge: Boolean = true): ImportStats {
        var added = 0
        var updated = 0
        var skipped = 0
        var tagsCreated = 0

        backup.items.forEach { item ->
            val existing = wishlistDao.getByIgdbId(item.igdbId)
            val status = parseStatus(item.status)

            if (existing == null) {
                val timestamp = now()
                val entryId = wishlistDao.insert(
                    WishlistEntryEntity(
                        igdbId = item.igdbId,
                        name = item.name,
                        coverImageId = item.coverImageId,
                        releaseDate = item.releaseDate,
                        rating = item.rating,
                        summary = item.summary,
                        status = status,
                        note = item.note,
                        addedAt = item.addedAt,
                        updatedAt = timestamp,
                    ),
                )
                tagsCreated += attachTags(entryId, item.tags)
                added++
            } else if (merge) {
                wishlistDao.update(
                    existing.entry.copy(
                        status = status,
                        note = item.note,
                        updatedAt = now(),
                    ),
                )
                wishlistDao.clearTags(existing.entry.id)
                tagsCreated += attachTags(existing.entry.id, item.tags)
                updated++
            } else {
                skipped++
            }
        }

        return ImportStats(
            added = added,
            updated = updated,
            skipped = skipped,
            tagsCreated = tagsCreated,
        )
    }

    private suspend fun attachTags(entryId: Long, tagNames: List<String>): Int {
        var created = 0
        tagNames.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .forEach { name ->
                val (tagId, wasCreated) = resolveTagId(name)
                if (wasCreated) created++
                if (tagId > 0) {
                    wishlistDao.insertTagCrossRef(EntryTagCrossRef(entryId = entryId, tagId = tagId))
                }
            }
        return created
    }

    /** @return the tag id and whether it had to be created. */
    private suspend fun resolveTagId(name: String): Pair<Long, Boolean> {
        tagDao.getByName(name)?.let { return it.id to false }
        val inserted = tagDao.insert(
            TagEntity(name = name, isPreset = false, colorArgb = DEFAULT_TAG_COLOR),
        )
        if (inserted > 0) return inserted to true
        // Insert was ignored because another writer won the race; read the winner back.
        return (tagDao.getByName(name)?.id ?: -1L) to false
    }

    private fun parseStatus(raw: String): EntryStatus =
        EntryStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: EntryStatus.WANT

    private companion object {
        const val DEFAULT_TAG_COLOR = 0xFF6C6C6C.toInt()
    }
}

internal fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    isPreset = isPreset,
    colorArgb = colorArgb,
)

internal fun WishlistEntryWithTags.toDomain(): WishlistEntry = WishlistEntry(
    id = entry.id,
    igdbId = entry.igdbId,
    name = entry.name,
    coverImageId = entry.coverImageId,
    releaseDate = entry.releaseDate,
    rating = entry.rating,
    summary = entry.summary,
    status = entry.status,
    note = entry.note,
    addedAt = entry.addedAt,
    updatedAt = entry.updatedAt,
    tags = tags.map { Tag(it.id, it.name, it.isPreset, it.colorArgb) },
)
