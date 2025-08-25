package me.jochum.filmqueuer.domain

import java.util.UUID

interface QueueFilmRepository {
    suspend fun addFilmToQueue(
        queueId: UUID,
        filmTmdbId: Int,
    ): QueueFilm

    suspend fun removeFilmFromQueue(
        queueId: UUID,
        filmTmdbId: Int,
    ): Boolean

    suspend fun findFilmsByQueueId(queueId: UUID): List<Film>

    suspend fun isFilmInQueue(
        queueId: UUID,
        filmTmdbId: Int,
    ): Boolean
}
