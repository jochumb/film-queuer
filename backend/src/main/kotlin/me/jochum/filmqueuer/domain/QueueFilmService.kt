package me.jochum.filmqueuer.domain

import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.adapters.tmdb.TmdbTvDetails
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
        tv: Boolean = false,
    ): QueueFilm {
        // Create complete film from TMDB ID
        val film = createFilmFromTmdbId(tmdbId, tv)

        // Save film
        filmRepository.save(film)

        // Add film to queue
        return queueFilmRepository.addFilmToQueue(queueId, tmdbId)
    }

    private suspend fun createFilmFromTmdbId(
        tmdbId: Int,
        tv: Boolean = false,
    ): Film {
        return try {
            if (tv) {
                // Fetch TV show details
                val tmdbDetails = tmdbService.getTvDetails(tmdbId)

                // Convert genres list
                val genresList = tmdbDetails.genres.map { it.name }.takeIf { it.isNotEmpty() }

                // Build full poster URL if posterPath is available
                val fullPosterPath = tmdbDetails.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }

                // Calculate total runtime by fetching all season details and summing episode runtimes
                val totalRuntime = calculateTotalTvRuntime(tmdbId, tmdbDetails)

                Film(
                    tmdbId = tmdbId,
                    title = tmdbDetails.name,
                    originalTitle = tmdbDetails.originalName,
                    releaseDate = tmdbDetails.firstAirDate?.let { LocalDate.parse(it) },
                    runtime = totalRuntime,
                    genres = genresList,
                    posterPath = fullPosterPath,
                    tv = true,
                )
            } else {
                // Fetch movie details
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
                    tv = false,
                )
            }
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

    private suspend fun calculateTotalTvRuntime(
        tvId: Int,
        tvDetails: TmdbTvDetails,
    ): Int? {
        return try {
            var totalRuntime = 0

            // Get all seasons (excluding season 0 which is usually specials)
            val seasons = tvDetails.seasons.filter { it.seasonNumber > 0 }

            for (season in seasons) {
                try {
                    val seasonDetails = tmdbService.getTvSeasonDetails(tvId, season.seasonNumber)

                    // Sum up all episode runtimes in this season
                    val seasonRuntime =
                        seasonDetails.episodes.sumOf { episode ->
                            episode.runtime ?: 0
                        }

                    totalRuntime += seasonRuntime
                } catch (e: Exception) {
                    println("Failed to fetch season ${season.seasonNumber} details for TV show $tvId: ${e.message}")
                    // Continue with other seasons even if one fails
                }
            }

            if (totalRuntime > 0) totalRuntime else null
        } catch (e: Exception) {
            println("Failed to calculate total runtime for TV show $tvId: ${e.message}")
            null
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
