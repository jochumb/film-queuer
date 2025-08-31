package me.jochum.filmqueuer

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.adapters.persistence.DatabaseConfig
import me.jochum.filmqueuer.adapters.persistence.MySqlPersonRepository
import me.jochum.filmqueuer.adapters.persistence.PersonEnrichmentUtility
import me.jochum.filmqueuer.adapters.tmdb.TmdbClient
import me.jochum.filmqueuer.adapters.web.configureHTTP
import me.jochum.filmqueuer.adapters.web.configureRouting
import me.jochum.filmqueuer.adapters.web.configureSerialization
import me.jochum.filmqueuer.domain.PersonEnrichmentService

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    DatabaseConfig.init()

    // Run person enrichment if requested
    runBlocking {
        val tmdbService = TmdbClient()
        val personRepository = MySqlPersonRepository()
        val personEnrichmentService = PersonEnrichmentService(personRepository, tmdbService)

        PersonEnrichmentUtility.enrichPersonsIfRequested(personEnrichmentService)
    }

    configureHTTP()
    configureSerialization()
    configureRouting()
}
