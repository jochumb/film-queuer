package me.jochum.filmqueuer.adapters.web

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class FilmDto(
    val id: Int,
    val title: String,
    val originalTitle: String? = null,
    @JsonSchema.Format("date")
    val releaseDate: String? = null,
    val posterPath: String? = null,
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val overview: String? = null,
    val mediaType: String? = null,
    @JsonSchema.Description("Combined role/job(s) in a filmography context")
    val role: String? = null,
    val tv: Boolean = false,
    val owned: Boolean = false,
    val watched: Boolean = false,
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

@Serializable
data class UpdateSortTitleDto(
    val sortTitle: String,
)
