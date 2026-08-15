package me.jochum.filmqueuer.adapters.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

fun Route.configureImageRoutes(storageDir: String) {
    get("/images/queue/{filename}") {
        val filename = call.parameters["filename"]
        if (filename.isNullOrBlank() || filename.contains("/") || filename.contains("..")) {
            call.respond(HttpStatusCode.BadRequest, "Invalid filename")
            return@get
        }

        val file = File(storageDir, filename)
        if (!file.exists()) {
            call.respond(HttpStatusCode.NotFound)
            return@get
        }

        call.respondFile(file)
    }
}
