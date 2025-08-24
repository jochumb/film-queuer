package me.jochum.filmqueuer.adapters.web

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import me.jochum.filmqueuer.adapters.persistence.MySqlPersonRepository
import me.jochum.filmqueuer.adapters.tmdb.TmdbClient
import me.jochum.filmqueuer.adapters.tmdb.TmdbService

fun Application.configureRouting() {
    // Initialize dependencies
    val tmdbService: TmdbService = TmdbClient()
    val personRepository = MySqlPersonRepository()

    routing {
        get("/") {
            call.respondText("Film Queuer API is running!")
        }

        route("/api") {
            configureFilmRoutes()
            configurePersonRoutes(tmdbService, personRepository)
        }
    }
}
