package me.jochum.filmqueuer.adapters.web

import kotlinx.serialization.Serializable

@Serializable
data class FilmDto(
    val id: Int,
    val title: String,
    val originalTitle: String? = null,
    val releaseDate: String? = null,
    val posterPath: String? = null,
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val overview: String? = null,
    val mediaType: String? = null,
    val role: String? = null,
)

@Serializable
data class FilmographyDto(
    val films: List<FilmDto>,
    val availableDepartments: List<String>,
)
