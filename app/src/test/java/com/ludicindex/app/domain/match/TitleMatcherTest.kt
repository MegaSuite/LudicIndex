package com.ludicindex.app.domain.match

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleMatcherTest {

    @Test
    fun normalizeFoldsCasePunctuationAndMarks() {
        assertEquals("cyberpunk 2077", TitleMatcher.normalize("Cyberpunk 2077\u2122"))
        assertEquals("tom clancy s rainbow six", TitleMatcher.normalize("Tom Clancy's Rainbow Six"))
        assertEquals("a and b", TitleMatcher.normalize("  A   &   B  "))
    }

    @Test
    fun matchKeyStripsEditionAndPlatformSuffixes() {
        assertEquals("stardew valley", TitleMatcher.matchKey("Stardew Valley PS4 & PS5"))
        assertEquals("cyberpunk 2077", TitleMatcher.matchKey("Cyberpunk 2077 Ultimate Edition"))
        assertEquals("hollow knight", TitleMatcher.matchKey("Hollow Knight"))
        assertEquals("the witcher 3 wild hunt", TitleMatcher.matchKey("The Witcher 3: Wild Hunt"))
    }

    @Test
    fun matchKeyKeepsSubtitleSoSequelsDoNotCollide() {
        assertNotEquals(
            TitleMatcher.matchKey("Hollow Knight"),
            TitleMatcher.matchKey("Hollow Knight: Silksong"),
        )
    }

    @Test
    fun searchTitleDropsTrailingPlatformMarkersButKeepsTheRest() {
        assertEquals("A Little to the Left", TitleMatcher.searchTitle("A Little to the Left PS4 & PS5"))
        assertEquals("Dying Light 2 Stay Human", TitleMatcher.searchTitle("Dying Light 2 Stay Human PS4&PS5"))
        assertEquals("GRIS", TitleMatcher.searchTitle("GRIS | PS4 & PS5"))
        assertEquals("HUMANKIND\u2122", TitleMatcher.searchTitle("HUMANKIND\u2122 PS4 & PS5"))
        // A full-width space separates the title and the marker in some catalog entries.
        assertEquals(
            "EARTH DEFENSE FORCE: WORLD BROTHERS 2",
            TitleMatcher.searchTitle("EARTH DEFENSE FORCE: WORLD BROTHERS 2\u3000PS4 & PS5"),
        )
        // Titles without the marker are untouched.
        assertEquals("Hollow Knight: Silksong", TitleMatcher.searchTitle("Hollow Knight: Silksong"))
    }

    @Test
    fun unambiguousIndexKeepsUniqueTitles() {
        val index = TitleMatcher.unambiguousIndex(
            listOf(1L to "Celeste", 2L to "Halo Infinite"),
        )

        assertEquals(1L, index["celeste"])
        assertEquals(2L, index["halo infinite"])
    }

    @Test
    fun unambiguousIndexDropsTitlesSharedByMoreThanOneGame() {
        // "Halo Infinite" and "Halo Infinite Remaster" collapse to the same key once the
        // edition suffix is stripped; an ambiguous key must not be used.
        val index = TitleMatcher.unambiguousIndex(
            listOf(1L to "Celeste", 2L to "Halo Infinite", 3L to "Halo Infinite Remaster"),
        )

        assertEquals(1L, index["celeste"])
        assertNull(index["halo infinite"])
        assertTrue(index.size == 1)
    }
}
