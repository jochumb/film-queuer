package me.jochum.filmqueuer.domain

import java.time.LocalDateTime
import java.util.UUID

data class QueueFilm(
    val queueId: UUID,
    val filmTmdbId: Int,
    val addedAt: LocalDateTime = LocalDateTime.now(),
)
