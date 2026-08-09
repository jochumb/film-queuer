package me.jochum.filmqueuer.domain

import java.time.LocalDate

data class Film(
    val tmdbId: Int,
    val title: String,
    val originalTitle: String? = null,
    val releaseDate: LocalDate? = null,
    val runtime: Int? = null,
    val genres: List<String>? = null,
    val posterPath: String? = null,
    val tv: Boolean = false,
    val directorTmdbIds: List<Int> = emptyList(),
    // Null means "no explicit sort title yet" - the repository fills in a computed default
    // (title with a leading "The"/"A"/"An" stripped) on first insert and preserves any later
    // manual correction across re-matches, mirroring Person.sortName.
    val sortTitle: String? = null,
) {
    companion object {
        private val LEADING_ARTICLES = listOf("The ", "An ", "A ")

        fun defaultSortTitle(title: String): String {
            val trimmed = title.trim()
            val article = LEADING_ARTICLES.firstOrNull { trimmed.startsWith(it, ignoreCase = true) }
            return if (article != null) trimmed.substring(article.length) else trimmed
        }
    }
}
