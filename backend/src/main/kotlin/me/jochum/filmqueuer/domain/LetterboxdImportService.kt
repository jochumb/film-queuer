package me.jochum.filmqueuer.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import me.jochum.filmqueuer.adapters.letterboxd.LetterboxdCsvParser
import me.jochum.filmqueuer.adapters.letterboxd.LetterboxdFilmRow
import me.jochum.filmqueuer.adapters.tmdb.TmdbCrewMember
import me.jochum.filmqueuer.adapters.tmdb.TmdbMovie
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import java.sql.SQLTransactionRollbackException
import java.time.LocalDate
import java.util.UUID

private const val LETTERBOXD_SOURCE = "LETTERBOXD"
private const val MATCH_CONCURRENCY = 5
private const val DEADLOCK_MAX_ATTEMPTS = 3
private const val DEADLOCK_RETRY_DELAY_MS = 150L

data class ImportSummary(
    val totalRows: Int,
    val created: Int,
    val updated: Int,
    val autoMatched: Int,
    val unmatched: List<String>,
)

class LetterboxdImportService(
    private val externalFilmRefRepository: ExternalFilmRefRepository,
    private val filmRepository: FilmRepository,
    private val personRepository: PersonRepository,
    private val tmdbService: TmdbService,
    private val filmFactory: TmdbFilmFactory,
) {
    suspend fun importCollection(csvContent: String): ImportSummary =
        importRows(LetterboxdCsvParser.parseListExport(csvContent), markOwned = true, markWatched = false)

    suspend fun importWatched(csvContent: String): ImportSummary =
        importRows(
            LetterboxdCsvParser.parseWatchedExport(csvContent),
            markOwned = false,
            markWatched = true,
        )

    suspend fun linkManually(
        refId: UUID,
        tmdbId: Int,
        tv: Boolean = false,
    ): ExternalFilmRef? {
        val ref = externalFilmRefRepository.findById(refId) ?: return null
        return applyMatch(ref, tmdbId, tv)
    }

    // MATCH_CONCURRENCY concurrent transactions writing to persons/films/film_directors can hit
    // transient InnoDB deadlocks (e.g. a REPLACE INTO persons racing an insert into
    // film_directors that FK-references it). These are safe to retry - MySQL guarantees one
    // side of the deadlock is rolled back cleanly - so retry a few times before giving up.
    private suspend fun <T> retryOnDeadlock(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: Exception) {
                attempt++
                if (!isDeadlock(e) || attempt >= DEADLOCK_MAX_ATTEMPTS) throw e
                delay(DEADLOCK_RETRY_DELAY_MS * attempt)
            }
        }
    }

    private fun isDeadlock(e: Throwable): Boolean {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is SQLTransactionRollbackException) return true
            cause = cause.cause
        }
        return false
    }

    private suspend fun importRows(
        rows: List<LetterboxdFilmRow>,
        markOwned: Boolean,
        markWatched: Boolean,
    ): ImportSummary {
        var created = 0
        var updated = 0
        val needsMatch = mutableListOf<ExternalFilmRef>()

        for (row in rows) {
            val existing = externalFilmRefRepository.findBySourceTitleYear(LETTERBOXD_SOURCE, row.title, row.year)
            if (existing == null) {
                val ref =
                    ExternalFilmRef(
                        id = UUID.randomUUID(),
                        source = LETTERBOXD_SOURCE,
                        title = row.title,
                        year = row.year,
                        owned = markOwned,
                        watched = markWatched,
                    )
                externalFilmRefRepository.save(ref)
                created++
                needsMatch.add(ref)
            } else {
                val merged = existing.copy(owned = existing.owned || markOwned, watched = existing.watched || markWatched)
                if (merged != existing) {
                    externalFilmRefRepository.update(merged)
                    updated++
                }
                if (existing.filmTmdbId == null) {
                    needsMatch.add(merged)
                }
            }
        }

        val matched = matchAll(needsMatch)

        return ImportSummary(
            totalRows = rows.size,
            created = created,
            updated = updated,
            autoMatched = matched.count { it },
            unmatched =
                needsMatch.filterIndexed { index, _ -> !matched[index] }
                    .map { "${it.title} (${it.year ?: "?"})" },
        )
    }

    private suspend fun matchAll(refs: List<ExternalFilmRef>): List<Boolean> =
        coroutineScope {
            val semaphore = Semaphore(MATCH_CONCURRENCY)
            refs.map { ref ->
                async { semaphore.withPermit { tryAutoMatch(ref) } }
            }.map { it.await() }
        }

    private suspend fun tryAutoMatch(ref: ExternalFilmRef): Boolean {
        return try {
            // Narrowing the search itself by year (not just filtering the results afterward)
            // matters: TMDB ranks by popularity, so a low-profile film sharing a title with a
            // well-known one can be pushed off the results entirely without this.
            val candidates =
                tmdbService.searchMovies(ref.title, ref.year).results.filter { movie ->
                    movie.title.equals(ref.title, ignoreCase = true) &&
                        (ref.year == null || movie.releaseDate?.take(4)?.toIntOrNull() == ref.year)
                }
            val match = pickBestCandidate(candidates) ?: return false
            applyMatch(ref, match.id, tv = false) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * When multiple TMDB results share the exact title and year (e.g. a well-known film and an
     * obscure, unrelated one released the same year with the same title), prefer the one with
     * more votes rather than declining outright — but only when there's a clear winner. A tie
     * (e.g. both unreleased/zero votes) is left for manual review rather than guessed.
     */
    private fun pickBestCandidate(candidates: List<TmdbMovie>): TmdbMovie? {
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.single()

        val ranked = candidates.sortedByDescending { it.voteCount }
        return if (ranked[0].voteCount == ranked[1].voteCount) null else ranked[0]
    }

    private suspend fun applyMatch(
        ref: ExternalFilmRef,
        tmdbId: Int,
        tv: Boolean,
    ): ExternalFilmRef? {
        return try {
            retryOnDeadlock {
                // TV matches route through TmdbFilmFactory, which resolves directors from
                // aggregate_credits (ranked by episode count, since TV doesn't have movies' single
                // top-level `credits` list). The movie path stays on its own TmdbMovieDetails
                // fetch, rather than routing through the factory too, because it needs the raw
                // `credits` field here for resolveDirectors below.
                val film =
                    if (tv) {
                        filmFactory.createFilm(tmdbId, tv = true)
                    } else {
                        val details = tmdbService.getMovieDetails(tmdbId)
                        Film(
                            tmdbId = tmdbId,
                            title = details.title,
                            originalTitle = details.originalTitle,
                            releaseDate = details.originalReleaseDate()?.let { LocalDate.parse(it) },
                            runtime = details.runtime,
                            genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() },
                            posterPath = details.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                            tv = false,
                            directorTmdbIds = resolveDirectors(details.credits?.crew ?: emptyList()),
                        )
                    }
                // save() insert-ignores on conflict; a re-link of an already-matched film (e.g. to
                // backfill data added since it was first matched) needs update() to actually apply.
                if (filmRepository.findByTmdbId(tmdbId) == null) {
                    filmRepository.save(film)
                } else {
                    filmRepository.update(film)
                }
                val updatedRef = ref.copy(filmTmdbId = tmdbId)
                externalFilmRefRepository.update(updatedRef)
                updatedRef
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves every director credited on the film to a Person record (co-directed films are
     * common — Coen Brothers, Daniels, etc.), registering any that don't already exist. Never
     * overwrites an existing Person — that row may be curated (e.g. selected by the user as an
     * actor), and PersonRepository.save() fully replaces the row including department.
     */
    private suspend fun resolveDirectors(crew: List<TmdbCrewMember>): List<Int> {
        val directors = crew.filter { it.job == "Director" }.distinctBy { it.id }
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
}
