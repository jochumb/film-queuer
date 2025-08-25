package me.jochum.filmqueuer.domain

import java.time.Instant
import java.util.UUID

data class QueueFilm(
    val queueId: UUID,
    val filmTmdbId: Int,
    val addedAt: Instant = Instant.now(),
    val sortOrder: Int = 0,
)
