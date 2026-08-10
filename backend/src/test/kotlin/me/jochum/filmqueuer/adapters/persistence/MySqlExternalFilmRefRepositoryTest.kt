package me.jochum.filmqueuer.adapters.persistence

import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.domain.CollectionSortField
import me.jochum.filmqueuer.domain.Department
import me.jochum.filmqueuer.domain.ExternalFilmRef
import me.jochum.filmqueuer.domain.Film
import me.jochum.filmqueuer.domain.Person
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MySqlExternalFilmRefRepositoryTest {
    private lateinit var repository: MySqlExternalFilmRefRepository

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(PersonTable, FilmTable, ExternalFilmRefTable, FilmDirectorTable)
        }
        repository = MySqlExternalFilmRefRepository()
    }

    @AfterEach
    fun cleanup() {
        transaction {
            ExternalFilmRefTable.deleteAll()
            FilmDirectorTable.deleteAll()
            FilmTable.deleteAll()
            PersonTable.deleteAll()
        }
    }

    private fun testRef(
        title: String = "Fight Club",
        year: Int? = 1999,
        owned: Boolean = true,
        watched: Boolean = false,
        filmTmdbId: Int? = null,
        removed: Boolean = false,
    ) = ExternalFilmRef(
        id = UUID.randomUUID(),
        source = "LETTERBOXD",
        title = title,
        year = year,
        filmTmdbId = filmTmdbId,
        owned = owned,
        watched = watched,
        createdAt = Instant.now(),
        removed = removed,
    )

    @Test
    fun `save should store and retrieve a ref by id`() =
        runBlocking {
            val ref = testRef()
            repository.save(ref)

            val found = repository.findById(ref.id)

            assertEquals(ref, found)
        }

    @Test
    fun `findBySourceTitleYear should find an exact match`() =
        runBlocking {
            val ref = testRef()
            repository.save(ref)

            val found = repository.findBySourceTitleYear("LETTERBOXD", "Fight Club", 1999)

            assertEquals(ref, found)
        }

    @Test
    fun `findBySourceTitleYear should match on null year`() =
        runBlocking {
            val ref = testRef(title = "Untitled Short", year = null)
            repository.save(ref)

            val found = repository.findBySourceTitleYear("LETTERBOXD", "Untitled Short", null)

            assertEquals(ref, found)
        }

    @Test
    fun `findBySourceTitleYear should return null when nothing matches`() =
        runBlocking {
            assertNull(repository.findBySourceTitleYear("LETTERBOXD", "Nonexistent", 2000))
        }

    @Test
    fun `update should change owned, watched and filmTmdbId`() =
        runBlocking {
            val ref = testRef(owned = true, watched = false)
            repository.save(ref)
            MySqlFilmRepository().save(Film(tmdbId = 550, title = "Fight Club"))

            val updated = ref.copy(owned = true, watched = true, filmTmdbId = 550)
            val result = repository.update(updated)

            assertTrue(result)
            assertEquals(updated, repository.findById(ref.id))
        }

    @Test
    fun `findPage should return every saved ref when unfiltered`() =
        runBlocking {
            repository.save(testRef(title = "Fight Club"))
            repository.save(testRef(title = "Se7en", year = 1995))

            val page =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )

            assertEquals(2, page.size)
        }

    @Test
    fun `count should match the total number of refs matching the filter`() =
        runBlocking {
            repository.save(testRef(title = "Fight Club", owned = true, watched = false))
            repository.save(testRef(title = "Se7en", year = 1995, owned = false, watched = true))

            assertEquals(2, repository.count(owned = null, watched = null, unmatched = null))
            assertEquals(1, repository.count(owned = true, watched = null, unmatched = null))
            assertEquals(1, repository.count(owned = null, watched = true, unmatched = null))
        }

    @Test
    fun `findPage should respect offset and limit`() =
        runBlocking {
            repository.save(testRef(title = "A"))
            repository.save(testRef(title = "B"))
            repository.save(testRef(title = "C"))

            val page =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = false,
                    offset = 1,
                    limit = 1,
                )

            assertEquals(1, page.size)
        }

    @Test
    fun `findPage should sort by title ascending and descending`() =
        runBlocking {
            repository.save(testRef(title = "Se7en", year = 1995))
            repository.save(testRef(title = "Fight Club"))

            val ascending =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )
            assertEquals(listOf("Fight Club", "Se7en"), ascending.map { it.title })

            val descending =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = true,
                    offset = 0,
                    limit = 40,
                )
            assertEquals(listOf("Se7en", "Fight Club"), descending.map { it.title })
        }

    @Test
    fun `findPage should sort matched films by their sort title, stripping a leading article`() =
        runBlocking {
            // By raw title, "Se7en" < "The Godfather" (S < T). By sort title, "Godfather" (the
            // leading "The" is stripped) < "Se7en" (G < S) - this only distinguishes the two.
            val filmRepository = MySqlFilmRepository()
            filmRepository.save(Film(tmdbId = 238, title = "The Godfather"))
            filmRepository.save(Film(tmdbId = 550, title = "Se7en"))

            repository.save(testRef(title = "The Godfather", filmTmdbId = 238))
            repository.save(testRef(title = "Se7en", year = 1995, filmTmdbId = 550))

            val page =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )

            assertEquals(listOf("The Godfather", "Se7en"), page.map { it.title })
        }

    @Test
    fun `findPage should sort by year`() =
        runBlocking {
            repository.save(testRef(title = "Newer", year = 2010))
            repository.save(testRef(title = "Older", year = 1950))

            val page =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.YEAR,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )

            assertEquals(listOf("Older", "Newer"), page.map { it.title })
        }

    @Test
    fun `findPage should sort by director family name via the joined Film and Person tables`() =
        runBlocking {
            val filmRepository = MySqlFilmRepository()
            val personRepository = MySqlPersonRepository()
            personRepository.save(Person(tmdbId = 1, name = "Zack Snyder", department = Department.DIRECTING))
            personRepository.save(Person(tmdbId = 2, name = "Ang Lee", department = Department.DIRECTING))
            filmRepository.save(Film(tmdbId = 550, title = "Snyder Film", directorTmdbIds = listOf(1)))
            filmRepository.save(Film(tmdbId = 551, title = "Lee Film", directorTmdbIds = listOf(2)))

            repository.save(testRef(title = "Snyder Film", filmTmdbId = 550))
            repository.save(testRef(title = "Lee Film", year = 2001, filmTmdbId = 551))

            val page =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.DIRECTOR,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )

            assertEquals(listOf("Lee Film", "Snyder Film"), page.map { it.title })
        }

    @Test
    fun `findPage should sort by family name, not full name, when the two orders differ`() =
        runBlocking {
            // "Alfred Hitchcock" < "Woody Allen" by full name, but "Allen" < "Hitchcock" by
            // family name - this only distinguishes the two sort behaviors.
            val filmRepository = MySqlFilmRepository()
            val personRepository = MySqlPersonRepository()
            personRepository.save(Person(tmdbId = 1, name = "Alfred Hitchcock", department = Department.DIRECTING))
            personRepository.save(Person(tmdbId = 2, name = "Woody Allen", department = Department.DIRECTING))
            filmRepository.save(Film(tmdbId = 550, title = "Hitchcock Film", directorTmdbIds = listOf(1)))
            filmRepository.save(Film(tmdbId = 551, title = "Allen Film", directorTmdbIds = listOf(2)))

            repository.save(testRef(title = "Hitchcock Film", filmTmdbId = 550))
            repository.save(testRef(title = "Allen Film", year = 2001, filmTmdbId = 551))

            val page =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.DIRECTOR,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )

            assertEquals(listOf("Allen Film", "Hitchcock Film"), page.map { it.title })
        }

    @Test
    fun `findPage sorting by director should break ties with year, then title`() =
        runBlocking {
            val filmRepository = MySqlFilmRepository()
            val personRepository = MySqlPersonRepository()
            personRepository.save(Person(tmdbId = 1, name = "Steven Spielberg", department = Department.DIRECTING))

            // Same director throughout: "Zzz" sorts after "Aaa" by title but comes first because
            // its year (1993) is earlier - proving year is checked before title within a director.
            filmRepository.save(Film(tmdbId = 1, title = "Zzz Later Title", directorTmdbIds = listOf(1)))
            filmRepository.save(Film(tmdbId = 2, title = "Aaa Earlier Title", directorTmdbIds = listOf(1)))
            // Same director and same year: title breaks the tie.
            filmRepository.save(Film(tmdbId = 3, title = "Beta", directorTmdbIds = listOf(1)))
            filmRepository.save(Film(tmdbId = 4, title = "Alpha", directorTmdbIds = listOf(1)))

            repository.save(testRef(title = "Zzz Later Title", year = 1993, filmTmdbId = 1))
            repository.save(testRef(title = "Aaa Earlier Title", year = 1975, filmTmdbId = 2))
            repository.save(testRef(title = "Beta", year = 2000, filmTmdbId = 3))
            repository.save(testRef(title = "Alpha", year = 2000, filmTmdbId = 4))

            val page =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.DIRECTOR,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )

            assertEquals(
                listOf("Aaa Earlier Title", "Zzz Later Title", "Alpha", "Beta"),
                page.map { it.title },
            )
        }

    @Test
    fun `findPage should filter by owned, watched and unmatched`() =
        runBlocking {
            val filmRepository = MySqlFilmRepository()
            filmRepository.save(Film(tmdbId = 550, title = "Fight Club"))

            repository.save(testRef(title = "Owned Matched", owned = true, watched = false, filmTmdbId = 550))
            repository.save(testRef(title = "Watched Unmatched", year = 2001, owned = false, watched = true))

            val ownedOnly =
                repository.findPage(
                    owned = true,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )
            assertEquals(listOf("Owned Matched"), ownedOnly.map { it.title })

            val unmatchedOnly =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = true,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )
            assertEquals(listOf("Watched Unmatched"), unmatchedOnly.map { it.title })

            val matchedOnly =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = false,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )
            assertEquals(listOf("Owned Matched"), matchedOnly.map { it.title })
        }

    @Test
    fun `findPage and count should exclude removed items by default and include them when asked`() =
        runBlocking {
            repository.save(testRef(title = "Visible Film"))
            repository.save(testRef(title = "Hidden Film", year = 2001, removed = true))

            val defaultPage =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                )
            assertEquals(listOf("Visible Film"), defaultPage.map { it.title })
            assertEquals(1, repository.count(null, null, null))

            val removedPage =
                repository.findPage(
                    owned = null,
                    watched = null,
                    unmatched = null,
                    sortField = CollectionSortField.TITLE,
                    sortDescending = false,
                    offset = 0,
                    limit = 40,
                    removed = true,
                )
            assertEquals(listOf("Hidden Film"), removedPage.map { it.title })
            assertEquals(1, repository.count(null, null, null, removed = true))
        }

    @Test
    fun `setRemoved should hide an item and restore it, without a re-import resurrecting it`() =
        runBlocking {
            val ref = testRef(title = "Fight Club")
            repository.save(ref)

            assertTrue(repository.setRemoved(ref.id, true))
            assertEquals(true, repository.findById(ref.id)?.removed)

            // A re-import merging owned/watched flags via update() must not un-hide the item.
            repository.update(ref.copy(owned = true, watched = true))
            assertEquals(true, repository.findById(ref.id)?.removed)

            assertTrue(repository.setRemoved(ref.id, false))
            assertEquals(false, repository.findById(ref.id)?.removed)
        }

    @Test
    fun `setRemoved should return false when the ref does not exist`() =
        runBlocking {
            assertFalse(repository.setRemoved(UUID.randomUUID(), true))
        }

    @Test
    fun `findByFilmTmdbId should return the linked ref`() =
        runBlocking {
            val filmRepository = MySqlFilmRepository()
            filmRepository.save(Film(tmdbId = 550, title = "Fight Club"))
            val ref = testRef(filmTmdbId = 550)
            repository.save(ref)

            val found = repository.findByFilmTmdbId(550)

            assertEquals(ref, found)
        }

    @Test
    fun `findRandomPicks should only return owned, unwatched, matched refs under the runtime cap`() =
        runBlocking {
            val filmRepository = MySqlFilmRepository()
            filmRepository.save(Film(tmdbId = 1, title = "Short Owned Unwatched", runtime = 90))
            filmRepository.save(Film(tmdbId = 2, title = "Too Long", runtime = 150))
            filmRepository.save(Film(tmdbId = 3, title = "No Runtime Data", runtime = null))

            repository.save(testRef(title = "Short Owned Unwatched", filmTmdbId = 1, owned = true, watched = false))
            repository.save(testRef(title = "Too Long", year = 2000, filmTmdbId = 2, owned = true, watched = false))
            repository.save(testRef(title = "No Runtime Data", year = 2001, filmTmdbId = 3, owned = true, watched = false))
            repository.save(testRef(title = "Owned But Watched", year = 2002, owned = true, watched = true))
            repository.save(testRef(title = "Not Owned", year = 2003, owned = false, watched = false))
            repository.save(testRef(title = "Unmatched", year = 2004, owned = true, watched = false, filmTmdbId = null))
            val removedRef = testRef(title = "Removed", year = 2005, owned = true, watched = false)
            repository.save(removedRef)
            repository.setRemoved(removedRef.id, true)

            val picks = repository.findRandomPicks(owned = true, watched = false, maxRuntime = 100, count = 10)

            assertEquals(listOf("Short Owned Unwatched"), picks.map { it.title })
        }

    @Test
    fun `findRandomPicks should cap the result at count even when more are eligible`() =
        runBlocking {
            val filmRepository = MySqlFilmRepository()
            repeat(5) { i ->
                filmRepository.save(Film(tmdbId = 100 + i, title = "Film $i", runtime = 90))
                repository.save(testRef(title = "Film $i", year = 2000 + i, filmTmdbId = 100 + i))
            }

            val picks = repository.findRandomPicks(owned = true, watched = false, maxRuntime = 100, count = 2)

            assertEquals(2, picks.size)
        }

    @Test
    fun `findRandomPicks should ignore the runtime cap when null`() =
        runBlocking {
            val filmRepository = MySqlFilmRepository()
            filmRepository.save(Film(tmdbId = 1, title = "Long Film", runtime = 200))
            repository.save(testRef(title = "Long Film", filmTmdbId = 1))

            val picks = repository.findRandomPicks(owned = true, watched = false, maxRuntime = null, count = 10)

            assertEquals(listOf("Long Film"), picks.map { it.title })
        }
}
