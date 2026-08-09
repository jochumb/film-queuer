package me.jochum.filmqueuer.adapters.letterboxd

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LetterboxdCsvParserTest {
    @Test
    fun `parseCsvLine should split plain comma separated fields`() {
        val fields = LetterboxdCsvParser.parseCsvLine("1,Gladiator,2000,https://boxd.it/2b9m,")
        assertEquals(listOf("1", "Gladiator", "2000", "https://boxd.it/2b9m", ""), fields)
    }

    @Test
    fun `parseCsvLine should keep commas inside quoted fields together`() {
        val fields = LetterboxdCsvParser.parseCsvLine("""2,"Three Billboards Outside Ebbing, Missouri",2017,https://boxd.it/ceBS,""")
        assertEquals(listOf("2", "Three Billboards Outside Ebbing, Missouri", "2017", "https://boxd.it/ceBS", ""), fields)
    }

    @Test
    fun `parseCsvLine should unescape doubled quotes inside quoted fields`() {
        val fields = LetterboxdCsvParser.parseCsvLine("1,\"She said \"\"hello\"\"\",2020,https://boxd.it/x,")
        assertEquals(listOf("1", "She said \"hello\"", "2020", "https://boxd.it/x", ""), fields)
    }

    @Test
    fun `parseListExport should skip the list metadata preamble and parse film rows`() {
        val csv =
            """
            Letterboxd list export v7
            Date,Name,Tags,URL,Description
            2023-09-30,Blu-ray Collection,"own, blu-ray",https://boxd.it/pqzlc,

            Position,Name,Year,URL,Description
            1,Gladiator,2000,https://boxd.it/2b9m,
            2,"Three Billboards Outside Ebbing, Missouri",2017,https://boxd.it/ceBS,
            """.trimIndent()

        val rows = LetterboxdCsvParser.parseListExport(csv)

        assertEquals(2, rows.size)
        assertEquals(LetterboxdFilmRow("Gladiator", 2000), rows[0])
        assertEquals(LetterboxdFilmRow("Three Billboards Outside Ebbing, Missouri", 2017), rows[1])
    }

    @Test
    fun `parseListExport should reject content without the expected film table header`() {
        assertFailsWith<IllegalArgumentException> {
            LetterboxdCsvParser.parseListExport("not,a,letterboxd,export")
        }
    }

    @Test
    fun `parseWatchedExport should parse rows with no preamble`() {
        val csv =
            """
            Date,Name,Year,Letterboxd URI
            2018-02-26,You Were Never Really Here,2017,https://boxd.it/dWOS
            2018-02-26,"Three Billboards Outside Ebbing, Missouri",2017,https://boxd.it/ceBS
            """.trimIndent()

        val rows = LetterboxdCsvParser.parseWatchedExport(csv)

        assertEquals(2, rows.size)
        assertEquals(LetterboxdFilmRow("You Were Never Really Here", 2017), rows[0])
        assertEquals(LetterboxdFilmRow("Three Billboards Outside Ebbing, Missouri", 2017), rows[1])
    }

    @Test
    fun `parseWatchedExport should reject content without the expected header`() {
        assertFailsWith<IllegalArgumentException> {
            LetterboxdCsvParser.parseWatchedExport("not,a,letterboxd,export")
        }
    }

    @Test
    fun `parseWatchedExport should treat a missing year as null`() {
        val csv =
            """
            Date,Name,Year,Letterboxd URI
            2018-02-26,Some Untitled Short,,https://boxd.it/xyz
            """.trimIndent()

        val rows = LetterboxdCsvParser.parseWatchedExport(csv)

        assertEquals(1, rows.size)
        assertEquals(LetterboxdFilmRow("Some Untitled Short", null), rows[0])
    }
}
