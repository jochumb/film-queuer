package me.jochum.filmqueuer.adapters.letterboxd

data class LetterboxdFilmRow(
    val title: String,
    val year: Int?,
)

/**
 * Parses the CSV files found in a Letterboxd data export (Settings > Import & Export > Export).
 * Different export files have different headers/preambles, but "Name" and "Year" always sit at
 * the same relative column position once the real header row is found.
 */
object LetterboxdCsvParser {
    /**
     * List exports (e.g. a named list like "Blu-ray Collection") have a metadata preamble
     * ("Letterboxd list export v7", a Date/Name/Tags/URL/Description row for the list itself,
     * then a blank line) before the actual "Position,Name,Year,URL,Description" film table.
     */
    fun parseListExport(content: String): List<LetterboxdFilmRow> {
        val lines = content.lines()
        val headerIndex = lines.indexOfFirst { it.startsWith("Position,Name,Year,URL,Description") }
        require(headerIndex >= 0) { "Not a recognizable Letterboxd list export: missing film table header" }
        return parseRows(lines.drop(headerIndex + 1), nameIndex = 1, yearIndex = 2)
    }

    /** watched.csv has no preamble: "Date,Name,Year,Letterboxd URI" then one row per film. */
    fun parseWatchedExport(content: String): List<LetterboxdFilmRow> {
        val lines = content.lines()
        require(lines.firstOrNull()?.startsWith("Date,Name,Year,Letterboxd URI") == true) {
            "Not a recognizable Letterboxd watched export: missing expected header"
        }
        return parseRows(lines.drop(1), nameIndex = 1, yearIndex = 2)
    }

    private fun parseRows(
        lines: List<String>,
        nameIndex: Int,
        yearIndex: Int,
    ): List<LetterboxdFilmRow> {
        return lines
            .filter { it.isNotBlank() }
            .map { parseCsvLine(it) }
            .filter { it.size > maxOf(nameIndex, yearIndex) }
            .map { fields ->
                LetterboxdFilmRow(
                    title = fields[nameIndex].trim(),
                    year = fields[yearIndex].trim().toIntOrNull(),
                )
            }
    }

    /**
     * Minimal RFC4180 CSV line parser: handles quoted fields, embedded commas inside quotes,
     * and doubled `""` as an escaped quote. Does not support fields spanning multiple lines.
     */
    fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }
}
