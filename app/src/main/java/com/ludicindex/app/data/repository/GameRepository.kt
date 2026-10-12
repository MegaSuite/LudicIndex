package com.ludicindex.app.data.repository

import com.ludicindex.app.data.remote.IgdbClient
import com.ludicindex.app.data.remote.dto.GameDto
import com.ludicindex.app.data.remote.dto.InvolvedCompanyDto
import com.ludicindex.app.domain.model.Game

class GameRepository(private val client: IgdbClient) {

    /**
     * "Popular right now" = IGDB PopScore visits (popularity_type 1). The games endpoint no longer
     * exposes a popularity field, so we resolve game ids first and then fetch the game rows.
     */
    suspend fun popularRightNow(limit: Int = DEFAULT_LIMIT): List<Game> {
        val primitives = runCatching {
            client.popularityPrimitives(IgdbClient.POPULARITY_TYPE_VISITS, limit)
        }.getOrNull().orEmpty()

        if (primitives.isEmpty()) {
            return client.hypedGames(limit).map { it.toDomain() }
        }

        val orderedIds = primitives.map { it.gameId }.distinct()
        val byId = client.gamesByIds(orderedIds).associateBy { it.id }
        // The batch endpoint does not preserve request order, so restore the ranking here.
        return orderedIds.mapNotNull { byId[it] }.map { it.toDomain() }
    }

    suspend fun searchGames(term: String, limit: Int = DEFAULT_LIMIT): List<Game> {
        val trimmed = term.trim()
        if (trimmed.length < MIN_SEARCH_LENGTH) return emptyList()
        return client.searchGames(trimmed, limit).map { it.toDomain() }
    }

    suspend fun getGame(igdbId: Long): Game? = client.gameDetail(igdbId)?.toDomain()

    /** Throws an [com.ludicindex.app.data.remote.IgdbException] with a user-facing message. */
    suspend fun testConnection() = client.ping()

    private companion object {
        const val DEFAULT_LIMIT = 24
        const val MIN_SEARCH_LENGTH = 2
    }
}

internal fun GameDto.toDomain(): Game = Game(
    igdbId = id,
    name = name,
    coverImageId = cover?.imageId,
    releaseDate = firstReleaseDate,
    rating = totalRating ?: rating,
    summary = summary,
    genres = genres.map { it.name },
    platforms = platforms.map { it.name },
)

internal fun InvolvedCompanyDto.roleLabel(): String = when {
    developer -> "开发商"
    publisher -> "发行商"
    porting -> "移植"
    supporting -> "支持"
    else -> "参与"
}
