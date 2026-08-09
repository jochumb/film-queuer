package me.jochum.filmqueuer.domain

data class Person(
    val tmdbId: Int,
    val name: String,
    val department: Department,
    val imagePath: String? = null,
    // Null means "no explicit sort name yet" - the repository fills in a computed default
    // ("Lastname, Firstname", using the last word of `name` as the surname) on first insert and
    // preserves any later manual correction on subsequent saves, so re-resolving a person (e.g.
    // via another film match) never clobbers a fix the user made through the sort-name edit
    // endpoint. Sorting on the full "Lastname, Firstname" string is equivalent to sorting on the
    // surname alone, since the comma and first name only break ties within the same surname.
    val sortName: String? = null,
) {
    companion object {
        fun defaultSortName(name: String): String {
            val trimmed = name.trim()
            val lastSpace = trimmed.lastIndexOf(' ')
            if (lastSpace == -1) return trimmed
            val lastName = trimmed.substring(lastSpace + 1)
            val firstNames = trimmed.substring(0, lastSpace)
            return "$lastName, $firstNames"
        }
    }
}

enum class Department {
    ACTING,
    DIRECTING,
    WRITING,
    OTHER,
    ;

    companion object {
        fun fromString(value: String?): Department {
            return when (value?.lowercase()) {
                "acting" -> ACTING
                "directing" -> DIRECTING
                "writing" -> WRITING
                else -> OTHER
            }
        }
    }
}
