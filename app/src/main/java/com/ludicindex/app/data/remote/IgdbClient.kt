package com.ludicindex.app.data.remote

import com.ludicindex.app.data.remote.dto.GameDto
import com.ludicindex.app.data.remote.dto.IgdbErrorDto
import com.ludicindex.app.data.remote.dto.PopularityPrimitiveDto
import com.ludicindex.app.domain.model.CompanyRole
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

/**
 * Thin, typed wrapper over [IgdbApi]: builds Apicalypse queries, adds auth headers, respects the
 * rate limit, retries once on 401 with a fresh token, and maps HTTP failures to [IgdbException].
 */
class IgdbClient(
    private val api: IgdbApi,
    private val auth: IgdbAuth,
    private val json: Json,
    private val rateLimiter: RateLimiter,
    private val baseUrl: String = DEFAULT_BASE_URL,
) {

    /** Cheapest possible authenticated call, used by the "test connection" button. */
    suspend fun ping(): Unit {
        post("games", Apicalypse.builder().fields("id").limit(1).build())
    }

    suspend fun popularityPrimitives(
        popularityType: Int,
        limit: Int,
    ): List<PopularityPrimitiveDto> = decode(
        post(
            endpoint = "popularity_primitives",
            apicalypse = Apicalypse.builder()
                .fields("game_id", "value", "popularity_type")
                .where("popularity_type = $popularityType")
                .sort("value")
                .limit(limit)
                .build(),
        ),
    )

    suspend fun gamesByIds(ids: List<Long>, fields: String = LIST_FIELDS): List<GameDto> {
        if (ids.isEmpty()) return emptyList()
        return decode(
            post(
                endpoint = "games",
                apicalypse = Apicalypse.builder()
                    .fields(*fields.split(",").toTypedArray())
                    .where("id = (${ids.joinToString(",")})")
                    .limit(ids.size)
                    .build(),
            ),
        )
    }

    suspend fun searchGames(term: String, limit: Int = 30): List<GameDto> = decode(
        post(
            endpoint = "games",
            apicalypse = Apicalypse.builder()
                .fields(*LIST_FIELDS.split(",").toTypedArray())
                .search(term)
                .where("version_parent = null & game_type != ($EXCLUDED_GAME_TYPES)")
                .limit(limit)
                .build(),
        ),
    )

    /**
     * IGDB's "Related Content" feed: every game hanging off this one, minus the base game itself.
     *
     * One query covers both kinds of relation:
     * - `parent_game` → expansions, DLCs, updates, remakes, remasters, ports, mods, …
     *   (the website groups these by `game_type`)
     * - `version_parent` → editions such as "Deluxe Edition"
     *
     * Callers split the result; [GameDto.parentGame] / [GameDto.versionParent] say which is which.
     */
    suspend fun relatedGames(igdbId: Long): List<GameDto> = decode(
        post(
            endpoint = "games",
            apicalypse = Apicalypse.builder()
                .fields(*RELATED_FIELDS.split(",").toTypedArray())
                .where("parent_game = $igdbId | version_parent = $igdbId")
                .sort("first_release_date", descending = false)
                .limit(RELATED_LIMIT)
                .build(),
        ),
    )

    /**
     * Games where [companyId] was the developer or the publisher, newest first. Editions and
     * non-games are excluded the same way search excludes them.
     */
    suspend fun companyGames(
        companyId: Long,
        role: CompanyRole,
        limit: Int = COMPANY_PAGE_SIZE,
        offset: Int = 0,
    ): List<GameDto> {
        val roleFilter = when (role) {
            CompanyRole.DEVELOPER -> "involved_companies.developer = true"
            CompanyRole.PUBLISHER -> "involved_companies.publisher = true"
        }
        return decode(
            post(
                endpoint = "games",
                apicalypse = Apicalypse.builder()
                    .fields(*LIST_FIELDS.split(",").toTypedArray())
                    .where(
                        "involved_companies.company = $companyId & $roleFilter" +
                            " & version_parent = null & game_type != ($EXCLUDED_GAME_TYPES)",
                    )
                    .sort("first_release_date", descending = true)
                    .limit(limit)
                    .offset(offset)
                    .build(),
            ),
        )
    }

    /** Fallback popularity source when the PopScore primitives come back empty. */
    suspend fun hypedGames(limit: Int): List<GameDto> = decode(
        post(
            endpoint = "games",
            apicalypse = Apicalypse.builder()
                .fields(*LIST_FIELDS.split(",").toTypedArray())
                .where("hypes > 50")
                .sort("hypes")
                .limit(limit)
                .build(),
        ),
    )

    /** Raw JSON for the detail endpoint, so the caller can cache exactly what IGDB sent. */
    suspend fun gameDetailPayload(igdbId: Long): String = post(
        endpoint = "games",
        apicalypse = Apicalypse.builder()
            .fields(*DETAIL_FIELDS.split(",").toTypedArray())
            .where("id = $igdbId")
            .limit(1)
            .build(),
    )

    suspend fun gameDetail(igdbId: Long): GameDto? =
        decode<List<GameDto>>(gameDetailPayload(igdbId)).firstOrNull()

    private suspend fun post(endpoint: String, apicalypse: String): String {
        val credentials = auth.credentials() ?: throw IgdbException.MissingCredentials()
        val body = apicalypse.toRequestBody(TEXT_PLAIN)
        val url = "$baseUrl/$endpoint"

        return try {
            call(url, credentials.clientId, body)
        } catch (e: HttpException) {
            when (e.code()) {
                HTTP_UNAUTHORIZED -> {
                    // The cached token was stale: force a refresh and retry exactly once.
                    val fresh = auth.token(forceRefresh = true) ?: throw IgdbException.InvalidCredentials(e)
                    try {
                        call(url, credentials.clientId, body, bearer = fresh)
                    } catch (retry: HttpException) {
                        throw mapHttpException(retry)
                    }
                }
                else -> throw mapHttpException(e)
            }
        } catch (e: IgdbException) {
            throw e
        } catch (e: java.io.IOException) {
            throw IgdbException.Network(e)
        }
    }

    private suspend fun call(
        url: String,
        clientId: String,
        body: okhttp3.RequestBody,
        bearer: String? = null,
    ): String {
        rateLimiter.acquire()
        val token = bearer ?: auth.token() ?: throw IgdbException.MissingCredentials()
        return api.post(url, clientId, "Bearer $token", body = body).string()
    }

    private fun mapHttpException(e: HttpException): IgdbException = when (e.code()) {
        HTTP_UNAUTHORIZED -> IgdbException.InvalidCredentials(e)
        HTTP_FORBIDDEN -> IgdbException.InvalidCredentials(e)
        HTTP_BAD_REQUEST -> IgdbException.BadRequest(detailOf(e))
        HTTP_RATE_LIMITED -> IgdbException.RateLimited()
        else -> IgdbException.ServerError(e.code())
    }

    private fun detailOf(e: HttpException): String = runCatching {
        val raw = e.response()?.errorBody()?.string().orEmpty()
        val parsed = json.decodeFromString(IgdbErrorDto.serializer(), raw)
        parsed.cause ?: parsed.title ?: raw
    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "未知错误"

    private inline fun <reified T> decode(raw: String): T =
        json.decodeFromString(raw)

    companion object {
        const val DEFAULT_BASE_URL = "https://api.igdb.com/v4"

        const val POPULARITY_TYPE_VISITS = 1

        /**
         * Search should not surface things you cannot really wishlist: DLC, bundles, mods, seasons,
         * packs and updates.
         *
         * Remakes/remasters/ports stay in. Eventually filtering on `game_type = 0` looked right but
         * was both too strict and ineffective: it hid remasters such as "Gears of War: Reloaded"
         * (type 9) while still returning "Deluxe Edition" duplicates (they are type 0).
         * `version_parent = null` is IGDB's documented way to drop edition duplicates.
         */
        private const val EXCLUDED_GAME_TYPES = "1,3,5,7,13,14"

        private const val RELATED_LIMIT = 100

        /** Page size for the "other games by this company" list. */
        const val COMPANY_PAGE_SIZE = 30

        private const val RELATED_FIELDS =
            "id,name,game_type,cover.image_id,first_release_date,parent_game,version_parent"

        private val TEXT_PLAIN = "text/plain; charset=utf-8".toMediaType()

        private const val HTTP_BAD_REQUEST = 400
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_RATE_LIMITED = 429

        const val LIST_FIELDS =
            "id,name,summary,cover.image_id,genres.name,platforms.name," +
                "first_release_date,total_rating,rating,rating_count,hypes"

        const val DETAIL_FIELDS =
            "id,name,summary,storyline,first_release_date,game_type," +
                "rating,rating_count,aggregated_rating,aggregated_rating_count," +
                "total_rating,total_rating_count,hypes,follows," +
                "cover.image_id,artworks.image_id,screenshots.image_id," +
                "videos.video_id,videos.name," +
                "genres.name,themes.name,platforms.name,game_modes.name,player_perspectives.name," +
                "involved_companies.company.name,involved_companies.developer," +
                "involved_companies.publisher,involved_companies.porting," +
                "collection.name,franchise.name,game_engines.name," +
                "similar_games.name,similar_games.cover.image_id," +
                "bundles.name,bundles.cover.image_id,bundles.first_release_date," +
                "websites.url,websites.type," +
                "language_supports.language.name"
    }
}
