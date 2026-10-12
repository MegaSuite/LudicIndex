package com.ludicindex.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TitleListParserTest {

    @Test
    fun splitsOneTitlePerLine() {
        val titles = parseGameTitles("Hollow Knight\nCeleste\nHades")

        assertEquals(listOf("Hollow Knight", "Celeste", "Hades"), titles)
    }

    @Test
    fun stripsCommonListMarkers() {
        val titles = parseGameTitles(
            """
            - Hollow Knight
            * Celeste
            1. Hades
            2) Ori and the Blind Forest
            • 3. Silksong
            """.trimIndent(),
        )

        assertEquals(
            listOf("Hollow Knight", "Celeste", "Hades", "Ori and the Blind Forest", "3. Silksong"),
            titles,
        )
    }

    @Test
    fun dropsBlankLinesAndTrimsWhitespace() {
        val titles = parseGameTitles("  Celeste  \n\n   \n\tHades\t\n")

        assertEquals(listOf("Celeste", "Hades"), titles)
    }

    @Test
    fun collapsesDuplicatesIgnoringCaseAndKeepsOrder() {
        val titles = parseGameTitles("Celeste\nhades\nCELESTE\nHades")

        assertEquals(listOf("Celeste", "hades"), titles)
    }

    @Test
    fun handlesWindowsLineEndings() {
        val titles = parseGameTitles("Celeste\r\nHades\r\n")

        assertEquals(listOf("Celeste", "Hades"), titles)
    }

    @Test
    fun keepsTitlesWithNumbersInsideThem() {
        val titles = parseGameTitles("Final Fantasy 7 Remake\nNieR:Automata 2B")

        assertEquals(listOf("Final Fantasy 7 Remake", "NieR:Automata 2B"), titles)
    }

    @Test
    fun returnsNothingForEmptyInput() {
        assertEquals(emptyList<String>(), parseGameTitles(""))
        assertEquals(emptyList<String>(), parseGameTitles("   \n  \n"))
    }
}
