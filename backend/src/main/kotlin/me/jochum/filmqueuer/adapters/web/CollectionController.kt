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
    tmdbId = tmdbId,
    title = title,
    originalTitle = originalTitle,
    releaseDate = releaseDate.toDateString(),
    runtime = runtime,
    genres = genres,
    posterPath = posterPath,
    directors = directorTmdbIds.mapNotNull { directorsByTmdbId[it] },
    sortTitle = sortTitle ?: title,
    owned = owned,
    watched = watched,
)

private fun ExternalFilmRef.toDto(
    filmsByTmdbId: Map<Int, Film>,
    directorsByTmdbId: Map<Int, DirectorDto>,
) = ExternalFilmRefDto(
    id = id.toString(),
    source = source,
    title = title,
    year = year,
    filmTmdbId = filmTmdbId,
    owned = owned,
    watched = watched,
    removed = removed,
    film = filmTmdbId?.let { filmsByTmdbId[it]?.toResponseDto(directorsByTmdbId, owned, watched) },
)

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
                val filmsByTmdbId =
                    page.mapNotNull { it.filmTmdbId }
                        .toSet()
                        .mapNotNull { tmdbId -> filmRepository.findByTmdbId(tmdbId)?.let { tmdbId to it } }
                        .toMap()
                val directorsByTmdbId = resolveDirectors(filmsByTmdbId.values, personRepository)

                call.respond(
                    CollectionPageDto(
                        items = page.map { it.toDto(filmsByTmdbId, directorsByTmdbId) },
                        total = total,
                        offset = offset,
                        limit = limit,
                    ),
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch collection: ${e.message}")
            }
        }

        get("/random-picks") {
            try {
                val ownedParam = call.request.queryParameters["owned"]?.toBooleanStrictOrNull() ?: true
                val watchedParam = call.request.queryParameters["watched"]?.toBooleanStrictOrNull() ?: false
                val maxRuntimeParam = call.request.queryParameters["maxRuntime"]?.toIntOrNull() ?: 100
                val count = (call.request.queryParameters["count"]?.toIntOrNull() ?: 3).coerceIn(1, 20)

                val picks = externalFilmRefRepository.findRandomPicks(ownedParam, watchedParam, maxRuntimeParam, count)
                val filmsByTmdbId =
                    picks.mapNotNull { it.filmTmdbId }
                        .toSet()
                        .mapNotNull { tmdbId -> filmRepository.findByTmdbId(tmdbId)?.let { tmdbId to it } }
                        .toMap()
                val directorsByTmdbId = resolveDirectors(filmsByTmdbId.values, personRepository)

                call.respond(picks.map { it.toDto(filmsByTmdbId, directorsByTmdbId) })
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch random picks: ${e.message}")
            }
        }

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
                    val film = filmRepository.findByTmdbId(linkRequest.tmdbId)
                    val directorsByTmdbId = resolveDirectors(listOfNotNull(film), personRepository)
                    call.respond(updated.toDto(film?.let { mapOf(it.tmdbId to it) } ?: emptyMap(), directorsByTmdbId))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid id: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to link film: ${e.message}")
            }
        }
    }
}
