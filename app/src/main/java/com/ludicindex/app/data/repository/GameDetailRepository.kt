package com.ludicindex.app.data.repository

import com.ludicindex.app.data.local.dao.GameCacheDao
import com.ludicindex.app.data.local.entity.GameCacheEntity
import com.ludicindex.app.data.remote.IgdbClient
import com.ludicindex.app.data.remote.dto.GameDto
import com.ludicindex.app.domain.model.GameDetail
import com.ludicindex.app.domain.model.GameVideo
import com.ludicindex.app.domain.model.RelatedGame
import com.ludicindex.app.domain.model.RelatedGroup
import com.ludicindex.app.domain.model.SimilarGame
import com.ludicindex.app.domain.model.WebsiteLink
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Detail reads go through a raw-JSON cache so the screen still renders offline. The cache holds the
 * payload IGDB returned, which means a field we forgot to map is still recoverable later.
 */
class GameDetailRepository(
    private val client: IgdbClient,
    private val cacheDao: GameCacheDao,
    private val json: Json,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Related games come from a second endpoint, so the cache stores both payloads together. */
    @Serializable
    internal data class CachedDetailPayload(
        val game: List<GameDto> = emptyList(),
        val related: List<GameDto> = emptyList(),
    )

    suspend fun cached(igdbId: Long): GameDetail? =
        cacheDao.get(igdbId)?.let { decodeCached(it.payloadJson) }

    suspend fun fetch(igdbId: Long): GameDetail? {
        val gameJson = client.gameDetailPayload(igdbId)
        val games = decodeGames(gameJson)
        val base = games.firstOrNull() ?: return null

        // A failure here must not take down the whole detail page.
        val related = runCatching { client.relatedGames(igdbId) }.getOrDefault(emptyList())

        cacheDao.put(
            GameCacheEntity(
                igdbId = igdbId,
                payloadJson = json.encodeToString(
                    CachedDetailPayload.serializer(),
                    CachedDetailPayload(game = games, related = related),
                ),
                fetchedAt = now() / 1000,
            ),
        )
        return base.toDetail(related)
    }

    private fun decodeGames(raw: String): List<GameDto> =
        runCatching { json.decodeFromString(ListSerializer(GameDto.serializer()), raw) }
            .getOrDefault(emptyList())

    private fun decodeCached(raw: String): GameDetail? {
        runCatching { json.decodeFromString(CachedDetailPayload.serializer(), raw) }
            .getOrNull()
            ?.let { payload -> payload.game.firstOrNull()?.let { return it.toDetail(payload.related) } }

        // Cache written before related content was stored.
        return decodeGames(raw).firstOrNull()?.toDetail()
    }
}

/**
 * `game_type` → the label IGDB's website uses for that bucket, in display order.
 * 3 (Bundle) is absent on purpose: bundles are surfaced as "包含于合辑", which is a reverse lookup.
 */
private val RELATED_TYPE_LABELS: List<Pair<Int, String>> = listOf(
    2 to "资料片",
    1 to "DLC",
    4 to "独立资料片",
    14 to "更新",
    8 to "重制版",
    9 to "复刻版",
    10 to "强化版",
    11 to "移植版",
    12 to "衍生作品",
    5 to "Mod",
    6 to "章节",
    7 to "赛季",
    13 to "组合包",
)

internal fun buildRelatedGroups(
    related: List<GameDto>,
    bundledIn: List<GameDto>,
): List<RelatedGroup> {
    val groups = mutableListOf<RelatedGroup>()

    // Editions are the ones linked the other way round (version_parent rather than parent_game).
    val editions = related.filter { it.versionParent != null }
    val children = related.filter { it.parentGame != null }

    val byType = children.groupBy { it.gameType }
    RELATED_TYPE_LABELS.forEach { (type, label) ->
        byType[type]?.takeIf { it.isNotEmpty() }?.let { games ->
            groups += RelatedGroup(title = label, games = games.map { it.toRelatedGame() })
        }
    }

    // Anything IGDB adds a new game_type for still shows up rather than silently disappearing.
    val knownTypes = RELATED_TYPE_LABELS.map { it.first }.toSet()
    byType.filterKeys { it != null && it !in knownTypes }
        .forEach { (type, games) ->
            groups += RelatedGroup(title = "其他（类型 $type）", games = games.map { it.toRelatedGame() })
        }

    if (bundledIn.isNotEmpty()) {
        groups += RelatedGroup(title = "包含于合辑", games = bundledIn.map { it.toRelatedGame() })
    }
    if (editions.isNotEmpty()) {
        groups += RelatedGroup(title = "版本", games = editions.map { it.toRelatedGame() })
    }

    return groups
}

private fun GameDto.toRelatedGame(): RelatedGame = RelatedGame(
    igdbId = id,
    name = name,
    coverImageId = cover?.imageId,
    releaseDate = firstReleaseDate,
)

internal fun GameDto.toDetail(related: List<GameDto> = emptyList()): GameDetail = GameDetail(
    igdbId = id,
    name = name,
    summary = summary,
    storyline = storyline,
    releaseDate = firstReleaseDate,
    gameType = gameType,
    rating = rating,
    ratingCount = ratingCount,
    aggregatedRating = aggregatedRating,
    aggregatedRatingCount = aggregatedRatingCount,
    totalRating = totalRating,
    totalRatingCount = totalRatingCount,
    hypes = hypes,
    follows = follows,
    coverImageId = cover?.imageId,
    artworkImageIds = artworks.mapNotNull { it.imageId },
    screenshotImageIds = screenshots.mapNotNull { it.imageId },
    videos = videos.mapNotNull { video ->
        video.videoId?.let { GameVideo(name = video.name, videoId = it) }
    },
    genres = genres.map { it.name },
    themes = themes.map { it.name },
    platforms = platforms.map { it.name },
    gameModes = gameModes.map { it.name },
    playerPerspectives = playerPerspectives.map { it.name },
    engines = gameEngines.map { it.name },
    developers = involvedCompanies.filter { it.developer }.mapNotNull { it.company?.name }.distinct(),
    publishers = involvedCompanies.filter { it.publisher }.mapNotNull { it.company?.name }.distinct(),
    portingCompanies = involvedCompanies.filter { it.porting }.mapNotNull { it.company?.name }.distinct(),
    collection = collection?.name,
    franchise = franchise?.name,
    similarGames = similarGames.map {
        SimilarGame(igdbId = it.id, name = it.name, coverImageId = it.cover?.imageId)
    },
    relatedGroups = buildRelatedGroups(related, bundles),
    websites = websites.map { WebsiteLink(url = it.url, type = it.type) },
    languages = languageSupports.mapNotNull { it.language?.name }.distinct().sorted(),
)
