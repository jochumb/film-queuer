package me.jochum.filmqueuer.domain

import java.time.Instant
import java.util.UUID

data class ExternalFilmRef(
    val id: UUID,
    val source: String,
    val title: String,
    val year: Int?,
    val filmTmdbId: Int? = null,
    val owned: Boolean = false,
    val watched: Boolean = false,
    val createdAt: Instant = Instant.now(),
    // Soft-delete flag: hides the item from the Collection page without deleting the row, so a
    // re-imported Letterboxd export (which still lists the film) doesn't resurrect it.
    val removed: Boolean = false,
)
