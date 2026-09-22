package me.jochum.filmqueuer.adapters.persistence

import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.domain.Film
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MySqlFilmRepositoryTest {
    private lateinit var repository: MySqlFilmRepository

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(PersonTable, FilmTable, FilmDirectorTable)
        }
        repository = MySqlFilmRepository()
    }

    @AfterEach
    fun cleanup() {
        transaction {
            FilmDirectorTable.deleteAll()
            FilmTable.deleteAll()
            PersonTable.deleteAll()
        }
    }

    @Test
    fun `save should store film successfully`() =
        runBlocking {
            // Given
            val film =
                Film(
                    tmdbId = 550,
                    title = "Fight Club",
                    originalTitle = "Fight Club",
                    releaseDate = LocalDate.of(1999, 10, 15),
                    runtime = 139,
                    genres = listOf("Drama", "Thriller"),
                    posterPath = "https://image.tmdb.org/t/p/w500/path.jpg",
                )

            // When
            val result = repository.save(film)

            // Then - save() fills in the default sort title ("Fight Club" has no leading
            // article, so it's unchanged) since none was given. This is a fresh insert, so the
            // stored id matches the id the Film was constructed with.
            val expected = film.copy(sortTitle = "Fight Club")
            assertEquals(expected, result)

            // Verify it was saved with detailed field validation
            val found = repository.findByTmdbId(550, false)
            assertNotNull(found)
            assertEquals(expected, found)

            // Additional detailed field assertions
            assertEquals(550, found.tmdbId)
            assertEquals("Fight Club", found.title)
            assertEquals("Fight Club", found.originalTitle)
            assertEquals(LocalDate.of(1999, 10, 15), found.releaseDate)
            assertEquals(139, found.runtime)
            assertEquals(listOf("Drama", "Thriller"), found.genres)
            assertEquals("https://image.tmdb.org/t/p/w500/path.jpg", found.posterPath)
            assertFalse(found.tv)
        }

    @Test
    fun `update should modify existing film`() =
        runBlocking {
            // Given
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

            // When
            val saved1 = repository.save(film1) // Insert original film
            // update() is keyed by id (not tmdbId), so the replacement film must carry the
            // existing row's id - as a caller would after looking it up via findByTmdbId.
            val film2 =
                Film(
                    id = saved1.id,
                    tmdbId = 550,
                    title = "Updated Title",
                    originalTitle = "Updated Original Title",
                    releaseDate = LocalDate.of(2000, 1, 1),
                    runtime = 120,
                    genres = listOf("Action", "Thriller"),
                    posterPath = "https://image.tmdb.org/t/p/w500/updated.jpg",
                )
            val updated = repository.update(film2) // Update existing film

            // Then
            assertTrue(updated) // Should return true indicating successful update
            val found = repository.findByTmdbId(550, false)
            assertNotNull(found)

            // Validate all updated fields
            assertEquals(film2.title, found.title) // Should have updated title
            assertEquals(film2.originalTitle, found.originalTitle) // Should have updated original title
            assertEquals(film2.releaseDate, found.releaseDate) // Should have updated release date
            assertEquals(film2.runtime, found.runtime) // Should have updated runtime
            assertEquals(film2.genres, found.genres) // Should have updated genres
            assertEquals(film2.posterPath, found.posterPath) // Should have updated poster path
            // sortTitle is preserved from film1's computed default, not recomputed from
            // film2's title, mirroring MySqlPersonRepository's update() semantics
            assertEquals("Fight Club", found.sortTitle)
        }

    @Test
    fun `update should return false when film does not exist`() =
        runBlocking {
            // Given - a freshly-constructed Film gets its own random id, which won't match any row
            val film = Film(tmdbId = 999, title = "Non-existent Film", releaseDate = LocalDate.of(2023, 1, 1))

            // When
            val updated = repository.update(film)

            // Then
            assertFalse(updated) // Should return false indicating no update occurred
            val found = repository.findByTmdbId(999, false)
            assertNull(found) // Film should still not exist
        }

    @Test
    fun `findByTmdbId should return film when exists`() =
        runBlocking {
            // Given
            val film = Film(tmdbId = 550, title = "Fight Club", releaseDate = LocalDate.of(1999, 10, 15))
            repository.save(film)

            // When
            val result = repository.findByTmdbId(550, false)

            // Then
            assertNotNull(result)
            assertEquals(film.copy(sortTitle = "Fight Club"), result)
        }

    @Test
    fun `findByTmdbId should return null when not exists`() =
        runBlocking {
            // When
            val result = repository.findByTmdbId(999, false)

            // Then
            assertNull(result)
        }

    @Test
    fun `findAll should return all films`() =
        runBlocking {
            // Given
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
            films.forEach { repository.save(it) }

            // When
            val result = repository.findAll()

            // Then - "The Godfather" gets its leading article stripped for the default sort title
            assertEquals(3, result.size)
            assertTrue(result.containsAll(films.map { it.copy(sortTitle = Film.defaultSortTitle(it.title)) }))
        }

    @Test
    fun `findAll should return empty list when no films`() =
        runBlocking {
            // When
            val result = repository.findAll()

            // Then
            assertTrue(result.isEmpty())
        }

    @Test
    fun `save should handle nullable fields correctly`() =
        runBlocking {
            // Given
            val film =
                Film(
                    tmdbId = 550,
                    title = "Fight Club",
                    originalTitle = null,
                    releaseDate = null,
                    runtime = null,
                    genres = null,
                    posterPath = null,
                )

            // When
            val result = repository.save(film)

            // Then
            assertEquals(film.copy(sortTitle = "Fight Club"), result)

            val found = repository.findByTmdbId(550, false)
            assertNotNull(found)
            assertNull(found.originalTitle)
            assertNull(found.releaseDate)
            assertNull(found.runtime)
            assertNull(found.genres)
            assertNull(found.posterPath)
        }

    @Test
    fun `updateSortTitle should overwrite the computed default and survive a later save`() =
        runBlocking {
            // Given
            val film = Film(tmdbId = 550, title = "The Godfather")
            val saved = repository.save(film) // default sortTitle becomes "Godfather"

            // When - updateSortTitle is keyed by the film's real id, not its tmdbId
            val updated = repository.updateSortTitle(saved.id, "Godfather, The")

            // Then
            assertTrue(updated)
            assertEquals("Godfather, The", repository.findByTmdbId(550, false)?.sortTitle)

            // And a later re-save (e.g. this film being added to another queue) must not
            // clobber the manual correction
            repository.save(film)
            assertEquals("Godfather, The", repository.findByTmdbId(550, false)?.sortTitle)
        }

    @Test
    fun `updateSortTitle should return false when film does not exist`() =
        runBlocking {
            // When
            val updated = repository.updateSortTitle(UUID.randomUUID(), "Nothing")

            // Then
            assertFalse(updated)
        }

    @Test
    fun `save should keep a movie and a TV show sharing the same tmdbId as two independent rows`() =
        runBlocking {
            // Given - TMDB movie and TV ids are separate namespaces and can collide on the same
            // number (e.g. movie 206647 "Spectre" and TV show 206647 "Histoire(s) du cinema").
            // Before the UUID-id fix, films were keyed on bare tmdbId, so saving the second one
            // would silently collide with (and could overwrite) the first.
            val movie = Film(tmdbId = 206647, title = "Spectre", tv = false)
            val tvShow = Film(tmdbId = 206647, title = "Histoire(s) du cinema", tv = true)

            // When
            val savedMovie = repository.save(movie)
            val savedTvShow = repository.save(tvShow)

            // Then - two distinct rows, each with correct title/tv flag, not a collision
            assertTrue(savedMovie.id != savedTvShow.id)
            assertEquals(2, repository.findAll().size)

            val foundMovie = repository.findByTmdbId(206647, false)
            val foundTvShow = repository.findByTmdbId(206647, true)

            assertNotNull(foundMovie)
            assertNotNull(foundTvShow)
            assertEquals("Spectre", foundMovie.title)
            assertFalse(foundMovie.tv)
            assertEquals("Histoire(s) du cinema", foundTvShow.title)
            assertTrue(foundTvShow.tv)
        }
}
