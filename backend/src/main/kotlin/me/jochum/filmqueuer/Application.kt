package me.jochum.filmqueuer

import me.jochum.filmqueuer.adapters.web.*
import me.jochum.filmqueuer.adapters.persistence.DatabaseConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseConfig.init()
    configureHTTP()
    configureSerialization()
    configureRouting()
}