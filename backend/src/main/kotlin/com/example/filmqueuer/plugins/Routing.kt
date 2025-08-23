package com.example.filmqueuer.plugins

import com.example.filmqueuer.routes.configureFilmRoutes
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