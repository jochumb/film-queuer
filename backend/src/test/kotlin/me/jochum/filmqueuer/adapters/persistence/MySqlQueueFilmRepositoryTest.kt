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
import java.time.Instant
import java.time.LocalDate
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
            SchemaUtils.create(PersonTable, FilmTable, QueueTable, QueueFilmTable)
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
            PersonTable.deleteAll()
        }
    }

    @Test
    fun `addFilmToQueue should add film to queue successfully`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val film =
                Film(
                    tmdbId = 550,
                    title = "Fight Club",
                    originalTitle = "Fight Club",
                    releaseDate = LocalDate.of(1999, 10, 15),
                    runtime = 139,
                    genres = listOf("Drama", "Thriller"),
                    posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )

            // Create queue and film first
            val savedFilm = filmRepository.save(film)
            createTestQueue(queueId)

            // When
            val result = repository.addFilmToQueue(queueId, savedFilm.id)

            // Then
            assertEquals(queueId, result.queueId)
            assertEquals(savedFilm.id, result.filmId)
            assertTrue(result.addedAt.isBefore(Instant.now().plusSeconds(60)))
            assertTrue(result.addedAt.isAfter(Instant.now().minusSeconds(60)))
        }

    @Test
    fun `removeFilmFromQueue should remove film successfully`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val film =
                Film(
                    tmdbId = 550,
                    title = "Fight Club",
                    originalTitle = "Fight Club",
                    releaseDate = LocalDate.of(1999, 10, 15),
                    runtime = 139,
                    genres = listOf("Drama", "Thriller"),
                    posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )

            val savedFilm = filmRepository.save(film)
            createTestQueue(queueId)
            repository.addFilmToQueue(queueId, savedFilm.id)

            // When
            val result = repository.removeFilmFromQueue(queueId, savedFilm.id)

            // Then
            assertTrue(result)
            assertFalse(repository.isFilmInQueue(queueId, savedFilm.id))
        }

    @Test
    fun `removeFilmFromQueue should return false when film not in queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmId = UUID.randomUUID()
            createTestQueue(queueId)

            // When
            val result = repository.removeFilmFromQueue(queueId, filmId)

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
                    Film(
                        tmdbId = 550,
                        title = "Fight Club",
                        originalTitle = "Fight Club",
                        releaseDate = LocalDate.of(1999, 10, 15),
                        runtime = 139,
                        genres = listOf("Drama", "Thriller"),
                        posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                    ),
                    Film(
                        tmdbId = 13,
                        title = "Forrest Gump",
                        originalTitle = "Forrest Gump",
                        releaseDate = LocalDate.of(1994, 7, 6),
                        runtime = 142,
                        genres = listOf("Drama", "Romance"),
                        posterPath = "https://image.tmdb.org/t/p/w500/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
                    ),
                    Film(
                        tmdbId = 238,
                        title = "The Godfather",
                        originalTitle = "The Godfather",
                        releaseDate = LocalDate.of(1972, 3, 14),
                        runtime = 175,
                        genres = listOf("Crime", "Drama"),
                        posterPath = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                    ),
                )

            createTestQueue(queueId)
            val savedFilms = films.map { filmRepository.save(it) }

            // Add films to queue with small delays to ensure ordering
            savedFilms.forEach { film ->
                repository.addFilmToQueue(queueId, film.id)
                Thread.sleep(10) // Small delay to ensure different timestamps
            }

            // When
            val result = repository.findFilmsByQueueId(queueId)

            // Then
            assertEquals(3, result.size)

            // Validate first film (Fight Club) - complete field validation
            with(result[0]) {
                assertEquals(550, tmdbId)
                assertEquals("Fight Club", title)
                assertEquals("Fight Club", originalTitle)
                assertEquals(LocalDate.of(1999, 10, 15), releaseDate)
                assertEquals(139, runtime)
                assertEquals(listOf("Drama", "Thriller"), genres)
                assertEquals("https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg", posterPath)
            }

            // Validate second film (Forrest Gump)
            with(result[1]) {
                assertEquals(13, tmdbId)
                assertEquals("Forrest Gump", title)
                assertEquals(142, runtime)
                assertEquals(listOf("Drama", "Romance"), genres)
            }

            // Validate third film (The Godfather)
            with(result[2]) {
                assertEquals(238, tmdbId)
                assertEquals("The Godfather", title)
                assertEquals(175, runtime)
                assertEquals(listOf("Crime", "Drama"), genres)
            }
        }

    @Test
    fun `findFilmsByQueueId should return complete film data including enriched fields`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmWithAllFields =
                Film(
                    tmdbId = 550,
                    title = "Fight Club",
                    originalTitle = "Fight Club",
                    releaseDate = LocalDate.of(1999, 10, 15),
                    runtime = 139,
                    genres = listOf("Drama", "Thriller"),
                    posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )

            createTestQueue(queueId)
            val savedFilm = filmRepository.save(filmWithAllFields)
            repository.addFilmToQueue(queueId, savedFilm.id)

            // When
            val result = repository.findFilmsByQueueId(queueId)

            // Then - This test would have caught the missing field mapping bug!
            assertEquals(1, result.size)
            val retrievedFilm = result[0]

            // Validate ALL fields are correctly mapped from database
            assertEquals(filmWithAllFields.tmdbId, retrievedFilm.tmdbId)
            assertEquals(filmWithAllFields.title, retrievedFilm.title)
            assertEquals(filmWithAllFields.originalTitle, retrievedFilm.originalTitle)
            assertEquals(filmWithAllFields.releaseDate, retrievedFilm.releaseDate)
            assertEquals(filmWithAllFields.runtime, retrievedFilm.runtime) // ← Would fail before fix
            assertEquals(filmWithAllFields.genres, retrievedFilm.genres) // ← Would fail before fix
            assertEquals(filmWithAllFields.posterPath, retrievedFilm.posterPath) // ← Would fail before fix
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
            val film =
                Film(
                    tmdbId = 550,
                    title = "Fight Club",
                    originalTitle = "Fight Club",
                    releaseDate = LocalDate.of(1999, 10, 15),
                    runtime = 139,
                    genres = listOf("Drama", "Thriller"),
                    posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )

            val savedFilm = filmRepository.save(film)
            createTestQueue(queueId)
            repository.addFilmToQueue(queueId, savedFilm.id)

            // When
            val result = repository.isFilmInQueue(queueId, savedFilm.id)

            // Then
            assertTrue(result)
        }

    @Test
    fun `isFilmInQueue should return false when film is not in queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmId = UUID.randomUUID()
            createTestQueue(queueId)

            // When
            val result = repository.isFilmInQueue(queueId, filmId)

            // Then
            assertFalse(result)
        }

    @Test
    fun `should handle multiple queues independently`() =
        runBlocking {
            // Given
            val queue1Id = UUID.randomUUID()
            val queue2Id = UUID.randomUUID()
            val film =
                Film(
                    tmdbId = 550,
                    title = "Fight Club",
                    originalTitle = "Fight Club",
                    releaseDate = LocalDate.of(1999, 10, 15),
                    runtime = 139,
                    genres = listOf("Drama", "Thriller"),
                    posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )

            val savedFilm = filmRepository.save(film)
            createTestQueue(queue1Id)
            createTestQueue(queue2Id)

            repository.addFilmToQueue(queue1Id, savedFilm.id)

            // When & Then
            assertTrue(repository.isFilmInQueue(queue1Id, savedFilm.id))
            assertFalse(repository.isFilmInQueue(queue2Id, savedFilm.id))

            assertEquals(1, repository.findFilmsByQueueId(queue1Id).size)
            assertEquals(0, repository.findFilmsByQueueId(queue2Id).size)
        }

    @Test
    fun `reorderQueueFilms should update sort order correctly`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val films =
                listOf(
                    Film(
                        tmdbId = 550,
                        title = "Fight Club",
                        originalTitle = "Fight Club",
                        releaseDate = LocalDate.of(1999, 10, 15),
                        runtime = 139,
                        genres = listOf("Drama", "Thriller"),
                        posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                    ),
                    Film(
                        tmdbId = 13,
                        title = "Forrest Gump",
                        originalTitle = "Forrest Gump",
                        releaseDate = LocalDate.of(1994, 7, 6),
                        runtime = 142,
                        genres = listOf("Drama", "Romance"),
                        posterPath = "https://image.tmdb.org/t/p/w500/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
                    ),
                    Film(
                        tmdbId = 238,
                        title = "The Godfather",
                        originalTitle = "The Godfather",
                        releaseDate = LocalDate.of(1972, 3, 14),
                        runtime = 175,
                        genres = listOf("Crime", "Drama"),
                        posterPath = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                    ),
                )

            createTestQueue(queueId)
            val savedFilms = films.map { filmRepository.save(it) }

            // Add films to queue
            savedFilms.forEach { film ->
                repository.addFilmToQueue(queueId, film.id)
            }

            // When - Reorder films (reverse order)
            val godfather = savedFilms.first { it.tmdbId == 238 }
            val gump = savedFilms.first { it.tmdbId == 13 }
            val fightClub = savedFilms.first { it.tmdbId == 550 }
            val newOrder = listOf(godfather.id, gump.id, fightClub.id)
            val result = repository.reorderQueueFilms(queueId, newOrder)

            // Then
            assertTrue(result)

            val reorderedFilms = repository.findFilmsByQueueId(queueId)
            assertEquals(3, reorderedFilms.size)
            assertEquals(238, reorderedFilms[0].tmdbId) // The Godfather first
            assertEquals(13, reorderedFilms[1].tmdbId) // Forrest Gump second
            assertEquals(550, reorderedFilms[2].tmdbId) // Fight Club last
        }

    @Test
    fun `reorderQueueFilms should handle partial reorder`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val films =
                listOf(
                    Film(
                        tmdbId = 550,
                        title = "Fight Club",
                        originalTitle = "Fight Club",
                        releaseDate = LocalDate.of(1999, 10, 15),
                        runtime = 139,
                        genres = listOf("Drama", "Thriller"),
                        posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                    ),
                    Film(
                        tmdbId = 13,
                        title = "Forrest Gump",
                        originalTitle = "Forrest Gump",
                        releaseDate = LocalDate.of(1994, 7, 6),
                        runtime = 142,
                        genres = listOf("Drama", "Romance"),
                        posterPath = "https://image.tmdb.org/t/p/w500/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
                    ),
                    Film(
                        tmdbId = 238,
                        title = "The Godfather",
                        originalTitle = "The Godfather",
                        releaseDate = LocalDate.of(1972, 3, 14),
                        runtime = 175,
                        genres = listOf("Crime", "Drama"),
                        posterPath = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                    ),
                )

            createTestQueue(queueId)
            val savedFilms = films.map { filmRepository.save(it) }
            savedFilms.forEach { film ->
                repository.addFilmToQueue(queueId, film.id)
            }

            // When - Only reorder first two films
            val gump = savedFilms.first { it.tmdbId == 13 }
            val fightClub = savedFilms.first { it.tmdbId == 550 }
            val partialOrder = listOf(gump.id, fightClub.id)
            val result = repository.reorderQueueFilms(queueId, partialOrder)

            // Then
            assertTrue(result)

            val reorderedFilms = repository.findFilmsByQueueId(queueId)
            assertEquals(3, reorderedFilms.size)
            assertEquals(13, reorderedFilms[0].tmdbId) // Forrest Gump first
            assertEquals(550, reorderedFilms[1].tmdbId) // Fight Club second
            // The Godfather should remain in its original position with higher sort order
        }

    @Test
    fun `reorderQueueFilms should handle empty order list`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            createTestQueue(queueId)

            // When
            val result = repository.reorderQueueFilms(queueId, emptyList())

            // Then
            assertTrue(result) // Should succeed even with empty list
        }

    @Test
    fun `reorderQueueFilms should handle non-existent films gracefully`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val film = Film(tmdbId = 550, title = "Fight Club", releaseDate = LocalDate.of(1999, 10, 15))

            createTestQueue(queueId)
            val savedFilm = filmRepository.save(film)
            repository.addFilmToQueue(queueId, savedFilm.id)

            // When - Try to reorder with a non-existent film ID
            val orderWithNonExistent = listOf(savedFilm.id, UUID.randomUUID())
            val result = repository.reorderQueueFilms(queueId, orderWithNonExistent)

            // Then
            assertTrue(result) // Should still succeed

            val films = repository.findFilmsByQueueId(queueId)
            assertEquals(1, films.size)
            assertEquals(550, films[0].tmdbId)
        }

    @Test
    fun `findFilmsByQueueId should return films ordered by sortOrder`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val films =
                listOf(
                    Film(
                        tmdbId = 550,
                        title = "Fight Club",
                        originalTitle = "Fight Club",
                        releaseDate = LocalDate.of(1999, 10, 15),
                        runtime = 139,
                        genres = listOf("Drama", "Thriller"),
                        posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                    ),
                    Film(
                        tmdbId = 13,
                        title = "Forrest Gump",
                        originalTitle = "Forrest Gump",
                        releaseDate = LocalDate.of(1994, 7, 6),
                        runtime = 142,
                        genres = listOf("Drama", "Romance"),
                        posterPath = "https://image.tmdb.org/t/p/w500/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
                    ),
                    Film(
                        tmdbId = 238,
                        title = "The Godfather",
                        originalTitle = "The Godfather",
                        releaseDate = LocalDate.of(1972, 3, 14),
                        runtime = 175,
                        genres = listOf("Crime", "Drama"),
                        posterPath = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                    ),
                )

            createTestQueue(queueId)
            val savedFilms = films.map { filmRepository.save(it) }

            // Add films in one order
            savedFilms.forEach { film ->
                repository.addFilmToQueue(queueId, film.id)
                Thread.sleep(10) // Small delay to ensure different timestamps
            }

            // Reorder them differently
            val godfather = savedFilms.first { it.tmdbId == 238 }
            val fightClub = savedFilms.first { it.tmdbId == 550 }
            val gump = savedFilms.first { it.tmdbId == 13 }
            repository.reorderQueueFilms(queueId, listOf(godfather.id, fightClub.id, gump.id))

            // When
            val result = repository.findFilmsByQueueId(queueId)

            // Then - Should be ordered by sortOrder, not addedAt
            assertEquals(3, result.size)
            assertEquals(238, result[0].tmdbId) // The Godfather (sortOrder 0)
            assertEquals(550, result[1].tmdbId) // Fight Club (sortOrder 1)
            assertEquals(13, result[2].tmdbId) // Forrest Gump (sortOrder 2)
        }

    @Test
    fun `addFilmToQueue should assign next available sort order`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val films =
                listOf(
                    Film(tmdbId = 550, title = "Fight Club", releaseDate = LocalDate.of(1999, 10, 15)),
                    Film(tmdbId = 13, title = "Forrest Gump", releaseDate = LocalDate.of(1994, 7, 6)),
                )

            createTestQueue(queueId)
            val savedFilms = films.map { filmRepository.save(it) }

            // When - Add films sequentially
            val result1 = repository.addFilmToQueue(queueId, savedFilms[0].id)
            val result2 = repository.addFilmToQueue(queueId, savedFilms[1].id)

            // Then - Each film should get the next sort order
            assertEquals(0, result1.sortOrder)
            assertEquals(1, result2.sortOrder)

            val retrievedFilms = repository.findFilmsByQueueId(queueId)
            assertEquals(550, retrievedFilms[0].tmdbId) // First added, sortOrder 0
            assertEquals(13, retrievedFilms[1].tmdbId) // Second added, sortOrder 1
        }

    @Test
    fun `should prevent duplicate film entries in same queue`() {
        // Given
        val queueId = UUID.randomUUID()
        val film =
            Film(
                tmdbId = 550,
                title = "Fight Club",
                originalTitle = "Fight Club",
                releaseDate = LocalDate.of(1999, 10, 15),
                runtime = 139,
                genres = listOf("Drama", "Thriller"),
                posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
            )

        lateinit var savedFilmId: UUID
        runBlocking {
            val savedFilm = filmRepository.save(film)
            savedFilmId = savedFilm.id
            createTestQueue(queueId)
            repository.addFilmToQueue(queueId, savedFilmId)

            // Verify film was added
            assertTrue(repository.isFilmInQueue(queueId, savedFilmId))
            assertEquals(1, repository.findFilmsByQueueId(queueId).size)
        }

        // When - Try to add the same film again, should fail
        val exception =
            assertFailsWith<ExposedSQLException> {
                runBlocking {
                    repository.addFilmToQueue(queueId, savedFilmId)
                }
            }

        assertTrue(exception.cause is JdbcSQLIntegrityConstraintViolationException)

        // Then - Should still only have one entry
        runBlocking {
            assertEquals(1, repository.findFilmsByQueueId(queueId).size)
        }
    }

    @Test
    fun `deleteAllForQueue should remove all films for the queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val otherQueueId = UUID.randomUUID()
            val film1 =
                Film(
                    tmdbId = 550,
                    title = "Fight Club",
                    originalTitle = "Fight Club",
                    releaseDate = LocalDate.of(1999, 10, 15),
                    runtime = 139,
                    genres = listOf("Drama", "Thriller"),
                    posterPath = "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )
            val film2 =
                Film(
                    tmdbId = 238,
                    title = "The Godfather",
                    originalTitle = "The Godfather",
                    releaseDate = LocalDate.of(1972, 3, 14),
                    runtime = 175,
                    genres = listOf("Drama", "Crime"),
                    posterPath = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                )

            val savedFilm1 = filmRepository.save(film1)
            val savedFilm2 = filmRepository.save(film2)
            createTestQueue(queueId)
            createTestQueue(otherQueueId)
            repository.addFilmToQueue(queueId, savedFilm1.id)
            repository.addFilmToQueue(queueId, savedFilm2.id)
            repository.addFilmToQueue(otherQueueId, savedFilm1.id)

            // When
            val result = repository.deleteAllForQueue(queueId)

            // Then
            assertTrue(result)
            assertEquals(0, repository.findFilmsByQueueId(queueId).size)
            assertEquals(1, repository.findFilmsByQueueId(otherQueueId).size)
        }

    @Test
    fun `deleteAllForQueue should return true when queue has no films`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            createTestQueue(queueId)

            // When
            val result = repository.deleteAllForQueue(queueId)

            // Then
            assertTrue(result)
        }

    private suspend fun createTestQueue(queueId: UUID) {
        // Create a minimal queue entry for testing
        transaction {
            QueueTable.insert {
                it[id] = queueId
                it[type] = "PERSON"
                it[createdAt] = Instant.now()
            }
        }
    }
}
