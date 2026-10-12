package com.ludicindex.app.domain

import com.ludicindex.app.domain.model.Tag
import com.ludicindex.app.domain.model.WishlistEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class WishlistQueryTest {

    @Test
    fun filtersByQueryIgnoringCase() {
        val entries = listOf(entry(1, "Hollow Knight"), entry(2, "Celeste"))

        val result = entries.filterAndSort(WishlistFilter(query = "hollow"))

        assertEquals(listOf("Hollow Knight"), result.map { it.name })
    }

    @Test
    fun blankQueryMatchesEverything() {
        val entries = listOf(entry(1, "A"), entry(2, "B"))

        assertEquals(2, entries.filterAndSort(WishlistFilter(query = "   ")).size)
    }

    @Test
    fun matchAllTagsRequiresEverySelectedTag() {
        val entries = listOf(
            entry(1, "Both", tagIds = listOf(10, 20)),
            entry(2, "OnlyTen", tagIds = listOf(10)),
        )

        val result = entries.filterAndSort(
            WishlistFilter(tagIds = setOf(10, 20), matchAllTags = true),
        )

        assertEquals(listOf("Both"), result.map { it.name })
    }

    @Test
    fun matchAnyTagAcceptsPartialOverlap() {
        val entries = listOf(
            entry(1, "Both", tagIds = listOf(10, 20)),
            entry(2, "OnlyTen", tagIds = listOf(10)),
            entry(3, "None", tagIds = listOf(30)),
        )

        val result = entries.filterAndSort(
            WishlistFilter(
                tagIds = setOf(10, 20),
                matchAllTags = false,
                sort = WishlistSort.NAME_ASC,
            ),
        )

        assertEquals(listOf("Both", "OnlyTen"), result.map { it.name })
    }

    @Test
    fun sortsByNameCaseInsensitively() {
        val entries = listOf(entry(1, "zelda"), entry(2, "Ape Out"), entry(3, "Celeste"))

        val result = entries.filterAndSort(WishlistFilter(sort = WishlistSort.NAME_ASC))

        assertEquals(listOf("Ape Out", "Celeste", "zelda"), result.map { it.name })
    }

    @Test
    fun sortsByReleaseDateDescendingWithNullsLast() {
        val entries = listOf(
            entry(1, "NoDate", releaseDate = null),
            entry(2, "Old", releaseDate = 1_000),
            entry(3, "New", releaseDate = 2_000),
        )

        val result = entries.filterAndSort(WishlistFilter(sort = WishlistSort.RELEASE_DATE_DESC))

        assertEquals(listOf("New", "Old", "NoDate"), result.map { it.name })
    }

    @Test
    fun sortsByRatingDescendingWithNullsLast() {
        val entries = listOf(
            entry(1, "Unrated", rating = null),
            entry(2, "Good", rating = 80.0),
            entry(3, "Great", rating = 95.0),
        )

        val result = entries.filterAndSort(WishlistFilter(sort = WishlistSort.RATING_DESC))

        assertEquals(listOf("Great", "Good", "Unrated"), result.map { it.name })
    }

    @Test
    fun addedDescIsTheDefaultOrder() {
        val entries = listOf(entry(1, "First", addedAt = 100), entry(2, "Second", addedAt = 200))

        val result = entries.filterAndSort(WishlistFilter())

        assertEquals(listOf("Second", "First"), result.map { it.name })
    }

    @Test
    fun combinesQueryAndTagFilters() {
        val entries = listOf(
            entry(1, "Hollow Knight", tagIds = listOf(10)),
            entry(2, "Hollow Knight: Silksong", tagIds = listOf(20)),
        )

        val result = entries.filterAndSort(WishlistFilter(query = "hollow", tagIds = setOf(10)))

        assertEquals(listOf("Hollow Knight"), result.map { it.name })
    }

    private fun entry(
        id: Long,
        name: String,
        addedAt: Long = id,
        releaseDate: Long? = null,
        rating: Double? = null,
        tagIds: List<Long> = emptyList(),
    ) = WishlistEntry(
        id = id,
        igdbId = id,
        name = name,
        coverImageId = null,
        releaseDate = releaseDate,
        rating = rating,
        summary = null,
        addedAt = addedAt,
        updatedAt = addedAt,
        tags = tagIds.map { Tag(it, "tag$it", isPreset = false, colorArgb = 0) },
    )
}
