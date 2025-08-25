package me.jochum.filmqueuer.adapters.web

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import me.jochum.filmqueuer.domain.Department
import me.jochum.filmqueuer.domain.Film
import me.jochum.filmqueuer.domain.Person
import me.jochum.filmqueuer.domain.PersonQueue
import me.jochum.filmqueuer.domain.PersonRepository
import me.jochum.filmqueuer.domain.QueueFilm
import me.jochum.filmqueuer.domain.QueueFilmService
import me.jochum.filmqueuer.domain.QueueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueueControllerTest {
    private lateinit var queueRepository: QueueRepository
    private lateinit var personRepository: PersonRepository
    private lateinit var queueFilmService: QueueFilmService

    @BeforeEach
    fun setup() {
        queueRepository = mockk()
        personRepository = mockk()
        queueFilmService = mockk()
    }

    @Test
    fun `GET queues should return all queues with person data`() =
        testApplication {
            // Given
            val person =
                Person(
                    tmdbId = 123,
                    name = "Tom Hanks",
                    department = Department.ACTING,
                )

            val queueId = UUID.randomUUID()
            val createdAt = Instant.now()
            val personQueue =
                PersonQueue(
                    id = queueId,
                    personTmdbId = 123,
                    createdAt = createdAt,
                )

            coEvery { queueRepository.findAll() } returns listOf(personQueue)
            coEvery { personRepository.findByTmdbId(123) } returns person

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.get("/queues")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains(queueId.toString()))
            assertTrue(responseBody.contains("PERSON"))
            assertTrue(responseBody.contains("Tom Hanks"))
            assertTrue(responseBody.contains("ACTING"))

            coVerify { queueRepository.findAll() }
            coVerify { personRepository.findByTmdbId(123) }
        }

    @Test
    fun `GET queues should handle person queue without matching person`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val createdAt = Instant.now()
            val personQueue =
                PersonQueue(
                    id = queueId,
                    personTmdbId = 999,
                    createdAt = createdAt,
                )

            coEvery { queueRepository.findAll() } returns listOf(personQueue)
            coEvery { personRepository.findByTmdbId(999) } returns null

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.get("/queues")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains(queueId.toString()))
            assertTrue(responseBody.contains("PERSON"))
            assertTrue(responseBody.contains("null")) // person should be null

            coVerify { queueRepository.findAll() }
            coVerify { personRepository.findByTmdbId(999) }
        }

    @Test
    fun `GET queues should return empty list when no queues exist`() =
        testApplication {
            // Given
            coEvery { queueRepository.findAll() } returns emptyList()

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.get("/queues")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertEquals("[]", responseBody)

            coVerify { queueRepository.findAll() }
        }

    @Test
    fun `GET queues should handle multiple queues with different person data`() =
        testApplication {
            // Given
            val person1 = Person(123, "Tom Hanks", Department.ACTING)
            val person2 = Person(456, "Steven Spielberg", Department.DIRECTING)

            val queue1Id = UUID.randomUUID()
            val queue2Id = UUID.randomUUID()
            val createdAt = Instant.now()

            val queue1 = PersonQueue(queue1Id, 123, createdAt)
            val queue2 = PersonQueue(queue2Id, 456, createdAt)

            coEvery { queueRepository.findAll() } returns listOf(queue1, queue2)
            coEvery { personRepository.findByTmdbId(123) } returns person1
            coEvery { personRepository.findByTmdbId(456) } returns person2

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.get("/queues")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Tom Hanks"))
            assertTrue(responseBody.contains("Steven Spielberg"))
            assertTrue(responseBody.contains("ACTING"))
            assertTrue(responseBody.contains("DIRECTING"))

            coVerify { queueRepository.findAll() }
            coVerify { personRepository.findByTmdbId(123) }
            coVerify { personRepository.findByTmdbId(456) }
        }

    @Test
    fun `GET queues should handle queue repository error`() =
        testApplication {
            // Given
            coEvery { queueRepository.findAll() } throws RuntimeException("Database Error")

            application {
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.get("/queues")

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to fetch queues"))
            assertTrue(response.bodyAsText().contains("Database Error"))
        }

    @Test
    fun `GET queues should handle person repository error`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val personQueue = PersonQueue(queueId, 123, Instant.now())

            coEvery { queueRepository.findAll() } returns listOf(personQueue)
            coEvery { personRepository.findByTmdbId(123) } throws RuntimeException("Person lookup failed")

            application {
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.get("/queues")

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to fetch queues"))
        }

    @Test
    fun `POST queue films should add film to queue successfully`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val film =
                Film(
                    tmdbId = 550,
                    title = "Fight Club",
                    originalTitle = "Fight Club",
                    releaseDate = LocalDate.of(1999, 10, 15),
                )
            val queueFilm = QueueFilm(queueId, film.tmdbId, Instant.now())

            coEvery { queueFilmService.addFilmToQueue(queueId, film) } returns queueFilm

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response =
                client.post("/queues/$queueId/films") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "tmdbId": 550,
                            "title": "Fight Club",
                            "originalTitle": "Fight Club",
                            "releaseDate": "1999-10-15"
                        }
                        """.trimIndent(),
                    )
                }

            // Then
            assertEquals(HttpStatusCode.Created, response.status)
            assertTrue(response.bodyAsText().contains("Film added to queue successfully"))

            coVerify { queueFilmService.addFilmToQueue(queueId, film) }
        }

    @Test
    fun `POST queue films should return bad request for invalid queue ID`() =
        testApplication {
            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response =
                client.post("/queues/invalid-uuid/films") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"tmdbId": 550, "title": "Fight Club"}""")
                }

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid queue ID"))
        }

    @Test
    fun `GET queue films should return films successfully`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val films =
                listOf(
                    Film(550, "Fight Club", "Fight Club", LocalDate.of(1999, 10, 15)),
                    Film(13, "Forrest Gump", null, LocalDate.of(1994, 7, 6)),
                )

            coEvery { queueFilmService.getQueueFilms(queueId) } returns films

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.get("/queues/$queueId/films")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("Fight Club"))
            assertTrue(responseBody.contains("Forrest Gump"))
            assertTrue(responseBody.contains("550"))
            assertTrue(responseBody.contains("13"))

            coVerify { queueFilmService.getQueueFilms(queueId) }
        }

    @Test
    fun `GET queue films should return empty list when no films in queue`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            coEvery { queueFilmService.getQueueFilms(queueId) } returns emptyList()

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.get("/queues/$queueId/films")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(responseBody.contains("\"films\":[]"))

            coVerify { queueFilmService.getQueueFilms(queueId) }
        }

    @Test
    fun `GET queue films should return bad request for invalid queue ID`() =
        testApplication {
            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.get("/queues/invalid-uuid/films")

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid queue ID"))
        }

    @Test
    fun `POST queue films should handle service errors`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            coEvery { queueFilmService.addFilmToQueue(any(), any()) } throws RuntimeException("Database error")

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response =
                client.post("/queues/$queueId/films") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"tmdbId": 550, "title": "Fight Club"}""")
                }

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to add film to queue"))
        }

    @Test
    fun `DELETE queue films should remove film successfully`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550

            coEvery { queueFilmService.removeFilmFromQueue(queueId, filmTmdbId) } returns true

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.delete("/queues/$queueId/films/$filmTmdbId")

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Film removed from queue successfully"))

            coVerify { queueFilmService.removeFilmFromQueue(queueId, filmTmdbId) }
        }

    @Test
    fun `DELETE queue films should return not found when film not in queue`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550

            coEvery { queueFilmService.removeFilmFromQueue(queueId, filmTmdbId) } returns false

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.delete("/queues/$queueId/films/$filmTmdbId")

            // Then
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertTrue(response.bodyAsText().contains("Film not found in queue"))

            coVerify { queueFilmService.removeFilmFromQueue(queueId, filmTmdbId) }
        }

    @Test
    fun `DELETE queue films should return bad request for invalid queue ID`() =
        testApplication {
            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.delete("/queues/invalid-uuid/films/550")

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid queue ID"))
        }

    @Test
    fun `DELETE queue films should return bad request for invalid film TMDB ID`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.delete("/queues/$queueId/films/not-a-number")

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            val responseBody = response.bodyAsText()
            assertTrue(
                responseBody.contains("Invalid film TMDB ID format") ||
                    responseBody.contains("NumberFormatException") ||
                    responseBody.contains("For input string"),
            )
        }

    @Test
    fun `PUT queue films reorder should reorder films successfully`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val filmOrder = listOf(550, 238, 13)

            coEvery { queueFilmService.reorderQueueFilms(queueId, filmOrder) } returns true

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response =
                client.put("/queues/$queueId/films/reorder") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "filmOrder": [550, 238, 13]
                        }
                        """.trimIndent(),
                    )
                }

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Films reordered successfully"))

            coVerify { queueFilmService.reorderQueueFilms(queueId, filmOrder) }
        }

    @Test
    fun `PUT queue films reorder should return bad request for invalid queue ID`() =
        testApplication {
            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response =
                client.put("/queues/invalid-uuid/films/reorder") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"filmOrder": [550, 238]}""")
                }

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid queue ID"))
        }

    @Test
    fun `PUT queue films reorder should return bad request when queue ID is missing`() =
        testApplication {
            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response =
                client.put("/queues//films/reorder") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"filmOrder": [550]}""")
                }

            // Then
            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `PUT queue films reorder should handle service failure`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val filmOrder = listOf(550, 238)

            coEvery { queueFilmService.reorderQueueFilms(queueId, filmOrder) } returns false

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response =
                client.put("/queues/$queueId/films/reorder") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                            "filmOrder": [550, 238]
                        }
                        """.trimIndent(),
                    )
                }

            // Then
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Failed to reorder films"))

            coVerify { queueFilmService.reorderQueueFilms(queueId, filmOrder) }
        }

    @Test
    fun `PUT queue films reorder should handle service exceptions`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val filmOrder = listOf(550)

            coEvery { queueFilmService.reorderQueueFilms(queueId, filmOrder) } throws RuntimeException("Database error")

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response =
                client.put("/queues/$queueId/films/reorder") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"filmOrder": [550]}""")
                }

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to reorder films"))
            assertTrue(response.bodyAsText().contains("Database error"))
        }

    @Test
    fun `PUT queue films reorder should handle empty film order list`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val emptyOrder = emptyList<Int>()

            coEvery { queueFilmService.reorderQueueFilms(queueId, emptyOrder) } returns true

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response =
                client.put("/queues/$queueId/films/reorder") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"filmOrder": []}""")
                }

            // Then
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Films reordered successfully"))

            coVerify { queueFilmService.reorderQueueFilms(queueId, emptyOrder) }
        }

    @Test
    fun `DELETE queue films should handle service errors`() =
        testApplication {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550

            coEvery { queueFilmService.removeFilmFromQueue(queueId, filmTmdbId) } throws RuntimeException("Database error")

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository, queueFilmService)
                }
            }

            // When
            val response = client.delete("/queues/$queueId/films/$filmTmdbId")

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to remove film from queue"))
        }
}
