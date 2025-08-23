package com.example.filmqueuer.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.configureFilmRoutes() {
    route("/films") {
        get {
            call.respondText("Films endpoint")
        }
    }
}