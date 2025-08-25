package me.jochum.filmqueuer.domain

import java.time.LocalDate

data class Film(
    val tmdbId: Int,
    val title: String,
    val originalTitle: String? = null,
    val releaseDate: LocalDate? = null,
)
