package me.jochum.filmqueuer.adapters.web

import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.server.application.Application
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi
import me.jochum.filmqueuer.adapters.persistence.MySqlExternalFilmRefRepository
import me.jochum.filmqueuer.adapters.persistence.MySqlFilmRepository
import me.jochum.filmqueuer.adapters.persistence.MySqlPersonRepository
import me.jochum.filmqueuer.adapters.persistence.MySqlQueueFilmRepository
import me.jochum.filmqueuer.adapters.persistence.MySqlQueueRepository
import me.jochum.filmqueuer.adapters.storage.LocalQueueImageStorage
import me.jochum.filmqueuer.adapters.tmdb.TmdbClient
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.domain.LetterboxdImportService
import me.jochum.filmqueuer.domain.PersonSelectionService
import me.jochum.filmqueuer.domain.QueueFilmService
import me.jochum.filmqueuer.domain.QueueImageService
import me.jochum.filmqueuer.domain.TmdbFilmFactory

@OptIn(ExperimentalKtorApi::class)
fun Application.configureRouting() {
    // Initialize dependencies
    val tmdbService: TmdbService = TmdbClient()
    val personRepository = MySqlPersonRepository()
    val queueRepository = MySqlQueueRepository()
    val filmRepository = MySqlFilmRepository()
    val queueFilmRepository = MySqlQueueFilmRepository()
    val externalFilmRefRepository = MySqlExternalFilmRefRepository()
    val queueImageStorage = LocalQueueImageStorage()
    val filmFactory = TmdbFilmFactory(tmdbService, personRepository)
    val personSelectionService = PersonSelectionService(personRepository, queueRepository)
    val queueFilmService = QueueFilmService(filmRepository, queueFilmRepository, filmFactory)
    val queueImageService = QueueImageService(queueRepository, queueImageStorage)
    val letterboxdImportService =
        LetterboxdImportService(externalFilmRefRepository, filmRepository, personRepository, tmdbService, filmFactory)

    routing {
        /**
         * Tag: Health
         * Description: Liveness check.
         */
        get("/") {
            call.respondText("Film Queuer API is running!")
        }

        swaggerUI("/apidocs") {
            info =
                OpenApiInfo(
                    title = "Film Queuer API",
                    version = "1.0",
                    description =
                        "Manage personalized film/TV queues based on TMDB data, plus a Collection page for " +
                            "tracking a personal library imported from Letterboxd exports. Generated live from " +
                            "the routing tree, so it always reflects the running server.",
                )
            source = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)
        }

        configureImageRoutes(queueImageStorage.storageDir)

        route("/api") {
            configureFilmRoutes(tmdbService, filmRepository, externalFilmRefRepository)
            configurePersonRoutes(tmdbService, personSelectionService, personRepository, externalFilmRefRepository)
            configureQueueRoutes(queueRepository, personRepository, queueFilmService, externalFilmRefRepository, queueImageService)
            configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository)
        }
    }
}
