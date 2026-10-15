package com.ludicindex.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameDetailTest {

    @Test
    fun storeOptionsComeFromStoreLinksOnly() {
        val detail = GameDetail(
            igdbId = 1,
            name = "X",
            websites = listOf(
                WebsiteLink("https://www.facebook.com/x", 4),
                WebsiteLink("https://store.steampowered.com/app/1", 13),
                // PlayStation Store uses a newer, undocumented type id.
                WebsiteLink("https://store.playstation.com/en-us/concept/1", 30),
                WebsiteLink("https://www.xbox.com/en-US/games/store/x/9N", 20),
                WebsiteLink("https://en.wikipedia.org/wiki/X", 3),
            ),
        )

        assertEquals(
            listOf("Steam", "PlayStation Store", "Xbox Store"),
            detail.storeOptions(),
        )
    }

    @Test
    fun storeOptionsAreEmptyWhenThereAreNoStoreLinks() {
        val detail = GameDetail(
            igdbId = 1,
            name = "X",
            websites = listOf(WebsiteLink("https://www.facebook.com/x", 4)),
        )

        assertTrue(detail.storeOptions().isEmpty())
    }
}
