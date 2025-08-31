package me.jochum.filmqueuer.adapters.web

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import me.jochum.filmqueuer.adapters.persistence.MySqlFilmRepository
import me.jochum.filmqueuer.adapters.persistence.MySqlPersonRepository
import me.jochum.filmqueuer.adapters.persistence.MySqlQueueFilmRepository
import me.jochum.filmqueuer.adapters.persistence.MySqlQueueRepository
import me.jochum.filmqueuer.adapters.tmdb.TmdbClient
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.domain.PersonSelectionService
import me.jochum.filmqueuer.domain.QueueFilmService

fun Application.configureRouting() {
    // Initialize dependencies
    val tmdbService: TmdbService = TmdbClient()
    val personRepository = MySqlPersonRepository()
    val queueRepository = MySqlQueueRepository()
    val filmRepository = MySqlFilmRepository()
    val queueFilmRepository = MySqlQueueFilmRepository()
    val personSelectionService = PersonSelectionService(personRepository, queueRepository)
    val queueFilmService = QueueFilmService(filmRepository, queueFilmRepository, tmdbService)

    routing {
        get("/") {
            call.respondText("Film Queuer API is running!")
        }

        route("/api") {
            configureFilmRoutes()
            configurePersonRoutes(tmdbService, personSelectionService, personRepository)
            configureQueueRoutes(queueRepository, personRepository, queueFilmService)
        }
    }
}
