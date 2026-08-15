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
import me.jochum.filmqueuer.domain.ExternalFilmRefRepository
import me.jochum.filmqueuer.domain.InvalidImageUrlException
import me.jochum.filmqueuer.domain.NamedQueue
import me.jochum.filmqueuer.domain.PersonQueue
import me.jochum.filmqueuer.domain.PersonRepository
import me.jochum.filmqueuer.domain.QueueFilmService
import me.jochum.filmqueuer.domain.QueueImageService
import me.jochum.filmqueuer.domain.QueueRepository
import java.util.UUID

private suspend fun enrichWithOwnership(
    films: List<FilmResponseDto>,
    externalFilmRefRepository: ExternalFilmRefRepository,
): List<FilmResponseDto> {
    val refsByTmdbId = externalFilmRefRepository.findByFilmTmdbIds(films.map { it.tmdbId }).associateBy { it.filmTmdbId }
    return films.map { film ->
        val ref = refsByTmdbId[film.tmdbId]
        film.copy(owned = ref?.owned ?: false, watched = ref?.watched ?: false)
    }
}

private suspend fun mapQueueToDto(
    queue: me.jochum.filmqueuer.domain.Queue,
    personRepository: PersonRepository,
    filmCount: Int = 0,
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
                name = null,
                description = null,
                filmCount = filmCount,
            )
        }
        is NamedQueue ->
            QueueDto(
                id = queue.id.toString(),
                type = "NAMED",
                createdAt = queue.createdAt.toString(),
                person = null,
                name = queue.name,
                description = queue.description,
                imagePath = queue.imagePath,
                filmCount = filmCount,
            )
        else ->
            QueueDto(
                id = queue.id.toString(),
                type = "UNKNOWN",
                createdAt = queue.createdAt.toString(),
                person = null,
                name = null,
                description = null,
                filmCount = filmCount,
            )
    }
}

fun Route.configureQueueRoutes(
    queueRepository: QueueRepository,
    personRepository: PersonRepository,
    queueFilmService: QueueFilmService,
    externalFilmRefRepository: ExternalFilmRefRepository,
    queueImageService: QueueImageService,
) {
    route("/queues") {
        /**
         * Tag: Queues
         * Description: List all queues, ordered by sort_order. Includes person data for PERSON queues.
         */
        get {
            try {
                val queues = queueRepository.findAll()
                val result =
                    queues.map { queue ->
                        val filmCount = queueFilmService.getQueueFilms(queue.id).size
                        mapQueueToDto(queue, personRepository, filmCount)
                    }
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch queues: ${e.message}")
            }
        }

        /**
         * Tag: Queues
         * Description: Get a specific queue.
         */
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

                val filmCount = queueFilmService.getQueueFilms(queue.id).size
                val result = mapQueueToDto(queue, personRepository, filmCount)
                call.respond(result)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch queue: ${e.message}")
            }
        }

        /**
         * Tag: Queues
         * Description: Delete a queue. Also clears its films and, for a named queue, deletes its
         *   locally-stored thumbnail file so nothing orphans on disk.
         */
        delete("/{queueId}") {
            try {
                val queueIdString = call.parameters["queueId"]
                if (queueIdString == null) {
                    call.respond(HttpStatusCode.BadRequest, "Queue ID is required")
                    return@delete
                }

                val queueId = UUID.fromString(queueIdString)
                val queue = queueRepository.findById(queueId)

                if (queue == null) {
                    call.respond(HttpStatusCode.NotFound, "Queue not found")
                    return@delete
                }

                queueFilmService.clearQueue(queueId)
                queueImageService.deleteImageFile(queue)
                queueRepository.deleteById(queueId)
                call.respond(HttpStatusCode.OK, "Queue deleted successfully")
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to delete queue: ${e.message}")
            }
        }

        /**
         * Tag: Queues
         * Description: Add a film or TV show to a queue.
         */
        post("/{queueId}/films") {
            try {
                val queueIdString = call.parameters["queueId"]
                if (queueIdString == null) {
                    call.respond(HttpStatusCode.BadRequest, "Queue ID is required")
                    return@post
                }

                val queueId = UUID.fromString(queueIdString)
                val filmRequest = call.receive<FilmRequestDto>()

                queueFilmService.addFilmToQueue(queueId, filmRequest.tmdbId, filmRequest.tv)
                call.respond(HttpStatusCode.Created, "Film added to queue successfully")
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to add film to queue: ${e.message}")
            }
        }

        /**
         * Tag: Queues
         * Description: Get a queue's films, ordered by sort_order.
         */
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
                            enrichWithOwnership(
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
                                externalFilmRefRepository,
                            ),
                    )

                call.respond(response)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID: ${e.message}")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch queue films: ${e.message}")
            }
        }

        /**
         * Tag: Queues
         * Description: Remove a film from a queue.
         * Path: filmTmdbId [Int] TMDB film ID
         */
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

        /**
         * Tag: Queues
         * Description: Reorder a queue's films.
         */
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

        /**
         * Tag: Queues
         * Description: Reorder queues themselves.
         */
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

        /**
         * Tag: Queues
         * Description: Compact queue previews (with a few films each) for the home page. Accepts
         *   optional limit (max queues, default 9) and filmsLimit (max films per queue, default 3)
         *   query params.
         */
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
                        val queueDto = mapQueueToDto(queue, personRepository, totalFilms)

                        val filmsDto =
                            enrichWithOwnership(
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
                                externalFilmRefRepository,
                            )

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

        /**
         * Tag: Queues
         * Description: Create a named (non-person) queue.
         */
        post("/named") {
            try {
                val createRequest = call.receive<CreateNamedQueueDto>()

                if (createRequest.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "Queue name cannot be empty")
                    return@post
                }

                val namedQueue =
                    NamedQueue(
                        id = UUID.randomUUID(),
                        name = createRequest.name.trim(),
                        description = createRequest.description?.trim(),
                    )

                val savedQueue = queueRepository.save(namedQueue)
                val result = mapQueueToDto(savedQueue, personRepository)

                call.respond(HttpStatusCode.Created, result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to create named queue: ${e.message}")
            }
        }

        /**
         * Tag: Queues
         * Description: Set or clear a named queue's thumbnail. imagePath is a source URL to download; the
         *   backend stores a local copy and persists a local path like /images/queue/<file>.jpg, not the
         *   original URL. A blank or omitted imagePath clears the thumbnail (and deletes the old file).
         */
        put("/{queueId}/image-path") {
            try {
                val queueIdString = call.parameters["queueId"]
                if (queueIdString == null) {
                    call.respond(HttpStatusCode.BadRequest, "Queue ID is required")
                    return@put
                }

                val queueId = UUID.fromString(queueIdString)
                val updateDto = call.receive<UpdateQueueImageDto>()
                val sourceUrl = updateDto.imagePath?.trim()?.takeIf { it.isNotBlank() }

                val updated =
                    if (sourceUrl == null) {
                        queueImageService.clearImage(queueId)
                    } else {
                        queueImageService.setImage(queueId, sourceUrl)
                    }

                if (updated) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Named queue not found")
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, "Invalid queue ID: ${e.message}")
            } catch (e: InvalidImageUrlException) {
                call.respond(HttpStatusCode.BadRequest, e.message ?: "Invalid image URL")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update image: ${e.message}")
            }
        }
    }
}
