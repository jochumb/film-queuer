package me.jochum.filmqueuer.adapters.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import me.jochum.filmqueuer.domain.PersonQueue
import me.jochum.filmqueuer.domain.PersonRepository
import me.jochum.filmqueuer.domain.QueueFilmService
import me.jochum.filmqueuer.domain.QueueRepository
import java.util.UUID

private suspend fun mapQueueToDto(
    queue: me.jochum.filmqueuer.domain.Queue,
    personRepository: PersonRepository,
): QueueDto {
    return when (queue) {
        is PersonQueue -> {
            val person = personRepository.findByTmdbId(queue.personTmdbId)
            QueueDto(
                id = queue.id.toString(),
                type = "PERSON",
                createdAt = queue.createdAt.toString(),
                person =
                    person?.let {
                        SavedPersonDto(
                            tmdbId = it.tmdbId,
                            name = it.name,
                            department = it.department.name,
                            imagePath = it.imagePath,
                        )
                    },
            )
        }
        else ->
            QueueDto(
                id = queue.id.toString(),
                type = "UNKNOWN",
                createdAt = queue.createdAt.toString(),
                person = null,
            )
    }
}

fun Route.configureQueueRoutes(
    queueRepository: QueueRepository,
    personRepository: PersonRepository,
    queueFilmService: QueueFilmService,
) {
    route("/queues") {
        get {
            try {
                val queues = queueRepository.findAll()
                val result = queues.map { queue -> mapQueueToDto(queue, personRepository) }
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch queues: ${e.message}")
            }
        }

        get("/{queueId}") {
            try {
                val queueIdString = call.parameters["queueId"]
                if (queueIdString == null) {
                    call.respond(HttpStatusCode.BadRequest, "Queue ID is required")
                    return@get
                }

                val queueId = UUID.fromString(queueIdString)
                val queue = queueRepository.findById(queueId)

                if (queue == null) {
                    call.respond(HttpStatusCode.NotFound, "Queue not found")
                    return@get
                }

                val result = mapQueueToDto(queue, personRepository)
                call.respond(result)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch queue: ${e.message}")
            }
        }

        post("/{queueId}/films") {
            try {
                val queueIdString = call.parameters["queueId"]
                if (queueIdString == null) {
                    call.respond(HttpStatusCode.BadRequest, "Queue ID is required")
                    return@post
                }

                val queueId = UUID.fromString(queueIdString)
                val filmRequest = call.receive<FilmRequestDto>()

                queueFilmService.addFilmToQueue(queueId, filmRequest.tmdbId)
                call.respond(HttpStatusCode.Created, "Film added to queue successfully")
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to add film to queue: ${e.message}")
            }
        }

        get("/{queueId}/films") {
            try {
                val queueIdString = call.parameters["queueId"]
                if (queueIdString == null) {
                    call.respond(HttpStatusCode.BadRequest, "Queue ID is required")
                    return@get
                }

                val queueId = UUID.fromString(queueIdString)
                val films = queueFilmService.getQueueFilms(queueId)

                val response =
                    QueueFilmsDto(
                        films =
                            films.map { film ->
                                FilmResponseDto(
                                    tmdbId = film.tmdbId,
                                    title = film.title,
                                    originalTitle = film.originalTitle,
                                    releaseDate = film.releaseDate.toDateString(),
                                    runtime = film.runtime,
                                    genres = film.genres,
                                    posterPath = film.posterPath,
                                )
                            },
                    )

                call.respond(response)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch queue films: ${e.message}")
            }
        }

        delete("/{queueId}/films/{filmTmdbId}") {
            try {
                val queueIdString = call.parameters["queueId"]
                val filmTmdbIdString = call.parameters["filmTmdbId"]

                if (queueIdString == null || filmTmdbIdString == null) {
                    call.respond(HttpStatusCode.BadRequest, "Queue ID and Film TMDB ID are required")
                    return@delete
                }

                val queueId = UUID.fromString(queueIdString)
                val filmTmdbId = filmTmdbIdString.toInt()

                val removed = queueFilmService.removeFilmFromQueue(queueId, filmTmdbId)

                if (removed) {
                    call.respond(HttpStatusCode.OK, "Film removed from queue successfully")
                } else {
                    call.respond(HttpStatusCode.NotFound, "Film not found in queue")
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID or film TMDB ID: ${e.message}")
            } catch (e: NumberFormatException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid film TMDB ID format: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to remove film from queue: ${e.message}")
            }
        }

        put("/{queueId}/films/reorder") {
            try {
                val queueIdString = call.parameters["queueId"]
                if (queueIdString == null) {
                    call.respond(HttpStatusCode.BadRequest, "Queue ID is required")
                    return@put
                }

                val queueId = UUID.fromString(queueIdString)
                val reorderRequest = call.receive<ReorderFilmsDto>()

                val success = queueFilmService.reorderQueueFilms(queueId, reorderRequest.filmOrder)

                if (success) {
                    call.respond(HttpStatusCode.OK, "Films reordered successfully")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to reorder films")
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to reorder films: ${e.message}")
            }
        }

        put("/reorder") {
            try {
                val reorderRequest = call.receive<ReorderQueuesDto>()
                val queueIds = reorderRequest.queueOrder.map { UUID.fromString(it) }

                val success = queueRepository.reorderQueues(queueIds)

                if (success) {
                    call.respond(HttpStatusCode.OK, "Queues reordered successfully")
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Failed to reorder queues")
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID format: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to reorder queues: ${e.message}")
            }
        }

        get("/previews") {
            try {
                val limitParam = call.parameters["limit"]
                val filmsLimitParam = call.parameters["filmsLimit"]

                val limit = limitParam?.toIntOrNull() ?: 9
                val filmsLimit = filmsLimitParam?.toIntOrNull() ?: 3

                val queues = queueRepository.findAll().take(limit)
                val previews =
                    queues.map { queue ->
                        val films = queueFilmService.getQueueFilms(queue.id).take(filmsLimit)
                        val totalFilms = queueFilmService.getQueueFilms(queue.id).size
                        val queueDto = mapQueueToDto(queue, personRepository)

                        val filmsDto =
                            films.map { film ->
                                FilmResponseDto(
                                    tmdbId = film.tmdbId,
                                    title = film.title,
                                    originalTitle = film.originalTitle,
                                    releaseDate = film.releaseDate.toDateString(),
                                    runtime = film.runtime,
                                    genres = film.genres,
                                    posterPath = film.posterPath,
                                )
                            }

                        QueuePreviewDto(
                            queue = queueDto,
                            films = filmsDto,
                            totalFilms = totalFilms,
                        )
                    }

                call.respond(QueuePreviewsDto(previews))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch queue previews: ${e.message}")
            }
        }
    }
}
