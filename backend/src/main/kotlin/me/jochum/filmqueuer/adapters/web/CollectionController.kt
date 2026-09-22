package me.jochum.filmqueuer.adapters.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import me.jochum.filmqueuer.domain.CollectionSortField
import me.jochum.filmqueuer.domain.ExternalFilmRef
import me.jochum.filmqueuer.domain.ExternalFilmRefRepository
import me.jochum.filmqueuer.domain.Film
import me.jochum.filmqueuer.domain.FilmRepository
import me.jochum.filmqueuer.domain.ImportSummary
import me.jochum.filmqueuer.domain.LetterboxdImportService
import me.jochum.filmqueuer.domain.PersonRepository
import java.util.UUID

private fun Film.toResponseDto(
    directorsByTmdbId: Map<Int, DirectorDto>,
    owned: Boolean = false,
    watched: Boolean = false,
) = FilmResponseDto(
    id = id.toString(),
    tmdbId = tmdbId,
    title = title,
    originalTitle = originalTitle,
    releaseDate = releaseDate.toDateString(),
    runtime = runtime,
    genres = genres,
    posterPath = posterPath,
    tv = tv,
    directors = directorTmdbIds.mapNotNull { directorsByTmdbId[it] },
    sortTitle = sortTitle ?: title,
    owned = owned,
    watched = watched,
)

private fun ExternalFilmRef.toDto(
    filmsById: Map<UUID, Film>,
    directorsByTmdbId: Map<Int, DirectorDto>,
): ExternalFilmRefDto {
    val film = filmId?.let { filmsById[it] }
    return ExternalFilmRefDto(
        id = id.toString(),
        source = source,
        title = title,
        year = year,
        filmTmdbId = film?.tmdbId,
        owned = owned,
        watched = watched,
        removed = removed,
        film = film?.toResponseDto(directorsByTmdbId, owned, watched),
    )
}

private fun ImportSummary.toDto() =
    ImportSummaryDto(
        totalRows = totalRows,
        created = created,
        updated = updated,
        autoMatched = autoMatched,
        unmatched = unmatched,
    )

private suspend fun resolveDirectors(
    films: Collection<Film>,
    personRepository: PersonRepository,
): Map<Int, DirectorDto> =
    films.flatMap { it.directorTmdbIds }
        .toSet()
        .mapNotNull { personId ->
            personRepository.findByTmdbId(personId)?.let {
                personId to DirectorDto(tmdbId = personId, name = it.name, sortName = it.sortName ?: it.name)
            }
        }
        .toMap()

