package me.jochum.filmqueuer.adapters.web

import kotlinx.serialization.Serializable

@Serializable
data class FilmRequestDto(
    val tmdbId: Int,
)

@Serializable
data class QueueFilmsDto(
    val films: List<FilmResponseDto>,
)

@Serializable
data class FilmResponseDto(
    val tmdbId: Int,
    val title: String,
    val originalTitle: String? = null,
    val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: String? = null,
    val posterPath: String? = null,
)

@Serializable
data class ReorderFilmsDto(
    val filmOrder: List<Int>,
)
