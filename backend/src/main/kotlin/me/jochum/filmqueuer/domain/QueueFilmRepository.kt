package me.jochum.filmqueuer.domain

import java.util.UUID

interface QueueFilmRepository {
    suspend fun addFilmToQueue(
        queueId: UUID,
        filmId: UUID,
    ): QueueFilm

    suspend fun removeFilmFromQueue(
        queueId: UUID,
        filmId: UUID,
    ): Boolean

    suspend fun findFilmsByQueueId(queueId: UUID): List<Film>

    suspend fun isFilmInQueue(
        queueId: UUID,
        filmId: UUID,
    ): Boolean

    suspend fun reorderQueueFilms(
        queueId: UUID,
        filmOrder: List<UUID>,
    ): Boolean

    suspend fun deleteAllForQueue(queueId: UUID): Boolean
}
