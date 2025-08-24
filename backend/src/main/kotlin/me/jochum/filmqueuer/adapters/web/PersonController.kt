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
import me.jochum.filmqueuer.domain.Person
import me.jochum.filmqueuer.domain.PersonRepository

fun Route.configurePersonRoutes(
    tmdbService: TmdbService,
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
                val person =
                    Person(
                        tmdbId = personDto.tmdbId,
                        name = personDto.name,
                        department = Department.fromString(personDto.department),
                    )

                val savedPerson = personRepository.save(person)
                call.respond(HttpStatusCode.Created, "Person saved successfully")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to save person: ${e.message}")
            }
        }

        get {
            try {
                val persons = personRepository.findAll()
                val result =
                    persons.map { person ->
                        SavedPersonDto(
                            tmdbId = person.tmdbId,
                            name = person.name,
                            department = person.department.name,
                        )
                    }
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch persons: ${e.message}")
            }
        }
    }
}
