package me.jochum.filmqueuer.adapters.web

import me.jochum.filmqueuer.adapters.web.configureFilmRoutes
import me.jochum.filmqueuer.adapters.tmdb.TmdbClient
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.adapters.persistence.MySqlPersonRepository
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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