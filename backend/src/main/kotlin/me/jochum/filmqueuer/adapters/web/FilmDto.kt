package me.jochum.filmqueuer.adapters.web

import kotlinx.serialization.Serializable

@Serializable
data class FilmDto(
    val id: Int,
    val title: String,
    val director: String,
    val year: Int,
    val watched: Boolean = false
)