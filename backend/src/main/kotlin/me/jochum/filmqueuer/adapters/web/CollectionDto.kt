package me.jochum.filmqueuer.adapters.web

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class ExternalFilmRefDto(
    @JsonSchema.Format("uuid")
    val id: String,
    val source: String,
    val title: String,
    val year: Int?,
    @JsonSchema.Description("Null until matched to a TMDB film")
    val filmTmdbId: Int?,
    val owned: Boolean,
    val watched: Boolean,
    val removed: Boolean = false,
    val film: FilmResponseDto? = null,
)

@Serializable
data class CollectionPageDto(
    val items: List<ExternalFilmRefDto>,
    val total: Int,
    val offset: Int,
    val limit: Int,
)

@Serializable
data class ImportSummaryDto(
    val totalRows: Int,
    val created: Int,
    val updated: Int,
    val autoMatched: Int,
    val unmatched: List<String>,
)

@Serializable
data class LinkFilmDto(
    val tmdbId: Int,
    val tv: Boolean = false,
)

@Serializable
data class UpdateRemovedDto(
    val removed: Boolean,
)
