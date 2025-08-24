package me.jochum.filmqueuer.domain

data class Person(
    val tmdbId: Int,
    val name: String,
    val department: Department,
)

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
