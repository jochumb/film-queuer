package me.jochum.filmqueuer.adapters.web

import io.ktor.openapi.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
data class QueueDto(
    @JsonSchema.Format("uuid")
    val id: String,
    val type: String,
    @JsonSchema.Format("date-time")
    val createdAt: String,
    val person: SavedPersonDto? = null,
    val name: String? = null,
    val description: String? = null,
    @JsonSchema.Description(
        "NAMED queues only - a local /images/queue/... path, not the source URL that was provided " +
            "(see PUT /queues/{id}/image-path)",
    )
    val imagePath: String? = null,
    val filmCount: Int = 0,
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

@Serializable
data class CreateNamedQueueDto(
    val name: String,
    val description: String? = null,
)

@Serializable
data class UpdateQueueImageDto(
    val imagePath: String? = null,
)
