package com.ludicindex.app.domain.model

import com.ludicindex.app.domain.website.WebsiteCategory
import com.ludicindex.app.domain.website.websiteCategory
import com.ludicindex.app.domain.website.websiteLabel

data class GameVideo(
    val name: String?,
    val videoId: String,
)

data class SimilarGame(
    val igdbId: Long,
    val name: String,
    val coverImageId: String?,
)

data class WebsiteLink(
    val url: String,
    val type: Int?,
)

/** One row inside a related-content group. */
data class RelatedGame(
    val igdbId: Long,
    val name: String,
    val coverImageId: String?,
    val releaseDate: Long?,
)

/** A named bucket of related games, e.g. "资料片" or "版本". */
data class RelatedGroup(
    val title: String,
    val games: List<RelatedGame>,
)

/** Everything the detail screen renders. Populated from IGDB's /games endpoint. */
data class GameDetail(
    val igdbId: Long,
    val name: String,
    val summary: String? = null,
    val storyline: String? = null,
    val releaseDate: Long? = null,
    val gameType: Int? = null,
    val rating: Double? = null,
    val ratingCount: Int? = null,
    val aggregatedRating: Double? = null,
    val aggregatedRatingCount: Int? = null,
    val totalRating: Double? = null,
    val totalRatingCount: Int? = null,
    val hypes: Int? = null,
    val follows: Int? = null,
    val coverImageId: String? = null,
    val artworkImageIds: List<String> = emptyList(),
    val screenshotImageIds: List<String> = emptyList(),
    val videos: List<GameVideo> = emptyList(),
    val genres: List<String> = emptyList(),
    val themes: List<String> = emptyList(),
    val platforms: List<String> = emptyList(),
    val gameModes: List<String> = emptyList(),
    val playerPerspectives: List<String> = emptyList(),
    val engines: List<String> = emptyList(),
    val developers: List<CompanyRef> = emptyList(),
    val publishers: List<CompanyRef> = emptyList(),
    val portingCompanies: List<CompanyRef> = emptyList(),
    val collection: String? = null,
    val franchise: String? = null,
    val similarGames: List<SimilarGame> = emptyList(),
    /** IGDB's "Related Content": expansions, DLCs, editions, bundles, … empty groups omitted. */
    val relatedGroups: List<RelatedGroup> = emptyList(),
    val websites: List<WebsiteLink> = emptyList(),
    val languages: List<String> = emptyList(),
) {
    /** IGDB dropped the release-status enum; derive it from the release date instead. */
    fun releaseStatus(nowEpochSeconds: Long): ReleaseStatus = when {
        releaseDate == null -> ReleaseStatus.UNKNOWN
        releaseDate > nowEpochSeconds -> ReleaseStatus.UPCOMING
        else -> ReleaseStatus.RELEASED
    }

    /**
     * Labels of this game's store/platform links (Steam, PlayStation Store, Xbox Store, …) in IGDB
     * order. This is what a wishlist entry can be tagged with as its platform.
     */
    fun storeOptions(): List<String> = websites
        .filter { websiteCategory(it.type, it.url) == WebsiteCategory.STORE }
        .map { websiteLabel(it.type, it.url) }
        .distinct()

    fun toGame(): Game = Game(
        igdbId = igdbId,
        name = name,
        coverImageId = coverImageId,
        releaseDate = releaseDate,
        rating = totalRating ?: rating,
        summary = summary,
        genres = genres,
        platforms = platforms,
    )
}

enum class ReleaseStatus { RELEASED, UPCOMING, UNKNOWN }
