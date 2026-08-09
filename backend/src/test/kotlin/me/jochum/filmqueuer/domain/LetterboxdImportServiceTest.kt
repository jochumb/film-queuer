package me.jochum.filmqueuer.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.adapters.tmdb.TmdbAggregateCredits
import me.jochum.filmqueuer.adapters.tmdb.TmdbAggregateCrewJob
import me.jochum.filmqueuer.adapters.tmdb.TmdbAggregateCrewMember
import me.jochum.filmqueuer.adapters.tmdb.TmdbCredits
import me.jochum.filmqueuer.adapters.tmdb.TmdbCrewMember
import me.jochum.filmqueuer.adapters.tmdb.TmdbMovie
import me.jochum.filmqueuer.adapters.tmdb.TmdbMovieDetails
import me.jochum.filmqueuer.adapters.tmdb.TmdbMovieSearchResponse
import me.jochum.filmqueuer.adapters.tmdb.TmdbService
import me.jochum.filmqueuer.adapters.tmdb.TmdbTvDetails
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.SQLTransactionRollbackException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LetterboxdImportServiceTest {
    private lateinit var externalFilmRefRepository: ExternalFilmRefRepository
    private lateinit var filmRepository: FilmRepository
    private lateinit var personRepository: PersonRepository
    private lateinit var tmdbService: TmdbService
    private lateinit var filmFactory: TmdbFilmFactory
    private lateinit var service: LetterboxdImportService

    @BeforeEach
    fun setup() {
        externalFilmRefRepository = mockk()
        filmRepository = mockk()
        personRepository = mockk()
        tmdbService = mockk()
        filmFactory = TmdbFilmFactory(tmdbService, personRepository)
        service = LetterboxdImportService(externalFilmRefRepository, filmRepository, personRepository, tmdbService, filmFactory)
    }

    private val collectionCsv =
        """
        Letterboxd list export v7
        Date,Name,Tags,URL,Description
        2023-09-30,Blu-ray Collection,own,https://boxd.it/x,

        Position,Name,Year,URL,Description
        1,Fight Club,1999,https://boxd.it/a,
        """.trimIndent()

    private val watchedCsv =
        """
        Date,Name,Year,Letterboxd URI
        2020-01-01,Fight Club,1999,https://boxd.it/a
        """.trimIndent()

    private fun movieSearchResult(
        id: Int = 550,
        title: String = "Fight Club",
        releaseDate: String? = "1999-10-15",
    ) = TmdbMovieSearchResponse(
        page = 1,
        results = listOf(TmdbMovie(id = id, title = title, releaseDate = releaseDate)),
        totalPages = 1,
        totalResults = 1,
    )

    private fun movieDetails(id: Int = 550) =
        TmdbMovieDetails(
            id = id,
            title = "Fight Club",
            releaseDate = "1999-10-15",
            runtime = 139,
        )

    @Test
    fun `importCollection should create a new owned ref and auto-match an unambiguous title`() =
        runBlocking {
            coEvery { externalFilmRefRepository.findBySourceTitleYear("LETTERBOXD", "Fight Club", 1999) } returns null
            coEvery { externalFilmRefRepository.save(any()) } returns mockk()
            coEvery { externalFilmRefRepository.update(any()) } returns true
            coEvery { tmdbService.searchMovies("Fight Club", 1999) } returns movieSearchResult()
            coEvery { tmdbService.getMovieDetails(550) } returns movieDetails()
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } returns mockk()

            val summary = service.importCollection(collectionCsv)

            assertEquals(1, summary.totalRows)
            assertEquals(1, summary.created)
            assertEquals(0, summary.updated)
            assertEquals(1, summary.autoMatched)
            assertTrue(summary.unmatched.isEmpty())

            coVerify {
                externalFilmRefRepository.save(
                    match { it.title == "Fight Club" && it.year == 1999 && it.owned && !it.watched },
                )
            }
            coVerify { filmRepository.save(match { it.tmdbId == 550 }) }
            coVerify { externalFilmRefRepository.update(match { it.filmTmdbId == 550 }) }
        }

    @Test
    fun `importWatched should create a new watched (not owned) ref`() =
        runBlocking {
            coEvery { externalFilmRefRepository.findBySourceTitleYear("LETTERBOXD", "Fight Club", 1999) } returns null
            coEvery { externalFilmRefRepository.save(any()) } returns mockk()
            coEvery { externalFilmRefRepository.update(any()) } returns true
            coEvery { tmdbService.searchMovies("Fight Club", 1999) } returns movieSearchResult()
            coEvery { tmdbService.getMovieDetails(550) } returns movieDetails()
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } returns mockk()

            val summary = service.importWatched(watchedCsv)

            assertEquals(1, summary.created)
            coVerify {
                externalFilmRefRepository.save(
                    match { it.title == "Fight Club" && it.watched && !it.owned },
                )
            }
        }

    @Test
    fun `importCollection should leave a ref unmatched when TMDB search is ambiguous`() =
        runBlocking {
            val ambiguousResults =
                TmdbMovieSearchResponse(
                    page = 1,
                    results =
                        listOf(
                            TmdbMovie(id = 550, title = "Fight Club", releaseDate = "1999-10-15"),
                            TmdbMovie(id = 551, title = "Fight Club", releaseDate = "1999-01-01"),
                        ),
                    totalPages = 1,
                    totalResults = 2,
                )

            coEvery { externalFilmRefRepository.findBySourceTitleYear("LETTERBOXD", "Fight Club", 1999) } returns null
            coEvery { externalFilmRefRepository.save(any()) } returns mockk()
            coEvery { tmdbService.searchMovies("Fight Club", 1999) } returns ambiguousResults

            val summary = service.importCollection(collectionCsv)

            assertEquals(0, summary.autoMatched)
            assertEquals(listOf("Fight Club (1999)"), summary.unmatched)
            coVerify(exactly = 0) { filmRepository.save(any()) }
        }

    @Test
    fun `importCollection should break a same-title same-year tie by picking the higher vote count`() =
        runBlocking {
            val obscureDuplicateAndTheRealFilm =
                TmdbMovieSearchResponse(
                    page = 1,
                    results =
                        listOf(
                            TmdbMovie(id = 550, title = "Fight Club", releaseDate = "1999-10-15", voteCount = 28000),
                            TmdbMovie(id = 551, title = "Fight Club", releaseDate = "1999-01-01", voteCount = 3),
                        ),
                    totalPages = 1,
                    totalResults = 2,
                )

            coEvery { externalFilmRefRepository.findBySourceTitleYear("LETTERBOXD", "Fight Club", 1999) } returns null
            coEvery { externalFilmRefRepository.save(any()) } returns mockk()
            coEvery { externalFilmRefRepository.update(any()) } returns true
            coEvery { tmdbService.searchMovies("Fight Club", 1999) } returns obscureDuplicateAndTheRealFilm
            coEvery { tmdbService.getMovieDetails(550) } returns movieDetails()
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } returns mockk()

            val summary = service.importCollection(collectionCsv)

            assertEquals(1, summary.autoMatched)
            coVerify { filmRepository.save(match { it.tmdbId == 550 }) }
        }

    @Test
    fun `importCollection should ignore same-year results with a different title (e_g_ documentaries about the film)`() =
        runBlocking {
            val resultsWithADocumentary =
                TmdbMovieSearchResponse(
                    page = 1,
                    results =
                        listOf(
                            TmdbMovie(id = 550, title = "Fight Club", releaseDate = "1999-10-15"),
                            TmdbMovie(id = 999, title = "The Psychology of Fight Club", releaseDate = "1999-10-16"),
                        ),
                    totalPages = 1,
                    totalResults = 2,
                )

            coEvery { externalFilmRefRepository.findBySourceTitleYear("LETTERBOXD", "Fight Club", 1999) } returns null
            coEvery { externalFilmRefRepository.save(any()) } returns mockk()
            coEvery { externalFilmRefRepository.update(any()) } returns true
            coEvery { tmdbService.searchMovies("Fight Club", 1999) } returns resultsWithADocumentary
            coEvery { tmdbService.getMovieDetails(550) } returns movieDetails()
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } returns mockk()

            val summary = service.importCollection(collectionCsv)

            assertEquals(1, summary.autoMatched)
            assertTrue(summary.unmatched.isEmpty())
            coVerify { filmRepository.save(match { it.tmdbId == 550 }) }
        }

    @Test
    fun `importCollection should leave a ref unmatched when no candidate matches the year`() =
        runBlocking {
            coEvery { externalFilmRefRepository.findBySourceTitleYear("LETTERBOXD", "Fight Club", 1999) } returns null
            coEvery { externalFilmRefRepository.save(any()) } returns mockk()
            coEvery { tmdbService.searchMovies("Fight Club", 1999) } returns movieSearchResult(releaseDate = "2015-01-01")

            val summary = service.importCollection(collectionCsv)

            assertEquals(0, summary.autoMatched)
            assertEquals(listOf("Fight Club (1999)"), summary.unmatched)
        }

    @Test
    fun `importCollection should merge flags into an existing ref instead of creating a duplicate`() =
        runBlocking {
            val existing =
                ExternalFilmRef(
                    id = UUID.randomUUID(),
                    source = "LETTERBOXD",
                    title = "Fight Club",
                    year = 1999,
                    filmTmdbId = 550,
                    owned = false,
                    watched = true,
                )

            coEvery { externalFilmRefRepository.findBySourceTitleYear("LETTERBOXD", "Fight Club", 1999) } returns existing
            coEvery { externalFilmRefRepository.update(any()) } returns true

            val summary = service.importCollection(collectionCsv)

            assertEquals(0, summary.created)
            assertEquals(1, summary.updated)
            coVerify(exactly = 0) { externalFilmRefRepository.save(any()) }
            coVerify { externalFilmRefRepository.update(match { it.owned && it.watched && it.filmTmdbId == 550 }) }
            // Already matched, so no TMDB calls should happen for this row.
            coVerify(exactly = 0) { tmdbService.searchMovies(any()) }
        }

    @Test
    fun `importCollection should never un-hide an item the user removed, even when re-imported`() =
        runBlocking {
            val removedRef =
                ExternalFilmRef(
                    id = UUID.randomUUID(),
                    source = "LETTERBOXD",
                    title = "Fight Club",
                    year = 1999,
                    filmTmdbId = 550,
                    owned = false,
                    watched = true,
                    removed = true,
                )

            coEvery { externalFilmRefRepository.findBySourceTitleYear("LETTERBOXD", "Fight Club", 1999) } returns removedRef
            coEvery { externalFilmRefRepository.update(any()) } returns true

            service.importCollection(collectionCsv)

            coVerify { externalFilmRefRepository.update(match { it.removed }) }
            coVerify(exactly = 0) { externalFilmRefRepository.setRemoved(any(), any()) }
        }

    @Test
    fun `linkManually should resolve TMDB details and update the ref`() =
        runBlocking {
            val ref = ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Fight Club", year = 1999)

            coEvery { externalFilmRefRepository.findById(ref.id) } returns ref
            coEvery { tmdbService.getMovieDetails(550) } returns movieDetails()
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } returns mockk()
            coEvery { externalFilmRefRepository.update(any()) } returns true

            val result = service.linkManually(ref.id, 550)

            assertEquals(550, result?.filmTmdbId)
            coVerify { externalFilmRefRepository.update(match { it.filmTmdbId == 550 }) }
        }

    @Test
    fun `linkManually should link a TV mini-series with no director credits on TMDB and leave directorTmdbIds empty`() =
        runBlocking {
            val ref = ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Chernobyl", year = 2019)
            val tvDetails =
                TmdbTvDetails(
                    id = 87108,
                    name = "Chernobyl",
                    firstAirDate = "2019-05-06",
                    seasons = emptyList(),
                )

            coEvery { externalFilmRefRepository.findById(ref.id) } returns ref
            coEvery { tmdbService.getTvDetails(87108) } returns tvDetails
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } returns mockk()
            coEvery { externalFilmRefRepository.update(any()) } returns true

            val result = service.linkManually(ref.id, 87108, tv = true)

            assertEquals(87108, result?.filmTmdbId)
            coVerify { filmRepository.save(match { it.tv && it.title == "Chernobyl" && it.directorTmdbIds.isEmpty() }) }
            coVerify(exactly = 0) { personRepository.save(any()) }
            coVerify(exactly = 0) { tmdbService.getMovieDetails(any()) }
        }

    @Test
    fun `linkManually should resolve a TV mini-series director from aggregate_credits, ranked by episode count`() =
        runBlocking {
            val ref = ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Chernobyl", year = 2019)
            val tvDetails =
                TmdbTvDetails(
                    id = 87108,
                    name = "Chernobyl",
                    firstAirDate = "2019-05-06",
                    seasons = emptyList(),
                    aggregateCredits =
                        TmdbAggregateCredits(
                            crew =
                                listOf(
                                    TmdbAggregateCrewMember(
                                        id = 212408,
                                        name = "Johan Renck",
                                        jobs = listOf(TmdbAggregateCrewJob(job = "Director", episodeCount = 5)),
                                        totalEpisodeCount = 5,
                                    ),
                                    TmdbAggregateCrewMember(
                                        id = 35796,
                                        name = "Craig Mazin",
                                        jobs = listOf(TmdbAggregateCrewJob(job = "Writer", episodeCount = 5)),
                                        totalEpisodeCount = 5,
                                    ),
                                ),
                        ),
                )

            coEvery { externalFilmRefRepository.findById(ref.id) } returns ref
            coEvery { tmdbService.getTvDetails(87108) } returns tvDetails
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } returns mockk()
            coEvery { personRepository.findByTmdbId(212408) } returns null
            coEvery { personRepository.save(any()) } returns mockk()
            coEvery { externalFilmRefRepository.update(any()) } returns true

            val result = service.linkManually(ref.id, 87108, tv = true)

            assertEquals(87108, result?.filmTmdbId)
            // Only the actual director (5 episodes, job = Director) is resolved - the writer is not.
            coVerify { filmRepository.save(match { it.directorTmdbIds == listOf(212408) }) }
            coVerify {
                personRepository.save(match { it.tmdbId == 212408 && it.name == "Johan Renck" && it.department == Department.DIRECTING })
            }
            coVerify(exactly = 0) { personRepository.save(match { it.tmdbId == 35796 }) }
        }

    @Test
    fun `linkManually should return null when the ref does not exist`() =
        runBlocking {
            coEvery { externalFilmRefRepository.findById(any()) } returns null

            val result = service.linkManually(UUID.randomUUID(), 550)

            assertNull(result)
        }

    @Test
    fun `linkManually should update (not insert-ignore-skip) a film that was already saved before this match`() =
        runBlocking {
            val ref = ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Fight Club", year = 1999)
            val detailsWithDirector =
                movieDetails().copy(
                    credits = TmdbCredits(crew = listOf(TmdbCrewMember(id = 7, name = "David Fincher", job = "Director"))),
                )

            coEvery { externalFilmRefRepository.findById(ref.id) } returns ref
            coEvery { tmdbService.getMovieDetails(550) } returns detailsWithDirector
            coEvery { personRepository.findByTmdbId(7) } returns null
            coEvery { personRepository.save(any()) } returns mockk()
            // The film was already inserted by an earlier import, before director support existed.
            coEvery { filmRepository.findByTmdbId(550) } returns Film(tmdbId = 550, title = "Fight Club")
            coEvery { filmRepository.update(any()) } returns true
            coEvery { externalFilmRefRepository.update(any()) } returns true

            service.linkManually(ref.id, 550)

            coVerify(exactly = 0) { filmRepository.save(any()) }
            coVerify { filmRepository.update(match { it.tmdbId == 550 && it.directorTmdbIds == listOf(7) }) }
        }

    @Test
    fun `linkManually should register a new director Person and set directorTmdbId on the film`() =
        runBlocking {
            val ref = ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Fight Club", year = 1999)
            val detailsWithDirector =
                movieDetails().copy(
                    credits = TmdbCredits(crew = listOf(TmdbCrewMember(id = 7, name = "David Fincher", job = "Director"))),
                )

            coEvery { externalFilmRefRepository.findById(ref.id) } returns ref
            coEvery { tmdbService.getMovieDetails(550) } returns detailsWithDirector
            coEvery { personRepository.findByTmdbId(7) } returns null
            coEvery { personRepository.save(any()) } returns mockk()
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } returns mockk()
            coEvery { externalFilmRefRepository.update(any()) } returns true

            service.linkManually(ref.id, 550)

            coVerify {
                personRepository.save(match { it.tmdbId == 7 && it.name == "David Fincher" && it.department == Department.DIRECTING })
            }
            coVerify { filmRepository.save(match { it.directorTmdbIds == listOf(7) }) }
        }

    @Test
    fun `linkManually should not overwrite an already-known Person when resolving the director`() =
        runBlocking {
            val ref = ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Fight Club", year = 1999)
            val detailsWithDirector =
                movieDetails().copy(
                    credits = TmdbCredits(crew = listOf(TmdbCrewMember(id = 7, name = "David Fincher", job = "Director"))),
                )
            val existingPerson = Person(tmdbId = 7, name = "David Fincher", department = Department.ACTING)

            coEvery { externalFilmRefRepository.findById(ref.id) } returns ref
            coEvery { tmdbService.getMovieDetails(550) } returns detailsWithDirector
            coEvery { personRepository.findByTmdbId(7) } returns existingPerson
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } returns mockk()
            coEvery { externalFilmRefRepository.update(any()) } returns true

            service.linkManually(ref.id, 550)

            coVerify(exactly = 0) { personRepository.save(any()) }
            coVerify { filmRepository.save(match { it.directorTmdbIds == listOf(7) }) }
        }

    @Test
    fun `linkManually should retry once on a transient deadlock and still succeed`() =
        runBlocking {
            val ref = ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Fight Club", year = 1999)
            var saveAttempts = 0

            coEvery { externalFilmRefRepository.findById(ref.id) } returns ref
            coEvery { tmdbService.getMovieDetails(550) } returns movieDetails()
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } answers {
                saveAttempts++
                if (saveAttempts == 1) throw SQLTransactionRollbackException("Deadlock found trying to get lock")
                firstArg()
            }
            coEvery { externalFilmRefRepository.update(any()) } returns true

            val result = service.linkManually(ref.id, 550)

            assertEquals(550, result?.filmTmdbId)
            assertEquals(2, saveAttempts)
        }

    @Test
    fun `linkManually should not retry a non-deadlock failure`() =
        runBlocking {
            val ref = ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Fight Club", year = 1999)
            var saveAttempts = 0

            coEvery { externalFilmRefRepository.findById(ref.id) } returns ref
            coEvery { tmdbService.getMovieDetails(550) } returns movieDetails()
            coEvery { filmRepository.findByTmdbId(any()) } returns null
            coEvery { filmRepository.save(any()) } answers {
                saveAttempts++
                throw RuntimeException("not a deadlock")
            }

            val result = service.linkManually(ref.id, 550)

            assertNull(result)
            assertEquals(1, saveAttempts)
        }
}
