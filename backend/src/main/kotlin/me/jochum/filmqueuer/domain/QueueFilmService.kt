package me.jochum.filmqueuer.domain

import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import java.time.LocalDate
import java.util.UUID

class QueueFilmService(
    private val filmRepository: FilmRepository,
    private val queueFilmRepository: QueueFilmRepository,
    private val tmdbService: TmdbService,
) {
    suspend fun addFilmToQueue(
        queueId: UUID,
        tmdbId: Int,
    ): QueueFilm {
        // Create complete film from TMDB ID
        val film = createFilmFromTmdbId(tmdbId)

        // Save film
        filmRepository.save(film)

        // Add film to queue
        return queueFilmRepository.addFilmToQueue(queueId, tmdbId)
    }

    private suspend fun createFilmFromTmdbId(tmdbId: Int): Film {
        return try {
            val tmdbDetails = tmdbService.getMovieDetails(tmdbId)

            // Convert genres list
            val genresList = tmdbDetails.genres.map { it.name }.takeIf { it.isNotEmpty() }

            // Build full poster URL if posterPath is available
            val fullPosterPath = tmdbDetails.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

            Film(
                tmdbId = tmdbId,
                title = tmdbDetails.title,
                originalTitle = tmdbDetails.originalTitle,
                releaseDate = tmdbDetails.releaseDate?.let { LocalDate.parse(it) },
                runtime = tmdbDetails.runtime,
                genres = genresList,
                posterPath = fullPosterPath,
            )
        } catch (e: Exception) {
            println("Failed to create film from TMDB ID $tmdbId: ${e.message}")
            // Return minimal film if TMDB fetch fails
            Film(
                tmdbId = tmdbId,
                title = "Unknown Film",
                originalTitle = null,
                releaseDate = null,
                runtime = null,
                genres = null,
                posterPath = null,
            )
        }
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

    suspend fun reorderQueueFilms(
        queueId: UUID,
        filmOrder: List<Int>,
    ): Boolean = queueFilmRepository.reorderQueueFilms(queueId, filmOrder)
}
