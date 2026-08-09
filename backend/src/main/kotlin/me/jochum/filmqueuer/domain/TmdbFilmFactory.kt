package me.jochum.filmqueuer.domain

import me.jochum.filmqueuer.adapters.tmdb.TmdbAggregateCrewMember
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.adapters.tmdb.TmdbTvDetails
import java.time.LocalDate

/**
 * Maps TMDB movie/TV details onto the domain Film shape. Shared by QueueFilmService (adding a
 * film/TV show directly to a queue) and LetterboxdImportService (linking a TV mini-series match)
 * so the season/episode runtime-summing TV requires isn't duplicated.
 */
class TmdbFilmFactory(
    private val tmdbService: TmdbService,
    private val personRepository: PersonRepository,
) {
    suspend fun createFilm(
        tmdbId: Int,
        tv: Boolean,
    ): Film = if (tv) createTvFilm(tmdbId) else createMovieFilm(tmdbId)

    private suspend fun createMovieFilm(tmdbId: Int): Film {
        val details = tmdbService.getMovieDetails(tmdbId)
        return Film(
            tmdbId = tmdbId,
            title = details.title,
            originalTitle = details.originalTitle,
            releaseDate = details.originalReleaseDate()?.let { LocalDate.parse(it) },
            runtime = details.runtime,
            genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() },
            posterPath = details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            tv = false,
        )
    }

    private suspend fun createTvFilm(tmdbId: Int): Film {
        val details = tmdbService.getTvDetails(tmdbId)
        return Film(
            tmdbId = tmdbId,
            title = details.name,
            originalTitle = details.originalName,
            releaseDate = details.firstAirDate?.let { LocalDate.parse(it) },
            runtime = calculateTotalTvRuntime(tmdbId, details),
            genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() },
            posterPath = details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            tv = true,
            directorTmdbIds = resolveDirectors(details.directorCrew()),
        )
    }

    /**
     * Registers every person credited as a director on the show (co-directed mini-series, or an
     * anthology series with many episode directors, both produce more than one) as a Person,
     * never overwriting one that's already known. Mirrors LetterboxdImportService's movie-crew
     * resolveDirectors, just working off aggregate_credits' shape instead of a single credits list.
     */
    private suspend fun resolveDirectors(crew: List<TmdbAggregateCrewMember>): List<Int> {
        val directors = crew.distinctBy { it.id }
        for (director in directors) {
            if (personRepository.findByTmdbId(director.id) == null) {
                personRepository.save(
                    Person(
                        tmdbId = director.id,
                        name = director.name,
                        department = Department.DIRECTING,
                        imagePath = director.profilePath?.let { "https://image.tmdb.org/t/p/w200$it" },
                    ),
                )
            }
        }
        return directors.map { it.id }
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
}
