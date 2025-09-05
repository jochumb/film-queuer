package me.jochum.filmqueuer.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
            val film =
                Film(
                    550,
                    "Fight Club",
                    "Fight Club",
                    LocalDate.of(1999, 10, 15),
                    139,
                    listOf("Drama", "Thriller"),
                    "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )
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
            val film =
                Film(
                    550,
                    "Fight Club",
                    "Fight Club",
                    LocalDate.of(1999, 10, 15),
                    139,
                    listOf("Drama", "Thriller"),
                    "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )
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
                    Film(
                        550,
                        "Fight Club",
                        "Fight Club",
                        LocalDate.of(1999, 10, 15),
                        139,
                        listOf("Drama", "Thriller"),
                        "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                    ),
                    Film(
                        13,
                        "Forrest Gump",
                        "Forrest Gump",
                        LocalDate.of(1994, 7, 6),
                        142,
                        listOf("Drama", "Romance"),
                        "https://image.tmdb.org/t/p/w500/arw2vcBveWOVZr6pxd9XTd1TdQa.jpg",
                    ),
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
            val film =
                Film(
                    550,
                    "Fight Club",
                    "Fight Club",
                    LocalDate.of(1999, 10, 15),
                    139,
                    listOf("Drama", "Thriller"),
                    "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )
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

    @Test
    fun `addFilmToQueue should save TV show and add to queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 1399
            val tvShow =
                Film(
                    1399,
                    "Game of Thrones",
                    "Game of Thrones",
                    LocalDate.of(2011, 4, 17),
                    4560,
                    listOf("Drama", "Action"),
                    "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, tmdbId, Instant.now())

            val mockTvDetails = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbTvDetails>(relaxed = true)
            val mockSeason1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeason>(relaxed = true)
            val mockSeason2 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeason>(relaxed = true)
            val mockSeasonDetails1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeasonDetails>(relaxed = true)
            val mockSeasonDetails2 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeasonDetails>(relaxed = true)
            val mockEpisode1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbEpisode>(relaxed = true)
            val mockEpisode2 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbEpisode>(relaxed = true)
            val mockGenre = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbGenre>(relaxed = true)

            every { mockTvDetails.name } returns "Game of Thrones"
            every { mockTvDetails.originalName } returns "Game of Thrones"
            every { mockTvDetails.firstAirDate } returns "2011-04-17"
            every { mockTvDetails.posterPath } returns "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg"
            every { mockTvDetails.genres } returns listOf(mockGenre)
            every { mockTvDetails.seasons } returns listOf(mockSeason1, mockSeason2)
            every { mockGenre.name } returns "Drama"
            every { mockSeason1.seasonNumber } returns 1
            every { mockSeason2.seasonNumber } returns 2
            every { mockSeasonDetails1.episodes } returns listOf(mockEpisode1, mockEpisode2)
            every { mockSeasonDetails2.episodes } returns listOf(mockEpisode1)
            every { mockEpisode1.runtime } returns 60
            every { mockEpisode2.runtime } returns 50

            coEvery { tmdbService.getTvDetails(tmdbId) } returns mockTvDetails
            coEvery { tmdbService.getTvSeasonDetails(tmdbId, 1) } returns mockSeasonDetails1
            coEvery { tmdbService.getTvSeasonDetails(tmdbId, 2) } returns mockSeasonDetails2
            coEvery { filmRepository.save(any()) } returns tvShow
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tmdbId) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tmdbId) }
            coVerify { tmdbService.getTvDetails(tmdbId) }
            coVerify { tmdbService.getTvSeasonDetails(tmdbId, 1) }
            coVerify { tmdbService.getTvSeasonDetails(tmdbId, 2) }
        }

    @Test
    fun `addFilmToQueue should handle TV show with missing runtime data`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 1399
            val tvShow =
                Film(
                    1399,
                    "Game of Thrones",
                    "Game of Thrones",
                    LocalDate.of(2011, 4, 17),
                    null,
                    listOf("Drama"),
                    "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, tmdbId, Instant.now())

            val mockTvDetails = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbTvDetails>(relaxed = true)
            val mockSeason1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeason>(relaxed = true)
            val mockSeasonDetails1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeasonDetails>(relaxed = true)
            val mockEpisode1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbEpisode>(relaxed = true)
            val mockGenre = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbGenre>(relaxed = true)

            every { mockTvDetails.name } returns "Game of Thrones"
            every { mockTvDetails.originalName } returns "Game of Thrones"
            every { mockTvDetails.firstAirDate } returns "2011-04-17"
            every { mockTvDetails.posterPath } returns "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg"
            every { mockTvDetails.genres } returns listOf(mockGenre)
            every { mockTvDetails.seasons } returns listOf(mockSeason1)
            every { mockGenre.name } returns "Drama"
            every { mockSeason1.seasonNumber } returns 1
            every { mockSeasonDetails1.episodes } returns listOf(mockEpisode1)
            every { mockEpisode1.runtime } returns null

            coEvery { tmdbService.getTvDetails(tmdbId) } returns mockTvDetails
            coEvery { tmdbService.getTvSeasonDetails(tmdbId, 1) } returns mockSeasonDetails1
            coEvery { filmRepository.save(any()) } returns tvShow
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tmdbId) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tmdbId) }
        }

    @Test
    fun `addFilmToQueue should handle TV show season fetch failure gracefully`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 1399
            val tvShow =
                Film(
                    1399,
                    "Game of Thrones",
                    "Game of Thrones",
                    LocalDate.of(2011, 4, 17),
                    60,
                    listOf("Drama"),
                    "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, tmdbId, Instant.now())

            val mockTvDetails = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbTvDetails>(relaxed = true)
            val mockSeason1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeason>(relaxed = true)
            val mockSeason2 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeason>(relaxed = true)
            val mockSeasonDetails1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeasonDetails>(relaxed = true)
            val mockEpisode1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbEpisode>(relaxed = true)
            val mockGenre = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbGenre>(relaxed = true)

            every { mockTvDetails.name } returns "Game of Thrones"
            every { mockTvDetails.originalName } returns "Game of Thrones"
            every { mockTvDetails.firstAirDate } returns "2011-04-17"
            every { mockTvDetails.posterPath } returns "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg"
            every { mockTvDetails.genres } returns listOf(mockGenre)
            every { mockTvDetails.seasons } returns listOf(mockSeason1, mockSeason2)
            every { mockGenre.name } returns "Drama"
            every { mockSeason1.seasonNumber } returns 1
            every { mockSeason2.seasonNumber } returns 2
            every { mockSeasonDetails1.episodes } returns listOf(mockEpisode1)
            every { mockEpisode1.runtime } returns 60

            coEvery { tmdbService.getTvDetails(tmdbId) } returns mockTvDetails
            coEvery { tmdbService.getTvSeasonDetails(tmdbId, 1) } returns mockSeasonDetails1
            coEvery { tmdbService.getTvSeasonDetails(tmdbId, 2) } throws RuntimeException("Season not found")
            coEvery { filmRepository.save(any()) } returns tvShow
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tmdbId) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tmdbId) }
            coVerify { tmdbService.getTvSeasonDetails(tmdbId, 1) }
            coVerify { tmdbService.getTvSeasonDetails(tmdbId, 2) }
        }

    @Test
    fun `addFilmToQueue should create fallback TV show when TMDB fetch fails`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 1399
            val fallbackTvShow =
                Film(
                    1399,
                    "Unknown TV Show",
                    null,
                    null,
                    null,
                    null,
                    null,
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, tmdbId, Instant.now())

            coEvery { tmdbService.getTvDetails(tmdbId) } throws RuntimeException("TMDB API error")
            coEvery { filmRepository.save(any()) } returns fallbackTvShow
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tmdbId) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tmdbId) }
        }

    @Test
    fun `addFilmToQueue should filter out season 0 specials when calculating TV runtime`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 1399
            val tvShow =
                Film(
                    1399,
                    "Game of Thrones",
                    "Game of Thrones",
                    LocalDate.of(2011, 4, 17),
                    120,
                    listOf("Drama"),
                    "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, tmdbId, Instant.now())

            val mockTvDetails = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbTvDetails>(relaxed = true)
            val mockSpecialsSeason = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeason>(relaxed = true)
            val mockSeason1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeason>(relaxed = true)
            val mockSeasonDetails1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbSeasonDetails>(relaxed = true)
            val mockEpisode1 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbEpisode>(relaxed = true)
            val mockEpisode2 = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbEpisode>(relaxed = true)
            val mockGenre = mockk<me.jochum.filmqueuer.adapters.tmdb.TmdbGenre>(relaxed = true)

            every { mockTvDetails.name } returns "Game of Thrones"
            every { mockTvDetails.originalName } returns "Game of Thrones"
            every { mockTvDetails.firstAirDate } returns "2011-04-17"
            every { mockTvDetails.posterPath } returns "/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg"
            every { mockTvDetails.genres } returns listOf(mockGenre)
            every { mockTvDetails.seasons } returns listOf(mockSpecialsSeason, mockSeason1)
            every { mockGenre.name } returns "Drama"
            every { mockSpecialsSeason.seasonNumber } returns 0
            every { mockSeason1.seasonNumber } returns 1
            every { mockSeasonDetails1.episodes } returns listOf(mockEpisode1, mockEpisode2)
            every { mockEpisode1.runtime } returns 60
            every { mockEpisode2.runtime } returns 60

            coEvery { tmdbService.getTvDetails(tmdbId) } returns mockTvDetails
            coEvery { tmdbService.getTvSeasonDetails(tmdbId, 1) } returns mockSeasonDetails1
            coEvery { filmRepository.save(any()) } returns tvShow
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tmdbId) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tmdbId) }
            coVerify { tmdbService.getTvSeasonDetails(tmdbId, 1) }
            coVerify(exactly = 0) { tmdbService.getTvSeasonDetails(tmdbId, 0) }
        }
}
