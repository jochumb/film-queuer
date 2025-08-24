package me.jochum.filmqueuer.adapters.web

import kotlinx.serialization.Serializable

@Serializable
data class QueueDto(
    val id: String,
    val type: String,
    val createdAt: String,
    val person: SavedPersonDto? = null,
)
