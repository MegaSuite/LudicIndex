package com.ludicindex.app.domain.website

import org.junit.Assert.assertEquals
import org.junit.Test

class WebsiteTypesTest {

    @Test
    fun playstationAndXboxStoreLinksAreClassifiedAsStores() {
        // These store types are not in the documented 1-19 enum, so the host has to decide.
        assertEquals(
            WebsiteCategory.STORE,
            websiteCategory(30, "https://store.playstation.com/en-us/concept/10002694"),
        )
        assertEquals(
            "PlayStation Store",
            websiteLabel(30, "https://store.playstation.com/en-us/concept/10002694"),
        )
        assertEquals(
            WebsiteCategory.STORE,
            websiteCategory(null, "https://www.xbox.com/en-US/games/store/foo/9NPDN9R45JX4"),
        )
        assertEquals(
            "Xbox Store",
            websiteLabel(20, "https://www.xbox.com/en-US/games/store/foo/9NPDN9R45JX4"),
        )
    }

    @Test
    fun documentedTypesStillWin() {
        assertEquals(WebsiteCategory.STORE, websiteCategory(13, "https://store.steampowered.com/app/292030"))
        assertEquals("Steam", websiteLabel(13, "https://store.steampowered.com/app/292030"))
        assertEquals(WebsiteCategory.SOCIAL, websiteCategory(9, "https://www.youtube.com/watch?v=x"))
        assertEquals("YouTube", websiteLabel(9, "https://www.youtube.com/watch?v=x"))
        assertEquals(WebsiteCategory.WIKI, websiteCategory(3, "https://en.wikipedia.org/wiki/X"))
    }

    @Test
    fun storeHostBeatsAnUnknownType() {
        assertEquals(
            WebsiteCategory.STORE,
            websiteCategory(77, "https://www.nintendo.com/store/products/foo/"),
        )
        assertEquals("Nintendo eShop", websiteLabel(77, "https://www.nintendo.com/store/products/foo/"))
    }

    @Test
    fun socialHostsAreGroupedAsSocial() {
        assertEquals(WebsiteCategory.SOCIAL, websiteCategory(null, "https://twitter.com/cdprojektred"))
        assertEquals(WebsiteCategory.SOCIAL, websiteCategory(null, "https://bsky.app/profile/x"))
        assertEquals(WebsiteCategory.SOCIAL, websiteCategory(null, "https://discord.gg/abc"))
    }

    @Test
    fun unknownHostFallsBackToTheDomainNotTheWordLink() {
        assertEquals("ign.com", websiteLabel(99, "https://www.ign.com/games/the-witcher-3"))
        assertEquals(WebsiteCategory.OTHER, websiteCategory(99, "https://www.ign.com/games/the-witcher-3"))
        // No url and no known type never yields a bare "链接".
        assertEquals("网站", websiteLabel(null, null))
    }
}
