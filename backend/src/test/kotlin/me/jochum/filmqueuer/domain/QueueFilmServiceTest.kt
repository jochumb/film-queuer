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
    private lateinit var personRepository: PersonRepository
    private lateinit var filmFactory: TmdbFilmFactory
    private lateinit var service: QueueFilmService

    @BeforeEach
    fun setup() {
        filmRepository = mockk()
        queueFilmRepository = mockk()
        tmdbService = mockk()
        personRepository = mockk()
        filmFactory = TmdbFilmFactory(tmdbService, personRepository)
        service = QueueFilmService(filmRepository, queueFilmRepository, filmFactory)
    }

    @Test
    fun `addFilmToQueue should save film and add to queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 550
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
            val queueFilm = QueueFilm(queueId, film.id, Instant.now())

            coEvery { tmdbService.getMovieDetails(tmdbId) } returns mockk(relaxed = true)
            coEvery { filmRepository.save(any()) } returns film
            coEvery { queueFilmRepository.addFilmToQueue(queueId, film.id) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, film.id) }
        }

    @Test
    fun `addFilmToQueue should handle film already exists in film repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 550
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
            val queueFilm = QueueFilm(queueId, film.id, Instant.now())

            coEvery { tmdbService.getMovieDetails(tmdbId) } returns mockk(relaxed = true)
            coEvery { filmRepository.save(any()) } returns film // insert-if-absent handles duplicates
            coEvery { queueFilmRepository.addFilmToQueue(queueId, film.id) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, film.id) }
        }

    @Test
    fun `removeFilmFromQueue should delegate to repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmId = UUID.randomUUID()

            coEvery { queueFilmRepository.removeFilmFromQueue(queueId, filmId) } returns true

            // When
            val result = service.removeFilmFromQueue(queueId, filmId)

            // Then
            assertTrue(result)
            coVerify { queueFilmRepository.removeFilmFromQueue(queueId, filmId) }
        }

    @Test
    fun `removeFilmFromQueue should return false when film not found`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmId = UUID.randomUUID()

            coEvery { queueFilmRepository.removeFilmFromQueue(queueId, filmId) } returns false

            // When
            val result = service.removeFilmFromQueue(queueId, filmId)

            // Then
            assertFalse(result)
            coVerify { queueFilmRepository.removeFilmFromQueue(queueId, filmId) }
        }

    @Test
    fun `getQueueFilms should return films from repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val expectedFilms =
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
            val filmId = UUID.randomUUID()

            coEvery { queueFilmRepository.isFilmInQueue(queueId, filmId) } returns true

            // When
            val result = service.isFilmInQueue(queueId, filmId)

            // Then
            assertTrue(result)
            coVerify { queueFilmRepository.isFilmInQueue(queueId, filmId) }
        }

    @Test
    fun `isFilmInQueue should return false when film not in queue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmId = UUID.randomUUID()

            coEvery { queueFilmRepository.isFilmInQueue(queueId, filmId) } returns false

            // When
            val result = service.isFilmInQueue(queueId, filmId)

            // Then
            assertFalse(result)
            coVerify { queueFilmRepository.isFilmInQueue(queueId, filmId) }
        }

    @Test
    fun `addFilmToQueue should propagate repository exceptions`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 550
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
            val exception = RuntimeException("Database error")

            coEvery { tmdbService.getMovieDetails(tmdbId) } returns mockk(relaxed = true)
            coEvery { filmRepository.save(any()) } returns film
            coEvery { queueFilmRepository.addFilmToQueue(queueId, film.id) } throws exception

            // When & Then
            try {
                service.addFilmToQueue(queueId, tmdbId)
                assertTrue(false, "Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("Database error", e.message)
            }

            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, film.id) }
        }

    @Test
    fun `reorderQueueFilms should delegate to repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val filmOrder = listOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

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
            val filmOrder = listOf(UUID.randomUUID(), UUID.randomUUID())

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
            val emptyOrder = emptyList<UUID>()

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
            val filmOrder = listOf(UUID.randomUUID(), UUID.randomUUID())
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
                    tmdbId = 1399,
                    title = "Game of Thrones",
                    originalTitle = "Game of Thrones",
                    releaseDate = LocalDate.of(2011, 4, 17),
                    runtime = 4560,
                    genres = listOf("Drama", "Action"),
                    posterPath = "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, tvShow.id, Instant.now())

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
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tvShow.id) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tvShow.id) }
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
                    tmdbId = 1399,
                    title = "Game of Thrones",
                    originalTitle = "Game of Thrones",
                    releaseDate = LocalDate.of(2011, 4, 17),
                    runtime = null,
                    genres = listOf("Drama"),
                    posterPath = "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, tvShow.id, Instant.now())

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
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tvShow.id) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tvShow.id) }
        }

    @Test
    fun `addFilmToQueue should handle TV show season fetch failure gracefully`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 1399
            val tvShow =
                Film(
                    tmdbId = 1399,
                    title = "Game of Thrones",
                    originalTitle = "Game of Thrones",
                    releaseDate = LocalDate.of(2011, 4, 17),
                    runtime = 60,
                    genres = listOf("Drama"),
                    posterPath = "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, tvShow.id, Instant.now())

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
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tvShow.id) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tvShow.id) }
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
                    tmdbId = 1399,
                    title = "Unknown TV Show",
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, fallbackTvShow.id, Instant.now())

            coEvery { tmdbService.getTvDetails(tmdbId) } throws RuntimeException("TMDB API error")
            coEvery { filmRepository.save(any()) } returns fallbackTvShow
            coEvery { queueFilmRepository.addFilmToQueue(queueId, fallbackTvShow.id) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, fallbackTvShow.id) }
        }

    @Test
    fun `addFilmToQueue should filter out season 0 specials when calculating TV runtime`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val tmdbId = 1399
            val tvShow =
                Film(
                    tmdbId = 1399,
                    title = "Game of Thrones",
                    originalTitle = "Game of Thrones",
                    releaseDate = LocalDate.of(2011, 4, 17),
                    runtime = 120,
                    genres = listOf("Drama"),
                    posterPath = "https://image.tmdb.org/t/p/w500/1XS1oqL89opfnbLl8WnZY1O1uJx.jpg",
                    tv = true,
                )
            val queueFilm = QueueFilm(queueId, tvShow.id, Instant.now())

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
            coEvery { queueFilmRepository.addFilmToQueue(queueId, tvShow.id) } returns queueFilm

            // When
            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            // Then
            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(any()) }
            coVerify { queueFilmRepository.addFilmToQueue(queueId, tvShow.id) }
            coVerify { tmdbService.getTvSeasonDetails(tmdbId, 1) }
            coVerify(exactly = 0) { tmdbService.getTvSeasonDetails(tmdbId, 0) }
        }

    @Test
    fun `addFilmToQueue should resolve a TV show's director from aggregate_credits`() =
        runBlocking {
            val queueId = UUID.randomUUID()
            val tmdbId = 87108
            val tvDetails =
                me.jochum.filmqueuer.adapters.tmdb.TmdbTvDetails(
                    id = tmdbId,
                    name = "Chernobyl",
                    firstAirDate = "2019-05-06",
                    seasons = emptyList(),
                    aggregateCredits =
                        me.jochum.filmqueuer.adapters.tmdb.TmdbAggregateCredits(
                            crew =
                                listOf(
                                    me.jochum.filmqueuer.adapters.tmdb.TmdbAggregateCrewMember(
                                        id = 212408,
                                        name = "Johan Renck",
                                        jobs =
                                            listOf(
                                                me.jochum.filmqueuer.adapters.tmdb.TmdbAggregateCrewJob(
                                                    job = "Director",
                                                    episodeCount = 5,
                                                ),
                                            ),
                                        totalEpisodeCount = 5,
                                    ),
                                ),
                        ),
                )
            val savedFilm = Film(tmdbId = tmdbId, title = "Chernobyl", tv = true, directorTmdbIds = listOf(212408))
            val queueFilm = QueueFilm(queueId, savedFilm.id, Instant.now())

            coEvery { tmdbService.getTvDetails(tmdbId) } returns tvDetails
            coEvery { personRepository.findByTmdbId(212408) } returns null
            coEvery { personRepository.save(any()) } returns mockk()
            coEvery { filmRepository.save(any()) } returns savedFilm
            coEvery { queueFilmRepository.addFilmToQueue(queueId, savedFilm.id) } returns queueFilm

            val result = service.addFilmToQueue(queueId, tmdbId, tv = true)

            assertEquals(queueFilm, result)
            coVerify { filmRepository.save(match { it.directorTmdbIds == listOf(212408) }) }
            coVerify { personRepository.save(match { it.tmdbId == 212408 && it.name == "Johan Renck" }) }
        }

    @Test
    fun `clearQueue should delegate to repository`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()

            coEvery { queueFilmRepository.deleteAllForQueue(queueId) } returns true

            // When
            val result = service.clearQueue(queueId)

            // Then
            assertTrue(result)
            coVerify { queueFilmRepository.deleteAllForQueue(queueId) }
        }
}
