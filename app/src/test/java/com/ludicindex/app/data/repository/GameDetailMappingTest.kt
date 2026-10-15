package com.ludicindex.app.data.repository

import com.ludicindex.app.UNRELEASED_DETAIL_JSON
import com.ludicindex.app.WITCHER_DETAIL_JSON
import com.ludicindex.app.data.remote.dto.GameDto
import com.ludicindex.app.domain.model.CompanyRef
import com.ludicindex.app.domain.model.ReleaseStatus
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDetailMappingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun detailOf(payload: String) = json
        .decodeFromString(ListSerializer(GameDto.serializer()), payload)
        .single()
        .toDetail()

    @Test
    fun splitsCompaniesByTheirRole() {
        val detail = detailOf(WITCHER_DETAIL_JSON)

        assertEquals(listOf(CompanyRef(1, "CD Projekt RED")), detail.developers)
        assertEquals(listOf(CompanyRef(50, "WB Games")), detail.publishers)
        assertEquals(listOf(CompanyRef(99, "Fishlabs")), detail.portingCompanies)
    }

    @Test
    fun mapsMediaCollectionsAndSeries() {
        val detail = detailOf(WITCHER_DETAIL_JSON)

        assertEquals("coaarl", detail.coverImageId)
        assertEquals(listOf("ar6ijr"), detail.artworkImageIds)
        assertEquals(listOf("sctj8u"), detail.screenshotImageIds)
        assertEquals("yowv6_rspoM", detail.videos.single().videoId)
        assertEquals("The Witcher", detail.collection)
        assertEquals(listOf("Elden Ring", "Dragon Age: Inquisition"), detail.similarGames.map { it.name })
        assertEquals("co4jni", detail.similarGames.first().coverImageId)
    }

    @Test
    fun mapsLanguagesDeduplicatedAndSorted() {
        val detail = detailOf(WITCHER_DETAIL_JSON)

        // "German" appears twice in the payload.
        assertEquals(listOf("German", "Polish"), detail.languages)
    }

    @Test
    fun keepsWebsiteTypesForLabelling() {
        val detail = detailOf(WITCHER_DETAIL_JSON)

        assertEquals(listOf(4, 16), detail.websites.map { it.type })
    }

    @Test
    fun mapsAllScalarFields() {
        val detail = detailOf(WITCHER_DETAIL_JSON)

        assertEquals(1942L, detail.igdbId)
        assertEquals("The Witcher 3: Wild Hunt", detail.name)
        assertEquals(5491, detail.ratingCount)
        assertEquals(179, detail.hypes)
        assertEquals(listOf("Role-playing (RPG)", "Adventure"), detail.genres)
        assertEquals(listOf("PC (Microsoft Windows)"), detail.platforms)
        assertEquals(listOf("REDengine"), detail.engines)
    }

    @Test
    fun groupsRelatedGamesByGameTypeInDisplayOrder() {
        val base = json
            .decodeFromString(ListSerializer(GameDto.serializer()), WITCHER_DETAIL_JSON)
            .single()
        val related = json.decodeFromString(
            ListSerializer(GameDto.serializer()),
            """
            [
              {"id":10,"name":"Blood and Wine","game_type":2,"parent_game":1942,"cover":{"image_id":"co1"}},
              {"id":11,"name":"DLC One","game_type":1,"parent_game":1942},
              {"id":12,"name":"Next-Gen Update","game_type":14,"parent_game":1942},
              {"id":13,"name":"Deluxe Edition","game_type":0,"version_parent":1942},
              {"id":99,"name":"Some Future Type","game_type":77,"parent_game":1942}
            ]
            """.trimIndent(),
        )

        val groups = base.toDetail(related).relatedGroups

        assertEquals(
            listOf("资料片", "DLC", "更新", "其他（类型 77）", "版本"),
            groups.map { it.title },
        )
        assertEquals(listOf("Blood and Wine"), groups.first().games.map { it.name })
        assertEquals("co1", groups.first().games.single().coverImageId)
        // Editions are the ones linked via version_parent.
        assertEquals(listOf("Deluxe Edition"), groups.last().games.map { it.name })
    }

    @Test
    fun bundledInBecomesItsOwnGroup() {
        val bundles = json.decodeFromString(
            ListSerializer(GameDto.serializer()),
            """[{"id":22439,"name":"GOTY Edition","cover":{"image_id":"co1wz4"}}]""",
        )
        val base = json
            .decodeFromString(ListSerializer(GameDto.serializer()), WITCHER_DETAIL_JSON)
            .single()
            .copy(bundles = bundles)

        val groups = base.toDetail(emptyList()).relatedGroups

        assertEquals(listOf("包含于合辑"), groups.map { it.title })
        assertEquals(listOf("GOTY Edition"), groups.single().games.map { it.name })
    }

    @Test
    fun relatedGroupsAreEmptyWhenNothingIsLinked() {
        assertTrue(detailOf(WITCHER_DETAIL_JSON).relatedGroups.isEmpty())
    }

    @Test
    fun derivesReleaseStatusFromTheReleaseDate() {
        val released = detailOf(WITCHER_DETAIL_JSON)
        val upcoming = detailOf(UNRELEASED_DETAIL_JSON)
        val now = System.currentTimeMillis() / 1000

        assertEquals(ReleaseStatus.RELEASED, released.releaseStatus(now))
        assertEquals(ReleaseStatus.UPCOMING, upcoming.releaseStatus(now))
    }

    @Test
    fun toleratesAMissingReleaseDate() {
        val detail = detailOf("""[{"id":1,"name":"Mystery"}]""")

        assertNull(detail.releaseDate)
        assertEquals(ReleaseStatus.UNKNOWN, detail.releaseStatus(0))
        assertTrue(detail.genres.isEmpty())
    }
}
