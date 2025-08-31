package me.jochum.filmqueuer.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueueFilmServiceTest {
    private lateinit var filmRepository: FilmRepository
    private lateinit var queueFilmRepository: QueueFilmRepository
    private lateinit var tmdbService: TmdbService
    private lateinit var service: QueueFilmService

    @BeforeEach
    fun setup() {
        filmRepository = mockk()
        queueFilmRepository = mockk()
        tmdbService = mockk()
        service = QueueFilmService(filmRepository, queueFilmRepository, tmdbService)
    }

    @Test
    fun `addFilmToQueue should save film and add to queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 550
            val film = Film(550, "Fight Club", null, LocalDate.of(1999, 10, 15))
            val queueFilm = QueueFilm(queueId, tmdbId, Instant.now())

            coEvery { tmdbService.getMovieDetails(tmdbId) } returns mockk(relaxed = true)
            coEvery { filmRepository.save(any()) } returns film
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tmdbId) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tmdbId) }
        }

    @Test
    fun `addFilmToQueue should handle film already exists in film repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 550
            val film = Film(550, "Fight Club", null, LocalDate.of(1999, 10, 15))
            val queueFilm = QueueFilm(queueId, tmdbId, Instant.now())

            coEvery { tmdbService.getMovieDetails(tmdbId) } returns mockk(relaxed = true)
            coEvery { filmRepository.save(any()) } returns film // replace handles duplicates
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tmdbId) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tmdbId) }
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
                    Film(550, "Fight Club", null, LocalDate.of(1999, 10, 15)),
                    Film(13, "Forrest Gump", null, LocalDate.of(1994, 7, 6)),
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
            val tmdbId = 550
            val film = Film(550, "Fight Club", null, LocalDate.of(1999, 10, 15))
            val exception = RuntimeException("Database error")

            coEvery { tmdbService.getMovieDetails(tmdbId) } returns mockk(relaxed = true)
            coEvery { filmRepository.save(any()) } returns film
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tmdbId) } throws exception

            // When & Then
            try {
                service.addFilmToQueue(queueId, tmdbId)
                assertTrue(false, "Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("Database error", e.message)
            }

            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tmdbId) }
        }

    @Test
    fun `reorderQueueFilms should delegate to repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmOrder = listOf(550, 238, 13)

            coEvery { queueFilmRepository.reorderQueueFilms(queueId, filmOrder) } returns true

            // When
            val result = service.reorderQueueFilms(queueId, filmOrder)

            // Then
            assertTrue(result)
            coVerify { queueFilmRepository.reorderQueueFilms(queueId, filmOrder) }
        }

    @Test
    fun `reorderQueueFilms should return false when repository fails`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmOrder = listOf(550, 238)

            coEvery { queueFilmRepository.reorderQueueFilms(queueId, filmOrder) } returns false

            // When
            val result = service.reorderQueueFilms(queueId, filmOrder)

            // Then
            assertFalse(result)
            coVerify { queueFilmRepository.reorderQueueFilms(queueId, filmOrder) }
        }

    @Test
    fun `reorderQueueFilms should handle empty film order list`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val emptyOrder = emptyList<Int>()

            coEvery { queueFilmRepository.reorderQueueFilms(queueId, emptyOrder) } returns true

            // When
            val result = service.reorderQueueFilms(queueId, emptyOrder)

            // Then
            assertTrue(result)
            coVerify { queueFilmRepository.reorderQueueFilms(queueId, emptyOrder) }
        }

    @Test
    fun `reorderQueueFilms should propagate repository exceptions`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmOrder = listOf(550, 238)
            val exception = RuntimeException("Database reorder failed")

            coEvery { queueFilmRepository.reorderQueueFilms(queueId, filmOrder) } throws exception

            // When & Then
            try {
                service.reorderQueueFilms(queueId, filmOrder)
                assertTrue(false, "Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("Database reorder failed", e.message)
            }

            coVerify { queueFilmRepository.reorderQueueFilms(queueId, filmOrder) }
        }

    @Test
    fun `service should handle film save failure`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 550
            val exception = RuntimeException("Film save failed")

            coEvery { tmdbService.getMovieDetails(tmdbId) } returns mockk(relaxed = true)
            coEvery { filmRepository.save(any()) } throws exception

            // When & Then
            try {
                service.addFilmToQueue(queueId, tmdbId)
                assertTrue(false, "Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("Film save failed", e.message)
            }

            coVerify { filmRepository.save(any()) }
            coVerify(exactly = 0) { queueFilmRepository.addFilmToQueue(any(), any()) }
        }
}
