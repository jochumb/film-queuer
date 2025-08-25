package me.jochum.filmqueuer.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueueFilmServiceTest {
    private lateinit var filmRepository: FilmRepository
    private lateinit var queueFilmRepository: QueueFilmRepository
    private lateinit var service: QueueFilmService

    @BeforeEach
    fun setup() {
        filmRepository = mockk()
        queueFilmRepository = mockk()
        service = QueueFilmService(filmRepository, queueFilmRepository)
    }

    @Test
    fun `addFilmToQueue should save film and add to queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val film = Film(550, "Fight Club", null, "1999-10-15")
            val queueFilm = QueueFilm(queueId, film.tmdbId, LocalDateTime.now())

            coEvery { filmRepository.save(film) } returns film
            coEvery { queueFilmRepository.addFilmToQueue(queueId, film.tmdbId) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, film)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(film) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, film.tmdbId) }
        }

    @Test
    fun `addFilmToQueue should handle film already exists in film repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val film = Film(550, "Fight Club", null, "1999-10-15")
            val queueFilm = QueueFilm(queueId, film.tmdbId, LocalDateTime.now())

            coEvery { filmRepository.save(film) } returns film // insertIgnore handles duplicates
            coEvery { queueFilmRepository.addFilmToQueue(queueId, film.tmdbId) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, film)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(film) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, film.tmdbId) }
        }

    @Test
    fun `removeFilmFromQueue should delegate to repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550

            coEvery { queueFilmRepository.removeFilmFromQueue(queueId, filmTmdbId) } returns true

            // When
            val result = service.removeFilmFromQueue(queueId, filmTmdbId)

            // Then
            assertTrue(result)
            coVerify { queueFilmRepository.removeFilmFromQueue(queueId, filmTmdbId) }
        }

    @Test
    fun `removeFilmFromQueue should return false when film not found`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550

            coEvery { queueFilmRepository.removeFilmFromQueue(queueId, filmTmdbId) } returns false

            // When
            val result = service.removeFilmFromQueue(queueId, filmTmdbId)

            // Then
            assertFalse(result)
            coVerify { queueFilmRepository.removeFilmFromQueue(queueId, filmTmdbId) }
        }

    @Test
    fun `getQueueFilms should return films from repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val expectedFilms =
                listOf(
                    Film(550, "Fight Club", null, "1999-10-15"),
                    Film(13, "Forrest Gump", null, "1994-07-06"),
                )

            coEvery { queueFilmRepository.findFilmsByQueueId(queueId) } returns expectedFilms

            // When
            val result = service.getQueueFilms(queueId)

            // Then
            assertEquals(expectedFilms, result)
            coVerify { queueFilmRepository.findFilmsByQueueId(queueId) }
        }

    @Test
    fun `getQueueFilms should return empty list for empty queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()

            coEvery { queueFilmRepository.findFilmsByQueueId(queueId) } returns emptyList()

            // When
            val result = service.getQueueFilms(queueId)

            // Then
            assertTrue(result.isEmpty())
            coVerify { queueFilmRepository.findFilmsByQueueId(queueId) }
        }

    @Test
    fun `isFilmInQueue should delegate to repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550

            coEvery { queueFilmRepository.isFilmInQueue(queueId, filmTmdbId) } returns true

            // When
            val result = service.isFilmInQueue(queueId, filmTmdbId)

            // Then
            assertTrue(result)
            coVerify { queueFilmRepository.isFilmInQueue(queueId, filmTmdbId) }
        }

    @Test
    fun `isFilmInQueue should return false when film not in queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmTmdbId = 550

            coEvery { queueFilmRepository.isFilmInQueue(queueId, filmTmdbId) } returns false

            // When
            val result = service.isFilmInQueue(queueId, filmTmdbId)

            // Then
            assertFalse(result)
            coVerify { queueFilmRepository.isFilmInQueue(queueId, filmTmdbId) }
        }

    @Test
    fun `addFilmToQueue should propagate repository exceptions`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val film = Film(550, "Fight Club", null, "1999-10-15")
            val exception = RuntimeException("Database error")

            coEvery { filmRepository.save(film) } returns film
            coEvery { queueFilmRepository.addFilmToQueue(queueId, film.tmdbId) } throws exception

            // When & Then
            try {
                service.addFilmToQueue(queueId, film)
                assertTrue(false, "Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("Database error", e.message)
            }

            coVerify { filmRepository.save(film) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, film.tmdbId) }
        }

    @Test
    fun `service should handle film save failure`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val film = Film(550, "Fight Club", null, "1999-10-15")
            val exception = RuntimeException("Film save failed")

            coEvery { filmRepository.save(film) } throws exception

            // When & Then
            try {
                service.addFilmToQueue(queueId, film)
                assertTrue(false, "Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("Film save failed", e.message)
            }

            coVerify { filmRepository.save(film) }
            coVerify(exactly = 0) { queueFilmRepository.addFilmToQueue(any(), any()) }
        }
}
