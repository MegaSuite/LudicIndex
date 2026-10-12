package com.ludicindex.app.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class ApicalypseTest {

    @Test
    fun buildsClausesInCallOrder() {
        val query = Apicalypse.builder()
            .fields("name", "rating")
            .where("rating > 80")
            .sort("rating")
            .search("zelda")
            .limit(10)
            .build()

        assertEquals(
            "fields name,rating; where rating > 80; sort rating desc; " +
                "search \"zelda\"; limit 10;",
            query,
        )
    }

    @Test
    fun supportsAscendingSortAndOffset() {
        val query = Apicalypse.builder()
            .fields("id")
            .sort("name", descending = false)
            .limit(33)
            .offset(22)
            .build()

        assertEquals("fields id; sort name asc; limit 33; offset 22;", query)
    }

    @Test
    fun escapesQuotesAndBackslashesInSearchTerms() {
        val query = Apicalypse.builder().search("say \"hi\" \\ bye").build()

        assertEquals("search \"say \\\"hi\\\" \\\\ bye\";", query)
    }

    @Test
    fun buildsIdBatches() {
        val ids = listOf(1942L, 1877L, 7L)

        val query = Apicalypse.builder()
            .fields("id", "name")
            .where("id = (${ids.joinToString(",")})")
            .limit(ids.size)
            .build()

        assertEquals("fields id,name; where id = (1942,1877,7); limit 3;", query)
    }
}
