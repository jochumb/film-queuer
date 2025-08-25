package me.jochum.filmqueuer.domain

import java.time.Instant
import java.util.UUID

abstract class Queue(
    open val id: UUID,
    open val createdAt: Instant = Instant.now(),
)

data class PersonQueue(
    override val id: UUID,
    val personTmdbId: Int,
    override val createdAt: Instant = Instant.now(),
) : Queue(id, createdAt)
