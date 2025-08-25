package me.jochum.filmqueuer.domain

data class Film(
    val tmdbId: Int,
    val title: String,
    val originalTitle: String? = null,
    val releaseDate: String? = null,
)
