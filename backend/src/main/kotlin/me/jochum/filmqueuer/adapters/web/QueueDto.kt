package me.jochum.filmqueuer.adapters.web

import kotlinx.serialization.Serializable

@Serializable
data class QueueDto(
    val id: String,
    val type: String,
    val createdAt: String,
    val person: SavedPersonDto? = null,
)

@Serializable
data class ReorderQueuesDto(
    val queueOrder: List<String>,
)

@Serializable
data class QueuePreviewDto(
    val queue: QueueDto,
    val films: List<FilmResponseDto>,
    val totalFilms: Int,
)

@Serializable
data class QueuePreviewsDto(
    val previews: List<QueuePreviewDto>,
)
