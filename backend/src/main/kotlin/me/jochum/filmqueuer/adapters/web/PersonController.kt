package me.jochum.filmqueuer.adapters.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.domain.Department
import me.jochum.filmqueuer.domain.PersonRepository
import me.jochum.filmqueuer.domain.PersonSelectionService

fun Route.configurePersonRoutes(
    tmdbService: TmdbService,
    personSelectionService: PersonSelectionService,
    personRepository: PersonRepository,
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
                        imagePath = personDto.imagePath,
                    )

                val response =
                    PersonSelectionResponseDto(
                        person =
                            SavedPersonDto(
                                tmdbId = result.person.tmdbId,
                                name = result.person.name,
                                department = result.person.department.name,
                                imagePath = result.person.imagePath,
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

                // Determine available departments from credits
                val availableDepartments = mutableSetOf<Department>()

                // Check if they have acting credits
                if (credits.cast.isNotEmpty()) {
                    availableDepartments.add(Department.ACTING)
                }

                // Check crew credits for directing and writing
                credits.crew.forEach { crewCredit ->
                    when (crewCredit.department?.lowercase()) {
                        "directing" -> availableDepartments.add(Department.DIRECTING)
                        "writing" -> availableDepartments.add(Department.WRITING)
                        else -> {
                            // Check specific jobs for other departments
                            when (crewCredit.job?.lowercase()) {
                                "director" -> availableDepartments.add(Department.DIRECTING)
                                "writer", "screenplay", "story" -> availableDepartments.add(Department.WRITING)
                            }
                        }
                    }
                }

                // Always include OTHER as a fallback option
                availableDepartments.add(Department.OTHER)

                val films =
                    when (Department.fromString(department)) {
                        Department.ACTING ->
                            credits.cast
                                .groupBy { it.id }
                                .map { (_, credits) ->
                                    val firstCredit = credits.first()
                                    val combinedRoles = credits.mapNotNull { it.character }.distinct().joinToString(", ")
                                    FilmDto(
                                        id = firstCredit.id,
                                        title = firstCredit.title ?: firstCredit.name ?: "Unknown",
                                        originalTitle = firstCredit.originalTitle ?: firstCredit.originalName,
                                        releaseDate = firstCredit.releaseDate ?: firstCredit.firstAirDate,
                                        posterPath = firstCredit.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                                        voteAverage = firstCredit.voteAverage,
                                        voteCount = firstCredit.voteCount,
                                        overview = firstCredit.overview,
                                        mediaType = firstCredit.mediaType,
                                        role = combinedRoles.takeIf { it.isNotBlank() },
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
                                .groupBy { it.id }
                                .map { (_, credits) ->
                                    val firstCredit = credits.first()
                                    val combinedJobs = credits.mapNotNull { it.job }.distinct().joinToString(", ")
                                    FilmDto(
                                        id = firstCredit.id,
                                        title = firstCredit.title ?: firstCredit.name ?: "Unknown",
                                        originalTitle = firstCredit.originalTitle ?: firstCredit.originalName,
                                        releaseDate = firstCredit.releaseDate ?: firstCredit.firstAirDate,
                                        posterPath = firstCredit.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                                        voteAverage = firstCredit.voteAverage,
                                        voteCount = firstCredit.voteCount,
                                        overview = firstCredit.overview,
                                        mediaType = firstCredit.mediaType,
                                        role = combinedJobs.takeIf { it.isNotBlank() },
                                    )
                                }
                    }

                call.respond(
                    FilmographyDto(
                        films = films.sortedByDescending { it.releaseDate },
                        availableDepartments = availableDepartments.map { it.name }.sorted(),
                    ),
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch filmography: ${e.message}")
            }
        }

        put("/{tmdbId}/department") {
            val tmdbId = call.parameters["tmdbId"]?.toIntOrNull()
            if (tmdbId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid tmdbId parameter")
                return@put
            }

            try {
                val updateDto = call.receive<UpdateDepartmentDto>()
                val department = Department.fromString(updateDto.department)

                val updated = personRepository.updateDepartment(tmdbId, department)
                if (updated) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Person not found")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update department: ${e.message}")
            }
        }
    }
}
