package me.jochum.filmqueuer.domain

import kotlinx.coroutines.delay
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import java.time.LocalDate

class FilmEnrichmentService(
    private val filmRepository: FilmRepository,
    private val tmdbService: TmdbService,
) {
    /**
     * Enriches existing films in the database with missing TMDB data
     * Currently focuses on missing runtime, genres, and poster paths
     */
    suspend fun enrichFilmsWithMissingData(): FilmEnrichmentResult {
        val allFilms = filmRepository.findAll()
        val filmsToEnrich =
            allFilms.filter { film ->
                film.runtime == null ||
                    film.genres.isNullOrEmpty() ||
                    film.posterPath.isNullOrBlank()
            }

        if (filmsToEnrich.isEmpty()) {
            return FilmEnrichmentResult(
                totalFilms = allFilms.size,
                filmsToEnrich = 0,
                enrichedFilms = 0,
                failedFilms = 0,
                message = "No films need enrichment",
            )
        }

        println("Found ${filmsToEnrich.size} films that need enrichment out of ${allFilms.size} total films")

        var enrichedCount = 0
        var failedCount = 0

        filmsToEnrich.forEachIndexed { index, film ->
            try {
                println("Enriching film ${index + 1}/${filmsToEnrich.size}: ${film.title} (TMDB ID: ${film.tmdbId})")

                // Get film details from TMDB
                val tmdbDetails = tmdbService.getMovieDetails(film.tmdbId)

                // Prepare enriched data
                val genresList = tmdbDetails.genres.map { it.name }.takeIf { it.isNotEmpty() }
                val fullPosterPath = tmdbDetails.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
                val releaseDate = tmdbDetails.releaseDate?.let { LocalDate.parse(it) }

                // Create enriched film, keeping existing data where new data is null
                val enrichedFilm =
                    film.copy(
                        title = tmdbDetails.title.ifBlank { film.title },
                        originalTitle = tmdbDetails.originalTitle ?: film.originalTitle,
                        releaseDate = releaseDate ?: film.releaseDate,
                        runtime = tmdbDetails.runtime ?: film.runtime,
                        genres = genresList ?: film.genres,
                        posterPath = fullPosterPath ?: film.posterPath,
                    )

                // Update enriched film (use update to avoid foreign key issues)
                val updated = filmRepository.update(enrichedFilm)
                if (updated) {
                    enrichedCount++
                } else {
                    // Film doesn't exist, save as new
                    filmRepository.save(enrichedFilm)
                    enrichedCount++
                }

                val enrichedFields = mutableListOf<String>()
                if (film.runtime == null && tmdbDetails.runtime != null) enrichedFields.add("runtime")
                if (film.genres.isNullOrEmpty() && genresList != null) enrichedFields.add("genres")
                if (film.posterPath.isNullOrBlank() && fullPosterPath != null) enrichedFields.add("poster")
                if (film.releaseDate == null && releaseDate != null) enrichedFields.add("release date")
                if (film.originalTitle.isNullOrBlank() && tmdbDetails.originalTitle != null) enrichedFields.add("original title")

                if (enrichedFields.isNotEmpty()) {
                    println("  ✓ Added: ${enrichedFields.joinToString(", ")}")
                } else {
                    println("  ℹ️  No new data available")
                }

                // Rate limiting - be respectful to TMDB API
                if (index < filmsToEnrich.size - 1) {
                    delay(250) // 250ms delay between requests (4 requests per second max)
                }
            } catch (e: Exception) {
                failedCount++
                println("  ✗ Failed to enrich ${film.title}: ${e.message}")
            }
        }

        return FilmEnrichmentResult(
            totalFilms = allFilms.size,
            filmsToEnrich = filmsToEnrich.size,
            enrichedFilms = enrichedCount,
            failedFilms = failedCount,
            message = "Enrichment completed: $enrichedCount enriched, $failedCount failed",
        )
    }
}

data class FilmEnrichmentResult(
    val totalFilms: Int,
    val filmsToEnrich: Int,
    val enrichedFilms: Int,
    val failedFilms: Int,
    val message: String,
)
