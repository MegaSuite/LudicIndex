package com.ludicindex.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NamedDto(
    val id: Long = 0,
    val name: String = "",
)

@Serializable
data class ImageRefDto(
    val id: Long = 0,
    @SerialName("image_id") val imageId: String? = null,
)

@Serializable
data class VideoDto(
    val id: Long = 0,
    val name: String? = null,
    @SerialName("video_id") val videoId: String? = null,
)

@Serializable
data class CompanyDto(
    val id: Long = 0,
    val name: String = "",
)

@Serializable
data class InvolvedCompanyDto(
    val id: Long = 0,
    val company: CompanyDto? = null,
    val developer: Boolean = false,
    val publisher: Boolean = false,
    val porting: Boolean = false,
    val supporting: Boolean = false,
)

@Serializable
data class WebsiteDto(
    val id: Long = 0,
    val url: String = "",
    val type: Int? = null,
)

@Serializable
data class PopularityPrimitiveDto(
    val id: Long = 0,
    @SerialName("game_id") val gameId: Long = 0,
    @SerialName("popularity_type") val popularityType: Int = 0,
    val value: Double = 0.0,
)

@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("token_type") val tokenType: String = "bearer",
)

@Serializable
data class IgdbErrorDto(
    val title: String? = null,
    val status: Int? = null,
    val cause: String? = null,
)

/**
 * A game as returned by IGDB. Every field is optional-with-default: IGDB is mid-migration
 * (enum fields turning into tables) and different endpoints return different field sets.
 */
@Serializable
data class LanguageSupportDto(
    val id: Long = 0,
    val language: NamedDto? = null,
)

@Serializable
data class GameDto(
    val id: Long = 0,
    val name: String = "",
    val summary: String? = null,
    val storyline: String? = null,
    @SerialName("first_release_date") val firstReleaseDate: Long? = null,
    val rating: Double? = null,
    @SerialName("rating_count") val ratingCount: Int? = null,
    @SerialName("aggregated_rating") val aggregatedRating: Double? = null,
    @SerialName("aggregated_rating_count") val aggregatedRatingCount: Int? = null,
    @SerialName("total_rating") val totalRating: Double? = null,
    @SerialName("total_rating_count") val totalRatingCount: Int? = null,
    val hypes: Int? = null,
    val follows: Int? = null,
    @SerialName("game_type") val gameType: Int? = null,
    @SerialName("parent_game") val parentGame: Long? = null,
    @SerialName("version_parent") val versionParent: Long? = null,
    val cover: ImageRefDto? = null,
    val bundles: List<GameDto> = emptyList(),
    val artworks: List<ImageRefDto> = emptyList(),
    val screenshots: List<ImageRefDto> = emptyList(),
    val videos: List<VideoDto> = emptyList(),
    val genres: List<NamedDto> = emptyList(),
    val themes: List<NamedDto> = emptyList(),
    val platforms: List<NamedDto> = emptyList(),
    @SerialName("game_modes") val gameModes: List<NamedDto> = emptyList(),
    @SerialName("player_perspectives") val playerPerspectives: List<NamedDto> = emptyList(),
    @SerialName("game_engines") val gameEngines: List<NamedDto> = emptyList(),
    @SerialName("involved_companies") val involvedCompanies: List<InvolvedCompanyDto> = emptyList(),
    val collection: NamedDto? = null,
    val franchise: NamedDto? = null,
    @SerialName("similar_games") val similarGames: List<GameDto> = emptyList(),
    @SerialName("language_supports") val languageSupports: List<LanguageSupportDto> = emptyList(),
    val websites: List<WebsiteDto> = emptyList(),
)
