package me.jochum.filmqueuer.adapters.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.domain.Department
import me.jochum.filmqueuer.domain.PersonSelectionService

fun Route.configurePersonRoutes(
    tmdbService: TmdbService,
    personSelectionService: PersonSelectionService,
) {
    route("/persons") {
        get("/search") {
            val query = call.request.queryParameters["q"]
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Query parameter 'q' is required")
                return@get
            }

            try {
                val tmdbResponse = tmdbService.searchPerson(query)
                val result =
                    PersonSearchResultDto(
                        results =
                            tmdbResponse.results
                                .sortedByDescending { it.popularity }
                                .take(5)
                                .map { person ->
                                    PersonDto(
                                        id = person.id,
                                        name = person.name,
                                        department = person.knownForDepartment,
                                        profilePath = person.profilePath?.let { "https://image.tmdb.org/t/p/w200$it" },
                                        popularity = person.popularity,
                                        knownFor = person.knownFor.mapNotNull { it.title ?: it.name },
                                    )
                                },
                    )
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to search persons: ${e.message}")
            }
        }

        post("/select") {
            try {
                val personDto = call.receive<PersonSelectionDto>()

                val result =
                    personSelectionService.selectPerson(
                        tmdbId = personDto.tmdbId,
                        name = personDto.name,
                        department = Department.fromString(personDto.department),
                    )

                val response =
                    PersonSelectionResponseDto(
                        person =
                            SavedPersonDto(
                                tmdbId = result.person.tmdbId,
                                name = result.person.name,
                                department = result.person.department.name,
                            ),
                        queueId = result.queue.id.toString(),
                    )

                call.respond(HttpStatusCode.Created, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to save person: ${e.message}")
            }
        }

        get("/{tmdbId}/filmography") {
            val tmdbId = call.parameters["tmdbId"]?.toIntOrNull()
            if (tmdbId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid tmdbId parameter")
                return@get
            }

            val department = call.request.queryParameters["department"]
            if (department.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Query parameter 'department' is required")
                return@get
            }

            try {
                val credits = tmdbService.getPersonMovieCredits(tmdbId)

                val films =
                    when (Department.fromString(department)) {
                        Department.ACTING ->
                            credits.cast.map { credit ->
                                FilmDto(
                                    id = credit.id,
                                    title = credit.title ?: credit.name ?: "Unknown",
                                    originalTitle = credit.originalTitle ?: credit.originalName,
                                    releaseDate = credit.releaseDate ?: credit.firstAirDate,
                                    posterPath = credit.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                                    voteAverage = credit.voteAverage,
                                    voteCount = credit.voteCount,
                                    overview = credit.overview,
                                    mediaType = credit.mediaType,
                                    role = credit.character,
                                )
                            }
                        Department.DIRECTING, Department.WRITING, Department.OTHER ->
                            credits.crew
                                .filter { credit ->
                                    when (Department.fromString(department)) {
                                        Department.DIRECTING -> credit.department == "Directing"
                                        Department.WRITING -> credit.department == "Writing"
                                        else -> true
                                    }
                                }
                                .map { credit ->
                                    FilmDto(
                                        id = credit.id,
                                        title = credit.title ?: credit.name ?: "Unknown",
                                        originalTitle = credit.originalTitle ?: credit.originalName,
                                        releaseDate = credit.releaseDate ?: credit.firstAirDate,
                                        posterPath = credit.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                                        voteAverage = credit.voteAverage,
                                        voteCount = credit.voteCount,
                                        overview = credit.overview,
                                        mediaType = credit.mediaType,
                                        role = credit.job,
                                    )
                                }
                    }

                call.respond(FilmographyDto(films = films.sortedByDescending { it.releaseDate }))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch filmography: ${e.message}")
            }
        }
    }
}