fun Route.configureCollectionRoutes(
    letterboxdImportService: LetterboxdImportService,
    externalFilmRefRepository: ExternalFilmRefRepository,
    filmRepository: FilmRepository,
    personRepository: PersonRepository,
) {
    route("/collection") {
        /**
         * Tag: Collection
         * Description: Import a Letterboxd "list" (owned) CSV export. Rows are deduplicated by
         *   source/title/year; re-importing merges the owned flag onto an existing row rather than
         *   creating a duplicate. Each row is auto-matched against TMDB, narrowed by year.
         * Body: text/csv [string] Raw CSV file content
         */
        post("/import/letterboxd/owned") {
            try {
                val summary = letterboxdImportService.importCollection(call.receiveText())
                call.respond(summary.toDto())
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid CSV: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to import collection: ${e.message}")
            }
        }

        /**
         * Tag: Collection
         * Description: Import a Letterboxd "watched" CSV export. Same matching/merge behavior as the
         *   owned import, but sets the watched flag instead.
         * Body: text/csv [string] Raw CSV file content
         */
        post("/import/letterboxd/watched") {
            try {
                val summary = letterboxdImportService.importWatched(call.receiveText())
                call.respond(summary.toDto())
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid CSV: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to import watched films: ${e.message}")
            }
        }

        /**
         * Tag: Collection
         * Description: Paginated, filtered, sorted Collection listing, enriched with matched film and
         *   director data. Query params: owned, watched (boolean). unmatched (boolean; true = only rows
         *   with no TMDB match, false = only matched rows). removed (boolean; show soft-deleted rows
         *   instead of visible ones, default false). sort (one of title/year/director/added, default
         *   title). order (asc/desc, default asc). offset/limit (default 0/40, limit max 200). q (title
         *   search, matching either the imported Letterboxd title or the matched film's title).
         */
        get {
            try {
                val ownedParam = call.request.queryParameters["owned"]?.toBooleanStrictOrNull()
                val watchedParam = call.request.queryParameters["watched"]?.toBooleanStrictOrNull()
                val unmatchedParam = call.request.queryParameters["unmatched"]?.toBooleanStrictOrNull()
                val removedParam = call.request.queryParameters["removed"]?.toBooleanStrictOrNull() ?: false
                val offset = (call.request.queryParameters["offset"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 40).coerceIn(1, 200)
                val sortField = CollectionSortField.fromParam(call.request.queryParameters["sort"])
                val sortDescending = call.request.queryParameters["order"] == "desc"
                val queryParam = call.request.queryParameters["q"]?.takeIf { it.isNotBlank() }

                val page =
                    externalFilmRefRepository.findPage(
                        ownedParam,
                        watchedParam,
                        unmatchedParam,
                        sortField,
                        sortDescending,
                        offset,
                        limit,
                        removedParam,
                        queryParam,
                    )
                val total = externalFilmRefRepository.count(ownedParam, watchedParam, unmatchedParam, removedParam, queryParam)
                val filmsById =
                    page.mapNotNull { it.filmId }
                        .toSet()
                        .mapNotNull { id -> filmRepository.findById(id)?.let { id to it } }
                        .toMap()
                val directorsByTmdbId = resolveDirectors(filmsById.values, personRepository)

                call.respond(
                    CollectionPageDto(
                        items = page.map { it.toDto(filmsById, directorsByTmdbId) },
                        total = total,
                        offset = offset,
                        limit = limit,
                    ),
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch collection: ${e.message}")
            }
        }

        /**
         * Tag: Collection
         * Description: Pick N random matched films from the Collection. Backs the home page's
         *   "Tonight's Picks" row. Query params: owned (default true), watched (default false),
         *   maxRuntime (minutes, omit for no cap, default 100), count (default 3, max 20).
         */
        get("/random-picks") {
            try {
                val ownedParam = call.request.queryParameters["owned"]?.toBooleanStrictOrNull() ?: true
                val watchedParam = call.request.queryParameters["watched"]?.toBooleanStrictOrNull() ?: false
                val maxRuntimeParam = call.request.queryParameters["maxRuntime"]?.toIntOrNull() ?: 100
                val count = (call.request.queryParameters["count"]?.toIntOrNull() ?: 3).coerceIn(1, 20)

                val picks = externalFilmRefRepository.findRandomPicks(ownedParam, watchedParam, maxRuntimeParam, count)
                val filmsById =
                    picks.mapNotNull { it.filmId }
                        .toSet()
                        .mapNotNull { id -> filmRepository.findById(id)?.let { id to it } }
                        .toMap()
                val directorsByTmdbId = resolveDirectors(filmsById.values, personRepository)

                call.respond(picks.map { it.toDto(filmsById, directorsByTmdbId) })
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch random picks: ${e.message}")
            }
        }

        /**
         * Tag: Collection
         * Description: Soft-delete or restore a collection row.
         */
        put("/{id}/removed") {
            try {
                val idString = call.parameters["id"]
                if (idString == null) {
                    call.respond(HttpStatusCode.BadRequest, "id is required")
                    return@put
                }

                val id = UUID.fromString(idString)
                val updateDto = call.receive<UpdateRemovedDto>()
                val updated = externalFilmRefRepository.setRemoved(id, updateDto.removed)

                if (updated) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Collection item not found")
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update removed status: ${e.message}")
            }
        }

        /**
         * Tag: Collection
         * Description: Manually link/relink a collection row to a TMDB film. Used by the "Fix match"
         *   flow for auto-match misses or mismatches.
         */
        put("/{id}/link") {
            try {
                val idString = call.parameters["id"]
                if (idString == null) {
                    call.respond(HttpStatusCode.BadRequest, "id is required")
                    return@put
                }

                val id = UUID.fromString(idString)
                val linkRequest = call.receive<LinkFilmDto>()
                val updated = letterboxdImportService.linkManually(id, linkRequest.tmdbId, linkRequest.tv)

                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound, "Collection item not found")
                } else {
                    val film = filmRepository.findByTmdbId(linkRequest.tmdbId, linkRequest.tv)
                    val directorsByTmdbId = resolveDirectors(listOfNotNull(film), personRepository)
                    call.respond(updated.toDto(film?.let { mapOf(it.id to it) } ?: emptyMap(), directorsByTmdbId))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to link film: ${e.message}")
            }
        }
    }
}
