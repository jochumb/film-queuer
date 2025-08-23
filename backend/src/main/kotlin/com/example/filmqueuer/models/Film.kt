package com.example.filmqueuer.models

import kotlinx.serialization.Serializable

@Serializable
data class Film(
    val id: Int,
    val title: String,
    val director: String,
    val year: Int,
    val watched: Boolean = false
)