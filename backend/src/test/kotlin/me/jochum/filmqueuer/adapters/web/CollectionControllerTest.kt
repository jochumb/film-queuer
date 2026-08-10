package me.jochum.filmqueuer.adapters.web

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
import me.jochum.filmqueuer.domain.CollectionSortField
import me.jochum.filmqueuer.domain.ExternalFilmRef
import me.jochum.filmqueuer.domain.ExternalFilmRefRepository
import me.jochum.filmqueuer.domain.Film
import me.jochum.filmqueuer.domain.FilmRepository
import me.jochum.filmqueuer.domain.ImportSummary
import me.jochum.filmqueuer.domain.LetterboxdImportService
import me.jochum.filmqueuer.domain.PersonRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectionControllerTest {
    private lateinit var letterboxdImportService: LetterboxdImportService
    private lateinit var externalFilmRefRepository: ExternalFilmRefRepository
    private lateinit var filmRepository: FilmRepository
    private lateinit var personRepository: PersonRepository

    @BeforeEach
    fun setup() {
        letterboxdImportService = mockk()
        externalFilmRefRepository = mockk()
        filmRepository = mockk()
        personRepository = mockk()
    }

    @Test
    fun `POST import letterboxd owned should return the import summary`() =
        testApplication {
            val summary = ImportSummary(totalRows = 5, created = 3, updated = 2, autoMatched = 4, unmatched = listOf("Some Film (2020)"))
            coEvery { letterboxdImportService.importCollection(any()) } returns summary

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response =
                client.post("/collection/import/letterboxd/owned") {
                    setBody("Position,Name,Year,URL,Description\n1,Fight Club,1999,https://boxd.it/a,")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("\"totalRows\":5"))
            assertTrue(body.contains("Some Film (2020)"))
            coVerify { letterboxdImportService.importCollection(any()) }
        }

    @Test
    fun `POST import letterboxd owned should return 400 for unparseable CSV`() =
        testApplication {
            coEvery { letterboxdImportService.importCollection(any()) } throws IllegalArgumentException("missing film table header")

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response = client.post("/collection/import/letterboxd/owned") { setBody("not a csv") }

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(response.bodyAsText().contains("Invalid CSV"))
        }

    @Test
    fun `POST import letterboxd owned should return 500 on unexpected errors`() =
        testApplication {
            coEvery { letterboxdImportService.importCollection(any()) } throws RuntimeException("TMDB is down")

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response = client.post("/collection/import/letterboxd/owned") { setBody("Position,Name,Year,URL,Description") }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertTrue(response.bodyAsText().contains("Failed to import collection"))
        }

    @Test
    fun `POST import letterboxd watched should return the import summary`() =
        testApplication {
            val summary = ImportSummary(totalRows = 1, created = 1, updated = 0, autoMatched = 1, unmatched = emptyList())
            coEvery { letterboxdImportService.importWatched(any()) } returns summary

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response =
                client.post("/collection/import/letterboxd/watched") {
                    setBody("Date,Name,Year,Letterboxd URI\n2020-01-01,Fight Club,1999,https://boxd.it/a")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { letterboxdImportService.importWatched(any()) }
        }

    @Test
    fun `GET collection should return a paginated page enriched with film details`() =
        testApplication {
            val ref =
                ExternalFilmRef(
                    id = UUID.randomUUID(),
                    source = "LETTERBOXD",
                    title = "Fight Club",
                    year = 1999,
                    filmTmdbId = 550,
                    owned = true,
                )
            coEvery { externalFilmRefRepository.findPage(null, null, null, CollectionSortField.TITLE, false, 0, 40) } returns listOf(ref)
            coEvery { externalFilmRefRepository.count(null, null, null) } returns 1
            coEvery { filmRepository.findByTmdbId(550) } returns Film(tmdbId = 550, title = "Fight Club", posterPath = "/poster.jpg")

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response = client.get("/collection")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("\"total\":1"))
            assertTrue(body.contains("\"offset\":0"))
            assertTrue(body.contains("\"limit\":40"))
            assertTrue(body.contains("/poster.jpg"))
        }

    @Test
    fun `GET collection should forward removed=true to view hidden items`() =
        testApplication {
            val ref =
                ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Hidden Film", year = 1999, removed = true)
            coEvery {
                externalFilmRefRepository.findPage(null, null, null, CollectionSortField.TITLE, false, 0, 40, true)
            } returns listOf(ref)
            coEvery { externalFilmRefRepository.count(null, null, null, true) } returns 1

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response = client.get("/collection?removed=true")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Hidden Film"))
        }

    @Test
    fun `GET collection should respect limit and offset`() =
        testApplication {
            val refs =
                (1..5).map {
                    ExternalFilmRef(
                        id = UUID.randomUUID(),
                        source = "LETTERBOXD",
                        title = "Film $it",
                        year = 2000,
                        owned = true,
                    )
                }
            coEvery {
                externalFilmRefRepository.findPage(null, null, null, CollectionSortField.TITLE, false, 2, 2)
            } returns refs.subList(2, 4)
            coEvery { externalFilmRefRepository.count(null, null, null) } returns refs.size

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response = client.get("/collection?offset=2&limit=2")

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("\"total\":5"))
            assertTrue(body.contains("\"offset\":2"))
            assertTrue(body.contains("\"limit\":2"))
            assertTrue(body.contains("Film 3"))
            assertTrue(body.contains("Film 4"))
            assertTrue(!body.contains("Film 5\""))
        }

    @Test
    fun `GET collection should filter by owned watched and unmatched query params`() =
        testApplication {
            val ownedMatched =
                ExternalFilmRef(
                    id = UUID.randomUUID(),
                    source = "LETTERBOXD",
                    title = "Owned Matched",
                    year = 2000,
                    owned = true,
                    filmTmdbId = 1,
                )
            val watchedUnmatched =
                ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Watched Unmatched", year = 2001, watched = true)
            coEvery {
                externalFilmRefRepository.findPage(true, null, null, CollectionSortField.TITLE, false, 0, 40)
            } returns listOf(ownedMatched)
            coEvery { externalFilmRefRepository.count(true, null, null) } returns 1
            coEvery {
                externalFilmRefRepository.findPage(null, null, true, CollectionSortField.TITLE, false, 0, 40)
            } returns listOf(watchedUnmatched)
            coEvery { externalFilmRefRepository.count(null, null, true) } returns 1
            coEvery { filmRepository.findByTmdbId(1) } returns Film(tmdbId = 1, title = "Owned Matched")

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val ownedResponse = client.get("/collection?owned=true")
            assertTrue(ownedResponse.bodyAsText().contains("Owned Matched"))
            assertTrue(!ownedResponse.bodyAsText().contains("Watched Unmatched"))

            val unmatchedResponse = client.get("/collection?unmatched=true")
            assertTrue(unmatchedResponse.bodyAsText().contains("Watched Unmatched"))
            assertTrue(!unmatchedResponse.bodyAsText().contains("Owned Matched"))
        }

    @Test
    fun `PUT collection id link should update and return the ref`() =
        testApplication {
            val id = UUID.randomUUID()
            val updated = ExternalFilmRef(id = id, source = "LETTERBOXD", title = "Fight Club", year = 1999, filmTmdbId = 550, owned = true)
            coEvery { letterboxdImportService.linkManually(id, 550) } returns updated
            coEvery { filmRepository.findByTmdbId(550) } returns Film(tmdbId = 550, title = "Fight Club")

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response =
                client.put("/collection/$id/link") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"tmdbId": 550}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"filmTmdbId\":550"))
        }

    @Test
    fun `PUT collection id link should forward tv=true so mini-series link correctly`() =
        testApplication {
            val id = UUID.randomUUID()
            val updated =
                ExternalFilmRef(id = id, source = "LETTERBOXD", title = "Chernobyl", year = 2019, filmTmdbId = 87108, owned = true)
            coEvery { letterboxdImportService.linkManually(id, 87108, true) } returns updated
            coEvery { filmRepository.findByTmdbId(87108) } returns Film(tmdbId = 87108, title = "Chernobyl", tv = true)

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response =
                client.put("/collection/$id/link") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"tmdbId": 87108, "tv": true}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("\"filmTmdbId\":87108"))
            coVerify { letterboxdImportService.linkManually(id, 87108, true) }
        }

    @Test
    fun `PUT collection id link should return 404 when the ref does not exist`() =
        testApplication {
            val id = UUID.randomUUID()
            coEvery { letterboxdImportService.linkManually(id, 550) } returns null

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response =
                client.put("/collection/$id/link") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"tmdbId": 550}""")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `PUT collection id link should return 400 for an invalid id`() =
        testApplication {
            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response =
                client.put("/collection/not-a-uuid/link") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"tmdbId": 550}""")
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `PUT collection id removed should hide the item`() =
        testApplication {
            val id = UUID.randomUUID()
            coEvery { externalFilmRefRepository.setRemoved(id, true) } returns true

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response =
                client.put("/collection/$id/removed") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"removed": true}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { externalFilmRefRepository.setRemoved(id, true) }
        }

    @Test
    fun `PUT collection id removed should restore a previously hidden item`() =
        testApplication {
            val id = UUID.randomUUID()
            coEvery { externalFilmRefRepository.setRemoved(id, false) } returns true

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response =
                client.put("/collection/$id/removed") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"removed": false}""")
                }

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { externalFilmRefRepository.setRemoved(id, false) }
        }

    @Test
    fun `PUT collection id removed should return 404 when the ref does not exist`() =
        testApplication {
            val id = UUID.randomUUID()
            coEvery { externalFilmRefRepository.setRemoved(id, true) } returns false

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response =
                client.put("/collection/$id/removed") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"removed": true}""")
                }

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `GET collection random-picks should default to owned, unwatched, 100-minute cap, count 3`() =
        testApplication {
            val ref = ExternalFilmRef(id = UUID.randomUUID(), source = "LETTERBOXD", title = "Short Film", year = 1999, filmTmdbId = 550)
            coEvery { externalFilmRefRepository.findRandomPicks(true, false, 100, 3) } returns listOf(ref)
            coEvery { filmRepository.findByTmdbId(550) } returns Film(tmdbId = 550, title = "Short Film", runtime = 90)

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response = client.get("/collection/random-picks")

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Short Film"))
            coVerify { externalFilmRefRepository.findRandomPicks(true, false, 100, 3) }
        }

    @Test
    fun `GET collection random-picks should forward custom filters`() =
        testApplication {
            coEvery { externalFilmRefRepository.findRandomPicks(false, true, 60, 5) } returns emptyList()

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response = client.get("/collection/random-picks?owned=false&watched=true&maxRuntime=60&count=5")

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { externalFilmRefRepository.findRandomPicks(false, true, 60, 5) }
        }

    @Test
    fun `GET collection random-picks should clamp count to a sane range`() =
        testApplication {
            coEvery { externalFilmRefRepository.findRandomPicks(true, false, 100, 20) } returns emptyList()

            application {
                configureSerialization()
                routing { configureCollectionRoutes(letterboxdImportService, externalFilmRefRepository, filmRepository, personRepository) }
            }

            val response = client.get("/collection/random-picks?count=500")

            assertEquals(HttpStatusCode.OK, response.status)
            coVerify { externalFilmRefRepository.findRandomPicks(true, false, 100, 20) }
        }
}
