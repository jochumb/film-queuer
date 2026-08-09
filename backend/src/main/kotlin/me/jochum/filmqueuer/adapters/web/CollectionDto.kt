package me.jochum.filmqueuer.adapters.web

import kotlinx.serialization.Serializable

@Serializable
data class ExternalFilmRefDto(
    val id: String,
    val source: String,
    val title: String,
    val year: Int?,
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
