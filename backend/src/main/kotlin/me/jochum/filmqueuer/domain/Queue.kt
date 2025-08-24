package me.jochum.filmqueuer.domain

import java.time.LocalDateTime
import java.util.UUID

abstract class Queue(
    open val id: UUID,
    open val createdAt: LocalDateTime = LocalDateTime.now(),
)

data class PersonQueue(
    override val id: UUID,
    val personTmdbId: Int,
    override val createdAt: LocalDateTime = LocalDateTime.now(),
) : Queue(id, createdAt)
