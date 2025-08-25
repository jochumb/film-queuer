package me.jochum.filmqueuer.domain

import java.util.UUID

class QueueFilmService(
    private val filmRepository: FilmRepository,
    private val queueFilmRepository: QueueFilmRepository,
) {
    suspend fun addFilmToQueue(
        queueId: UUID,
        film: Film,
    ): QueueFilm {
        // Save film if it doesn't exist
        filmRepository.save(film)

        // Add film to queue
        return queueFilmRepository.addFilmToQueue(queueId, film.tmdbId)
    }

    suspend fun removeFilmFromQueue(
        queueId: UUID,
        filmTmdbId: Int,
    ): Boolean = queueFilmRepository.removeFilmFromQueue(queueId, filmTmdbId)

    suspend fun getQueueFilms(queueId: UUID): List<Film> = queueFilmRepository.findFilmsByQueueId(queueId)

    suspend fun isFilmInQueue(
        queueId: UUID,
        filmTmdbId: Int,
    ): Boolean = queueFilmRepository.isFilmInQueue(queueId, filmTmdbId)
}
