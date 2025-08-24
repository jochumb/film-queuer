package me.jochum.filmqueuer.adapters.web

import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.configureFilmRoutes() {
    route("/films") {
        get {
            call.respondText("Films endpoint")
        }
    }
}
