package me.jochum.filmqueuer.adapters.web

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import me.jochum.filmqueuer.domain.Department
import me.jochum.filmqueuer.domain.Person
import me.jochum.filmqueuer.domain.PersonQueue
import me.jochum.filmqueuer.domain.PersonRepository
import me.jochum.filmqueuer.domain.QueueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueueControllerTest {
    private lateinit var queueRepository: QueueRepository
    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun setup() {
        queueRepository = mockk()
        personRepository = mockk()
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
            val createdAt = LocalDateTime.now()
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
                    configureQueueRoutes(queueRepository, personRepository)
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
            val createdAt = LocalDateTime.now()
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
                    configureQueueRoutes(queueRepository, personRepository)
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
                    configureQueueRoutes(queueRepository, personRepository)
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
            val createdAt = LocalDateTime.now()

            val queue1 = PersonQueue(queue1Id, 123, createdAt)
            val queue2 = PersonQueue(queue2Id, 456, createdAt)

            coEvery { queueRepository.findAll() } returns listOf(queue1, queue2)
            coEvery { personRepository.findByTmdbId(123) } returns person1
            coEvery { personRepository.findByTmdbId(456) } returns person2

            application {
                configureSerialization()
                routing {
                    configureQueueRoutes(queueRepository, personRepository)
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
                    configureQueueRoutes(queueRepository, personRepository)
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
            val personQueue = PersonQueue(queueId, 123, LocalDateTime.now())

            coEvery { queueRepository.findAll() } returns listOf(personQueue)
            coEvery { personRepository.findByTmdbId(123) } throws RuntimeException("Person lookup failed")

            application {
                routing {
                    configureQueueRoutes(queueRepository, personRepository)
                }
            }

            // When
            val response = client.get("/queues")

            // Then
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to fetch queues"))
        }
}
