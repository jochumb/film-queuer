package me.jochum.filmqueuer.adapters.web

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class FilmRequestDto(
    val tmdbId: Int,
    val tv: Boolean = false,
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
    @JsonSchema.Format("date")
    val releaseDate: String? = null,
    val runtime: Int? = null,
    val genres: List<String>? = null,
    val posterPath: String? = null,
    val directors: List<DirectorDto> = emptyList(),
    val sortTitle: String? = null,
    val owned: Boolean = false,
    val watched: Boolean = false,
)

@Serializable
data class DirectorDto(
    val tmdbId: Int,
    val name: String,
    val sortName: String,
)

@Serializable
data class ReorderFilmsDto(
    val filmOrder: List<Int>,
)
