package com.ludicindex.app.data.backup

import com.ludicindex.app.data.remote.dto.GameDto
import com.ludicindex.app.data.remote.dto.IgdbErrorDto
import com.ludicindex.app.data.remote.dto.PopularityPrimitiveDto
import com.ludicindex.app.data.remote.dto.TokenResponseDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Parses payloads recorded from the live API, so field renames show up as test failures. */
class IgdbJsonParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun games(payload: String): List<GameDto> =
        json.decodeFromString(ListSerializer(GameDto.serializer()), payload)

    @Test
    fun parsesTheWitcherDetailPayload() {
        val payload = """
            [{"id":1942,"age_ratings":[{"id":222156,"rating_category":38}],
              "aggregated_rating":91.73076923076923,"aggregated_rating_count":26,
              "alternative_names":[{"id":173745,"comment":"Alternative title","name":"Witcher III"}],
              "artworks":[{"id":303975,"image_id":"ar6ijr"}],
              "cover":{"id":480513,"image_id":"coaarl"},
              "first_release_date":1431993600,
              "game_engines":[{"id":24,"name":"REDengine"}],
              "game_modes":[{"id":1,"name":"Single player"}],
              "game_type":0,
              "genres":[{"id":12,"name":"Role-playing (RPG)"},{"id":31,"name":"Adventure"}],
              "hypes":179,
              "involved_companies":[{"id":17436,"company":{"id":50,"name":"WB Games"},
                "developer":false,"porting":false,"publisher":true,"supporting":false}],
              "keywords":[{"id":98,"name":"monsters"}],
              "name":"The Witcher 3: Wild Hunt",
              "platforms":[{"id":169,"name":"Xbox Series X|S"}],
              "player_perspectives":[{"id":2,"name":"Third person"}],
              "rating":93.77415284802106,"rating_count":5491,
              "screenshots":[{"id":1377966,"image_id":"sctj8u"}],
              "similar_games":[{"id":119133,"cover":{"id":212094,"image_id":"co4jni"},"name":"Elden Ring"}],
              "storyline":"The Witcher 3 concludes the story...",
              "summary":"An open-world action role-playing game.",
              "themes":[{"id":1,"name":"Action"}],
              "total_rating":92.75246103939514,"total_rating_count":5517,
              "videos":[{"id":9648,"name":"Developer Diary","video_id":"yowv6_rspoM"}],
              "websites":[{"id":19103,"url":"https://www.facebook.com/CDPROJEKTRED","type":4}]}]
        """.trimIndent()

        val game = games(payload).single()

        assertEquals(1942L, game.id)
        assertEquals("The Witcher 3: Wild Hunt", game.name)
        assertEquals("coaarl", game.cover?.imageId)
        assertEquals(92.75, game.totalRating!!, 0.01)
        assertEquals(0, game.gameType)
        assertEquals("Elden Ring", game.similarGames.single().name)
        assertEquals("yowv6_rspoM", game.videos.single().videoId)
        assertEquals("WB Games", game.involvedCompanies.single().company?.name)
        assertEquals("sctj8u", game.screenshots.single().imageId)
    }

    @Test
    fun toleratesFieldsThatIgbdHasDropped() {
        // "status" and "category" were removed during the enum-to-table migration.
        val payload = """[{"id":1,"name":"X","status":0,"category":0,"unexpected":{"a":1}}]"""

        val game = games(payload).single()

        assertEquals("X", game.name)
        assertNull(game.gameType)
    }

    @Test
    fun parsesPopularityPrimitives() {
        val payload = """[{"id":3890284,"game_id":117297,"popularity_type":1,"value":0.004334866510779}]"""

        val primitive = json.decodeFromString(
            ListSerializer(PopularityPrimitiveDto.serializer()),
            payload,
        ).single()

        assertEquals(117297L, primitive.gameId)
        assertEquals(1, primitive.popularityType)
    }

    @Test
    fun parsesTheTwitchTokenResponse() {
        val payload = """{"access_token":"abc","expires_in":5587808,"token_type":"bearer"}"""

        val token = json.decodeFromString(TokenResponseDto.serializer(), payload)

        assertEquals("abc", token.accessToken)
        assertEquals(5587808L, token.expiresIn)
    }

    @Test
    fun parsesIgbdErrorBodies() {
        val payload = """{"title":"Invalid Field","status":400,"cause":"Invalid field name: 'popularity'"}"""

        val error = json.decodeFromString(IgdbErrorDto.serializer(), payload)

        assertEquals("Invalid field name: 'popularity'", error.cause)
    }
}
