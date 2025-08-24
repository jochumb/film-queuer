package me.jochum.filmqueuer.adapters.web

import kotlinx.serialization.Serializable

@Serializable
data class PersonSearchResultDto(
    val results: List<PersonDto>,
)

@Serializable
data class PersonDto(
    val id: Int,
    val name: String,
    val department: String? = null,
    val profilePath: String? = null,
    val popularity: Double = 0.0,
    val knownFor: List<String> = emptyList(),
)

@Serializable
data class PersonSelectionDto(
    val tmdbId: Int,
    val name: String,
    val department: String?,
)

@Serializable
data class SavedPersonDto(
    val tmdbId: Int,
    val name: String,
    val department: String,
)

@Serializable
data class PersonSelectionResponseDto(
    val person: SavedPersonDto,
    val queueId: String,
)
