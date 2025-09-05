package me.jochum.filmqueuer.adapters.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import me.jochum.filmqueuer.adapters.tmdb.TmdbService

fun Route.configureFilmRoutes(tmdbService: TmdbService) {
    route("/films") {
        get("/search") {
            val query = call.request.queryParameters["q"]
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Query parameter 'q' is required")
                return@get
            }

            try {
                val tmdbResponse = tmdbService.searchMovies(query)

                val filmSearchResponse =
                    FilmSearchResponseDto(
                        page = tmdbResponse.page,
                        totalPages = tmdbResponse.totalPages,
                        totalResults = tmdbResponse.totalResults,
                        results =
                            tmdbResponse.results.map { movie ->
                                FilmDto(
                                    id = movie.id,
                                    title = movie.title,
                                    originalTitle = movie.originalTitle,
                                    releaseDate = movie.releaseDate,
                                    posterPath = movie.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                                    voteAverage = movie.voteAverage,
                                    voteCount = movie.voteCount,
                                    overview = movie.overview,
                                    mediaType = null,
                                    role = null,
                                    tv = false,
                                )
                            },
                    )

                call.respond(filmSearchResponse)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to search movies: ${e.message}")
            }
        }

        get("/search/tv") {
            val query = call.request.queryParameters["q"]
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Query parameter 'q' is required")
                return@get
            }

            try {
                val tmdbResponse = tmdbService.searchTv(query)

                val tvSearchResponse =
                    FilmSearchResponseDto(
                        page = tmdbResponse.page,
                        totalPages = tmdbResponse.totalPages,
                        totalResults = tmdbResponse.totalResults,
                        results =
                            tmdbResponse.results.map { tvShow ->
                                FilmDto(
                                    id = tvShow.id,
                                    title = tvShow.name,
                                    originalTitle = tvShow.originalName,
                                    releaseDate = tvShow.firstAirDate,
                                    posterPath = tvShow.posterPath?.let { "https://image.tmdb.org/t/p/w300$it" },
                                    voteAverage = tvShow.voteAverage,
                                    voteCount = tvShow.voteCount,
                                    overview = tvShow.overview,
                                    mediaType = null,
                                    role = null,
                                    tv = true,
                                )
                            },
                    )

                call.respond(tvSearchResponse)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to search TV shows: ${e.message}")
            }
        }
    }
}
