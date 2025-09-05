package me.jochum.filmqueuer.adapters.web

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import me.jochum.filmqueuer.adapters.tmdb.TmdbMovie
import me.jochum.filmqueuer.adapters.tmdb.TmdbMovieSearchResponse
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.adapters.tmdb.TmdbTvSearchResponse
import me.jochum.filmqueuer.adapters.tmdb.TmdbTvShow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilmControllerTest {
    private lateinit var tmdbService: TmdbService

    @BeforeEach
    fun setup() {
        tmdbService = mockk()
    }

    @Test
    fun `GET films search should return movie search results`() =
        testApplication {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }

            routing {
                configureFilmRoutes(tmdbService)
            }

            // Given
            val mockResponse =
                TmdbMovieSearchResponse(
                    page = 1,
                    totalPages = 1,
                    totalResults = 1,
                    results =
                        listOf(
                            TmdbMovie(
                                id = 550,
                                title = "Fight Club",
                                originalTitle = "Fight Club",
                                releaseDate = "1999-10-15",
                                posterPath = "/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                                voteAverage = 8.8,
                                voteCount = 26280,
                                overview = "A nameless first person narrator...",
                            ),
                        ),
                )

            coEvery { tmdbService.searchMovies("Fight Club") } returns mockResponse

            // When
            val response = client.get("/films/search?q=Fight Club")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { tmdbService.searchMovies("Fight Club") }

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Fight Club"))
        }

    @Test
    fun `GET films search tv should return TV show search results`() =
        testApplication {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }

            routing {
                configureFilmRoutes(tmdbService)
            }

            // Given
            val mockResponse =
                TmdbTvSearchResponse(
                    page = 1,
                    totalPages = 1,
                    totalResults = 1,
                    results =
                        listOf(
                            TmdbTvShow(
                                id = 1399,
                                name = "Game of Thrones",
                                originalName = "Game of Thrones",
                                firstAirDate = "2011-04-17",
                                posterPath = "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                                voteAverage = 9.2,
                                voteCount = 8654,
                                overview = "Seven noble families fight for control of the mythical land of Westeros.",
                            ),
                        ),
                )

            coEvery { tmdbService.searchTv("Game of Thrones") } returns mockResponse

            // When
            val response = client.get("/films/search/tv?q=Game of Thrones")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { tmdbService.searchTv("Game of Thrones") }

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Game of Thrones"))
        }

    @Test
    fun `GET films search should return bad request when query parameter is missing`() =
        testApplication {
            routing {
                configureFilmRoutes(tmdbService)
            }

            // When
            val response = client.get("/films/search")

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Query parameter 'q' is required", response.bodyAsText())
        }

    @Test
    fun `GET films search tv should return bad request when query parameter is missing`() =
        testApplication {
            routing {
                configureFilmRoutes(tmdbService)
            }

            // When
            val response = client.get("/films/search/tv")

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("Query parameter 'q' is required", response.bodyAsText())
        }

    @Test
    fun `GET films search should handle TMDB service errors for movies`() =
        testApplication {
            routing {
                configureFilmRoutes(tmdbService)
            }

            // Given
            coEvery { tmdbService.searchMovies("test") } throws RuntimeException("TMDB API Error")

            // When
            val response = client.get("/films/search?q=test")

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to search movies"))
        }

    @Test
    fun `GET films search tv should handle TMDB service errors for TV shows`() =
        testApplication {
            routing {
                configureFilmRoutes(tmdbService)
            }

            // Given
            coEvery { tmdbService.searchTv("test") } throws RuntimeException("TMDB API Error")

            // When
            val response = client.get("/films/search/tv?q=test")

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to search TV shows"))
        }

    @Test
    fun `GET films search should handle empty movie results`() =
        testApplication {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }

            routing {
                configureFilmRoutes(tmdbService)
            }

            // Given
            val mockResponse =
                TmdbMovieSearchResponse(
                    page = 1,
                    totalPages = 1,
                    totalResults = 0,
                    results = emptyList(),
                )

            coEvery { tmdbService.searchMovies("nonexistent") } returns mockResponse

            // When
            val response = client.get("/films/search?q=nonexistent")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { tmdbService.searchMovies("nonexistent") }

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("\"totalResults\":0"))
            assertTrue(responseBody.contains("\"results\":[]"))
        }

    @Test
    fun `GET films search tv should handle empty TV show results`() =
        testApplication {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }

            routing {
                configureFilmRoutes(tmdbService)
            }

            // Given
            val mockResponse =
                TmdbTvSearchResponse(
                    page = 1,
                    totalPages = 1,
                    totalResults = 0,
                    results = emptyList(),
                )

            coEvery { tmdbService.searchTv("nonexistent") } returns mockResponse

            // When
            val response = client.get("/films/search/tv?q=nonexistent")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { tmdbService.searchTv("nonexistent") }

            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("\"totalResults\":0"))
            assertTrue(responseBody.contains("\"results\":[]"))
        }
}