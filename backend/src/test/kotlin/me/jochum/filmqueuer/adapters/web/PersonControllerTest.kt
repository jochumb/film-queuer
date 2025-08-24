package me.jochum.filmqueuer.adapters.web

import me.jochum.filmqueuer.adapters.tmdb.*
import me.jochum.filmqueuer.domain.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.server.routing.*
import io.ktor.server.application.*
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonControllerTest {

    private lateinit var tmdbService: TmdbService
    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun setup() {
        tmdbService = mockk()
        personRepository = mockk()
    }

    @Test
    fun `GET persons search should return search results`() = testApplication {
        // Given
        val mockTmdbResponse = TmdbPersonSearchResponse(
            page = 1,
            results = listOf(
                TmdbPerson(
                    id = 123,
                    name = "Tom Hanks",
                    knownForDepartment = "Acting",
                    profilePath = "/path.jpg",
                    popularity = 25.5,
                    knownFor = listOf(
                        TmdbKnownFor(1, title = "Forrest Gump", mediaType = "movie", releaseDate = "1994-07-06", name = null, firstAirDate = null)
                    )
                )
            ),
            totalPages = 1,
            totalResults = 1
        )

        coEvery { tmdbService.searchPerson("tom hanks") } returns mockTmdbResponse

        application {
            configureSerialization()
            routing {
                configurePersonRoutes(tmdbService, personRepository)
            }
        }

        // When
        val response = client.get("/persons/search?q=tom hanks")

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Tom Hanks"))
        assertTrue(responseBody.contains("Acting"))
        assertTrue(responseBody.contains("Forrest Gump"))
    }

    @Test
    fun `GET persons search should return 400 when query parameter missing`() = testApplication {
        application {
            routing {
                configurePersonRoutes(tmdbService, personRepository)
            }
        }

        // When
        val response = client.get("/persons/search")

        // Then
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Query parameter 'q' is required"))
    }

    @Test
    fun `POST persons select should save person and return 201`() = testApplication {
        // Given
        val person = Person(
            tmdbId = 123,
            name = "Tom Hanks",
            department = Department.ACTING
        )

        coEvery { personRepository.save(person) } returns person

        application {
            configureSerialization()
            routing {
                configurePersonRoutes(tmdbService, personRepository)
            }
        }

        // When
        val response = client.post("/persons/select") {
            contentType(ContentType.Application.Json)
            setBody("""{"tmdbId": 123, "name": "Tom Hanks", "department": "Acting"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("Person saved successfully"))
        coVerify { personRepository.save(person) }
    }

    @Test
    fun `GET persons should return all saved persons`() = testApplication {
        // Given
        val persons = listOf(
            Person(123, "Tom Hanks", Department.ACTING),
            Person(456, "Steven Spielberg", Department.DIRECTING)
        )

        coEvery { personRepository.findAll() } returns persons

        application {
            configureSerialization()
            routing {
                configurePersonRoutes(tmdbService, personRepository)
            }
        }

        // When
        val response = client.get("/persons")

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("Tom Hanks"))
        assertTrue(responseBody.contains("Steven Spielberg"))
        assertTrue(responseBody.contains("ACTING"))
        assertTrue(responseBody.contains("DIRECTING"))
    }

    @Test
    fun `POST persons select should handle unknown department as OTHER`() = testApplication {
        // Given
        val expectedPerson = Person(
            tmdbId = 123,
            name = "John Doe",
            department = Department.OTHER
        )

        coEvery { personRepository.save(expectedPerson) } returns expectedPerson

        application {
            configureSerialization()
            routing {
                configurePersonRoutes(tmdbService, personRepository)
            }
        }

        // When
        val response = client.post("/persons/select") {
            contentType(ContentType.Application.Json)
            setBody("""{"tmdbId": 123, "name": "John Doe", "department": "Unknown Role"}""")
        }

        // Then
        assertEquals(HttpStatusCode.Created, response.status)
        coVerify { personRepository.save(expectedPerson) }
    }

    @Test
    fun `GET persons search should handle TMDB client error`() = testApplication {
        // Given
        coEvery { tmdbService.searchPerson("error") } throws RuntimeException("TMDB API Error")

        application {
            routing {
                configurePersonRoutes(tmdbService, personRepository)
            }
        }

        // When
        val response = client.get("/persons/search?q=error")

        // Then
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue(response.bodyAsText().contains("Failed to search persons"))
    }

    @Test
    fun `POST persons select should handle repository error`() = testApplication {
        // Given
        coEvery { personRepository.save(any()) } throws RuntimeException("Database Error")

        application {
            configureSerialization()
            routing {
                configurePersonRoutes(tmdbService, personRepository)
            }
        }

        // When
        val response = client.post("/persons/select") {
            contentType(ContentType.Application.Json)
            setBody("""{"tmdbId": 123, "name": "John Doe", "department": "Acting"}""")
        }

        // Then
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertTrue(response.bodyAsText().contains("Failed to save person"))
    }
}