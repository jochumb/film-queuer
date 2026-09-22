package me.jochum.filmqueuer.domain

import java.util.UUID

class QueueFilmService(
    private val filmRepository: FilmRepository,
    private val queueFilmRepository: QueueFilmRepository,
    private val filmFactory: TmdbFilmFactory,
) {
    suspend fun addFilmToQueue(
        queueId: UUID,
        tmdbId: Int,
        tv: Boolean = false,
    ): QueueFilm {
        // Create complete film from TMDB ID
        val film = createFilmFromTmdbId(tmdbId, tv)

        // Save film
        val saved = filmRepository.save(film)

        // Add film to queue
        return queueFilmRepository.addFilmToQueue(queueId, saved.id)
    }

    private suspend fun createFilmFromTmdbId(
        tmdbId: Int,
        tv: Boolean = false,
    ): Film {
        return try {
            filmFactory.createFilm(tmdbId, tv)
        } catch (e: Exception) {
            println("Failed to create ${if (tv) "TV show" else "film"} from TMDB ID $tmdbId: ${e.message}")
            // Return minimal film if TMDB fetch fails
            Film(
                tmdbId = tmdbId,
                title = if (tv) "Unknown TV Show" else "Unknown Film",
                originalTitle = null,
                releaseDate = null,
                runtime = null,
                genres = null,
                posterPath = null,
                tv = tv,
            )
        }
    }

    suspend fun removeFilmFromQueue(
        queueId: UUID,
        filmId: UUID,
    ): Boolean = queueFilmRepository.removeFilmFromQueue(queueId, filmId)

    suspend fun getQueueFilms(queueId: UUID): List<Film> = queueFilmRepository.findFilmsByQueueId(queueId)

    suspend fun isFilmInQueue(
        queueId: UUID,
        filmId: UUID,
    ): Boolean = queueFilmRepository.isFilmInQueue(queueId, filmId)

    suspend fun reorderQueueFilms(
        queueId: UUID,
        filmOrder: List<UUID>,
    ): Boolean = queueFilmRepository.reorderQueueFilms(queueId, filmOrder)

    suspend fun clearQueue(queueId: UUID): Boolean = queueFilmRepository.deleteAllForQueue(queueId)
}
