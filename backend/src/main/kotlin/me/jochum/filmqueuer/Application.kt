package me.jochum.filmqueuer

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import me.jochum.filmqueuer.adapters.persistence.DatabaseConfig
import me.jochum.filmqueuer.adapters.web.configureHTTP
import me.jochum.filmqueuer.adapters.web.configureRouting
import me.jochum.filmqueuer.adapters.web.configureSerialization

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
