package me.jochum.filmqueuer.adapters.persistence

import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.domain.Film
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MySqlQueueFilmRepositoryTest {
    private lateinit var repository: MySqlQueueFilmRepository
    private lateinit var filmRepository: MySqlFilmRepository

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(FilmTable, QueueTable, QueueFilmTable)
        }
        repository = MySqlQueueFilmRepository()
        filmRepository = MySqlFilmRepository()
    }

    @AfterEach
    fun cleanup() {
        transaction {
            QueueFilmTable.deleteAll()
            FilmTable.deleteAll()
            QueueTable.deleteAll()
        }
    }

    @Test
    fun `addFilmToQueue should add film to queue successfully`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550
            val film = Film(filmTmdbId, "Fight Club", null, "1999-10-15")

            // Create queue and film first
            filmRepository.save(film)
            createTestQueue(queueId)

            // When
            val result = repository.addFilmToQueue(queueId, filmTmdbId)

            // Then
            assertEquals(queueId, result.queueId)
            assertEquals(filmTmdbId, result.filmTmdbId)
            assertTrue(result.addedAt.isBefore(LocalDateTime.now().plusMinutes(1)))
            assertTrue(result.addedAt.isAfter(LocalDateTime.now().minusMinutes(1)))
        }

    @Test
    fun `removeFilmFromQueue should remove film successfully`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550
            val film = Film(filmTmdbId, "Fight Club", null, "1999-10-15")

            filmRepository.save(film)
            createTestQueue(queueId)
            repository.addFilmToQueue(queueId, filmTmdbId)

            // When
            val result = repository.removeFilmFromQueue(queueId, filmTmdbId)

            // Then
            assertTrue(result)
            assertFalse(repository.isFilmInQueue(queueId, filmTmdbId))
        }

    @Test
    fun `removeFilmFromQueue should return false when film not in queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550
            createTestQueue(queueId)

            // When
            val result = repository.removeFilmFromQueue(queueId, filmTmdbId)

            // Then
            assertFalse(result)
        }

    @Test
    fun `findFilmsByQueueId should return films ordered by addedAt`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val films =
                listOf(
                    Film(550, "Fight Club", null, "1999-10-15"),
                    Film(13, "Forrest Gump", null, "1994-07-06"),
                    Film(238, "The Godfather", null, "1972-03-14"),
                )

            createTestQueue(queueId)
            films.forEach { filmRepository.save(it) }

            // Add films to queue with small delays to ensure ordering
            films.forEach { film ->
                repository.addFilmToQueue(queueId, film.tmdbId)
                Thread.sleep(10) // Small delay to ensure different timestamps
            }

            // When
            val result = repository.findFilmsByQueueId(queueId)

            // Then
            assertEquals(3, result.size)
            assertEquals(550, result[0].tmdbId) // First added
            assertEquals(13, result[1].tmdbId) // Second added
            assertEquals(238, result[2].tmdbId) // Last added
        }

    @Test
    fun `findFilmsByQueueId should return empty list for empty queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            createTestQueue(queueId)

            // When
            val result = repository.findFilmsByQueueId(queueId)

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `findFilmsByQueueId should return empty list for non-existent queue`() =
        runBlocking {
            // Given
            val nonExistentQueueId = UUID.randomUUID()

            // When
            val result = repository.findFilmsByQueueId(nonExistentQueueId)

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `isFilmInQueue should return true when film is in queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550
            val film = Film(filmTmdbId, "Fight Club", null, "1999-10-15")

            filmRepository.save(film)
            createTestQueue(queueId)
            repository.addFilmToQueue(queueId, filmTmdbId)

            // When
            val result = repository.isFilmInQueue(queueId, filmTmdbId)

            // Then
            assertTrue(result)
        }

    @Test
    fun `isFilmInQueue should return false when film is not in queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550
            createTestQueue(queueId)

            // When
            val result = repository.isFilmInQueue(queueId, filmTmdbId)

            // Then
            assertFalse(result)
        }

    @Test
    fun `should handle multiple queues independently`() =
        runBlocking {
            // Given
            val queue1Id = UUID.randomUUID()
            val queue2Id = UUID.randomUUID()
            val filmTmdbId = 550
            val film = Film(filmTmdbId, "Fight Club", null, "1999-10-15")

            filmRepository.save(film)
            createTestQueue(queue1Id)
            createTestQueue(queue2Id)

            repository.addFilmToQueue(queue1Id, filmTmdbId)

            // When & Then
            assertTrue(repository.isFilmInQueue(queue1Id, filmTmdbId))
            assertFalse(repository.isFilmInQueue(queue2Id, filmTmdbId))

            assertEquals(1, repository.findFilmsByQueueId(queue1Id).size)
            assertEquals(0, repository.findFilmsByQueueId(queue2Id).size)
        }

    @Test
    fun `should prevent duplicate film entries in same queue`() {
        // Given
        val queueId = UUID.randomUUID()
        val filmTmdbId = 550
        val film = Film(filmTmdbId, "Fight Club", null, "1999-10-15")

        runBlocking {
            filmRepository.save(film)
            createTestQueue(queueId)
            repository.addFilmToQueue(queueId, filmTmdbId)

            // Verify film was added
            assertTrue(repository.isFilmInQueue(queueId, filmTmdbId))
            assertEquals(1, repository.findFilmsByQueueId(queueId).size)
        }

        // When - Try to add the same film again, should fail
        val exception =
            assertFailsWith<ExposedSQLException> {
                runBlocking {
                    repository.addFilmToQueue(queueId, filmTmdbId)
                }
            }

        assertTrue(exception.cause is JdbcSQLIntegrityConstraintViolationException)

        // Then - Should still only have one entry
        runBlocking {
            assertEquals(1, repository.findFilmsByQueueId(queueId).size)
        }
    }

    private suspend fun createTestQueue(queueId: UUID) {
        // Create a minimal queue entry for testing
        transaction {
            QueueTable.insert {
                it[id] = queueId
                it[type] = "PERSON"
                it[createdAt] = LocalDateTime.now()
            }
        }
    }
}
