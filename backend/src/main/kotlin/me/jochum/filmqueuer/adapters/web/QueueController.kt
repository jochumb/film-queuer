package me.jochum.filmqueuer.adapters.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import me.jochum.filmqueuer.domain.PersonQueue
import me.jochum.filmqueuer.domain.PersonRepository
import me.jochum.filmqueuer.domain.QueueRepository

fun Route.configureQueueRoutes(
    queueRepository: QueueRepository,
    personRepository: PersonRepository,
) {
    route("/queues") {
        get {
            try {
                val queues = queueRepository.findAll()
                val result =
                    queues.map { queue ->
                        when (queue) {
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
                call.respond(result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to fetch queues: ${e.message}")
            }
        }
    }
}
