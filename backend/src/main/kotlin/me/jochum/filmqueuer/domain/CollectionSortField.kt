package me.jochum.filmqueuer.domain

enum class CollectionSortField {
    TITLE,
    YEAR,
    DIRECTOR,
    ADDED,
    ;

    companion object {
        fun fromParam(value: String?): CollectionSortField =
            when (value?.lowercase()) {
                "year" -> YEAR
                "director" -> DIRECTOR
                "added" -> ADDED
                else -> TITLE
            }
    }
}
