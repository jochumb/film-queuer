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
    val tv: Boolean = false,
)

@Serializable
data class FilmographyDto(
    val films: List<FilmDto>,
    val availableDepartments: List<String>,
)

@Serializable
data class FilmSearchResponseDto(
    val page: Int,
    val results: List<FilmDto>,
    val totalPages: Int,
    val totalResults: Int,
)
