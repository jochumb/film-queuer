package me.jochum.filmqueuer.adapters.web

import me.jochum.filmqueuer.adapters.web.configureFilmRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Film Queuer API is running!")
        }
        
        route("/api") {
            configureFilmRoutes()
        }
    }
}