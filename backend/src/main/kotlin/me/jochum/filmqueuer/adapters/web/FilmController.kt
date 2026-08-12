package me.jochum.filmqueuer.adapters.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.domain.ExternalFilmRefRepository
import me.jochum.filmqueuer.domain.FilmRepository

private suspend fun enrichWithOwnership(
    films: List<FilmDto>,
    externalFilmRefRepository: ExternalFilmRefRepository,
): List<FilmDto> {
    val refsByTmdbId = externalFilmRefRepository.findByFilmTmdbIds(films.map { it.id }).associateBy { it.filmTmdbId }
    return films.map { film ->
        val ref = refsByTmdbId[film.id]
        film.copy(owned = ref?.owned ?: false, watched = ref?.watched ?: false)
    }
}

fun Route.configureFilmRoutes(
    tmdbService: TmdbService,
    filmRepository: FilmRepository,
    externalFilmRefRepository: ExternalFilmRefRepository,
) {
    route("/films") {
        get("/search") {
            val query = call.request.queryParameters["q"]
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Query parameter 'q' is required")
                return@get
            }
            val year = call.request.queryParameters["year"]?.toIntOrNull()

            try {
                val tmdbResponse = tmdbService.searchMovies(query, year)

                val filmSearchResponse =
                    FilmSearchResponseDto(
                        page = tmdbResponse.page,
                        totalPages = tmdbResponse.totalPages,
                        totalResults = tmdbResponse.totalResults,
                        results =
                            enrichWithOwnership(
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
                                externalFilmRefRepository,
                            ),
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
            val year = call.request.queryParameters["year"]?.toIntOrNull()

            try {
                val tmdbResponse = tmdbService.searchTv(query, year)

                val tvSearchResponse =
                    FilmSearchResponseDto(
                        page = tmdbResponse.page,
                        totalPages = tmdbResponse.totalPages,
                        totalResults = tmdbResponse.totalResults,
                        results =
                            enrichWithOwnership(
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
                                externalFilmRefRepository,
                            ),
                    )

                call.respond(tvSearchResponse)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to search TV shows: ${e.message}")
            }
        }

        put("/{tmdbId}/sort-title") {
            val tmdbId = call.parameters["tmdbId"]?.toIntOrNull()
            if (tmdbId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid tmdbId parameter")
                return@put
            }

            try {
                val updateDto = call.receive<UpdateSortTitleDto>()
                if (updateDto.sortTitle.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, "sortTitle must not be blank")
                    return@put
                }

                val updated = filmRepository.updateSortTitle(tmdbId, updateDto.sortTitle.trim())
                if (updated) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Film not found")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update sort title: ${e.message}")
            }
        }
    }
}
