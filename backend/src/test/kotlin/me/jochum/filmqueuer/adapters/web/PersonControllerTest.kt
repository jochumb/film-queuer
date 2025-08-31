package me.jochum.filmqueuer.adapters.web

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import me.jochum.filmqueuer.adapters.tmdb.TmdbKnownFor
import me.jochum.filmqueuer.adapters.tmdb.TmdbPerson
import me.jochum.filmqueuer.adapters.tmdb.TmdbPersonSearchResponse
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.domain.Department
import me.jochum.filmqueuer.domain.Person
import me.jochum.filmqueuer.domain.PersonQueue
import me.jochum.filmqueuer.domain.PersonRepository
import me.jochum.filmqueuer.domain.PersonSelectionResult
import me.jochum.filmqueuer.domain.PersonSelectionService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonControllerTest {
    private lateinit var tmdbService: TmdbService
    private lateinit var personSelectionService: PersonSelectionService
    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun setup() {
        tmdbService = mockk()
        personSelectionService = mockk()
        personRepository = mockk()
    }

    @Test
    fun `GET persons search should return search results`() =
        testApplication {
            // Given
            val mockTmdbResponse =
                TmdbPersonSearchResponse(
                    page = 1,
                    results =
                        listOf(
                            TmdbPerson(
                                id = 123,
                                name = "Tom Hanks",
                                knownForDepartment = "Acting",
                                profilePath = "/path.jpg",
                                popularity = 25.5,
                                knownFor =
                                    listOf(
                                        TmdbKnownFor(
                                            1,
                                            title = "Forrest Gump",
                                            mediaType = "movie",
                                            releaseDate = "1994-07-06",
                                            name = null,
                                            firstAirDate = null,
                                        ),
                                    ),
                            ),
                        ),
                    totalPages = 1,
                    totalResults = 1,
                )

            coEvery { tmdbService.searchPerson("tom hanks") } returns mockTmdbResponse

            application {
                configureSerialization()
                routing {
                    configurePersonRoutes(tmdbService, personSelectionService, personRepository)
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
    fun `GET persons search should return 400 when query parameter missing`() =
        testApplication {
            application {
                routing {
                    configurePersonRoutes(tmdbService, personSelectionService, personRepository)
                }
            }

            // When
            val response = client.get("/persons/search")

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Query parameter 'q' is required"))
        }

    @Test
    fun `POST persons select should save person and return 201`() =
        testApplication {
            // Given
            val person =
                Person(
                    tmdbId = 123,
                    name = "Tom Hanks",
                    department = Department.ACTING,
                )

            val queueId = UUID.randomUUID()
            val personQueue =
                PersonQueue(
                    id = queueId,
                    personTmdbId = 123,
                )

            val selectionResult =
                PersonSelectionResult(
                    person = person,
                    queue = personQueue,
                )

            coEvery {
                personSelectionService.selectPerson(123, "Tom Hanks", Department.ACTING)
            } returns selectionResult

            application {
                configureSerialization()
                routing {
                    configurePersonRoutes(tmdbService, personSelectionService, personRepository)
                }
            }

            // When
            val response =
                client.post("/persons/select") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"tmdbId": 123, "name": "Tom Hanks", "department": "Acting"}""")
                }

            // Then
            assertEquals(HttpStatusCode.Created, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Tom Hanks"))
            assertTrue(responseBody.contains("ACTING"))
            assertTrue(responseBody.contains(queueId.toString()))
            coVerify { personSelectionService.selectPerson(123, "Tom Hanks", Department.ACTING) }
        }

    @Test
    fun `POST persons select should handle unknown department as OTHER`() =
        testApplication {
            // Given
            val expectedPerson =
                Person(
                    tmdbId = 123,
                    name = "John Doe",
                    department = Department.OTHER,
                )

            val queueId = UUID.randomUUID()
            val personQueue =
                PersonQueue(
                    id = queueId,
                    personTmdbId = 123,
                )

            val selectionResult =
                PersonSelectionResult(
                    person = expectedPerson,
                    queue = personQueue,
                )

            coEvery {
                personSelectionService.selectPerson(123, "John Doe", Department.OTHER)
            } returns selectionResult

            application {
                configureSerialization()
                routing {
                    configurePersonRoutes(tmdbService, personSelectionService, personRepository)
                }
            }

            // When
            val response =
                client.post("/persons/select") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"tmdbId": 123, "name": "John Doe", "department": "Unknown Role"}""")
                }

            // Then
            assertEquals(HttpStatusCode.Created, response.status)
            coVerify { personSelectionService.selectPerson(123, "John Doe", Department.OTHER) }
        }

    @Test
    fun `GET persons search should handle TMDB client error`() =
        testApplication {
            // Given
            coEvery { tmdbService.searchPerson("error") } throws RuntimeException("TMDB API Error")

            application {
                routing {
                    configurePersonRoutes(tmdbService, personSelectionService, personRepository)
                }
            }

            // When
            val response = client.get("/persons/search?q=error")

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to search persons"))
        }

    @Test
    fun `POST persons select should handle repository error`() =
        testApplication {
            // Given
            coEvery { personSelectionService.selectPerson(123, "John Doe", Department.ACTING) } throws RuntimeException("Database Error")

            application {
                configureSerialization()
                routing {
                    configurePersonRoutes(tmdbService, personSelectionService, personRepository)
                }
            }

            // When
            val response =
                client.post("/persons/select") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"tmdbId": 123, "name": "John Doe", "department": "Acting"}""")
                }

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to save person"))
        }
}
