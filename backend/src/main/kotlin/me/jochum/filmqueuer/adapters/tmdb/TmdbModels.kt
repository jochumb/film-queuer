package me.jochum.filmqueuer.adapters.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbPersonSearchResponse(
    val page: Int,
    val results: List<TmdbPerson>,
    @SerialName("total_pages")
    val totalPages: Int,
    @SerialName("total_results")
    val totalResults: Int
)

@Serializable
data class TmdbPerson(
    val id: Int,
    val name: String,
    @SerialName("known_for_department")
    val knownForDepartment: String? = null,
    @SerialName("profile_path")
    val profilePath: String? = null,
    val popularity: Double = 0.0,
    @SerialName("known_for")
    val knownFor: List<TmdbKnownFor> = emptyList()
)

@Serializable
data class TmdbKnownFor(
    val id: Int,
    val title: String? = null,
    val name: String? = null,
    @SerialName("media_type")
    val mediaType: String,
    @SerialName("release_date")
    val releaseDate: String? = null,
    @SerialName("first_air_date")
    val firstAirDate: String? = null
)