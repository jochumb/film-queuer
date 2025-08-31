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
            SchemaUtils.create(FilmTable)
        }
        repository = MySqlFilmRepository()
    }

    @AfterEach
    fun cleanup() {
        transaction {
            FilmTable.deleteAll()
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

            // Then
            assertEquals(film, result)

            // Verify it was saved with detailed field validation
            val found = repository.findByTmdbId(550)
            assertNotNull(found)
            assertEquals(film, found)

            // Additional detailed field assertions
            assertEquals(550, found.tmdbId)
            assertEquals("Fight Club", found.title)
            assertEquals("Fight Club", found.originalTitle)
            assertEquals(LocalDate.of(1999, 10, 15), found.releaseDate)
            assertEquals(139, found.runtime)
            assertEquals(listOf("Drama", "Thriller"), found.genres)
            assertEquals("https://image.tmdb.org/t/p/w500/path.jpg", found.posterPath)
        }

    @Test
    fun `update should modify existing film`() =
        runBlocking {
            // Given
            val film1 =
                Film(
                    550,
                    "Fight Club",
                    "Fight Club",
                    LocalDate.of(1999, 10, 15),
                    139,
                    listOf("Drama", "Thriller"),
                    "https://image.tmdb.org/t/p/w500/pB8BM7pdSp6B6Ih7QZ4DrQ3PmJK.jpg",
                )
            val film2 =
                Film(
                    550,
                    "Updated Title",
                    "Updated Original Title",
                    LocalDate.of(2000, 1, 1),
                    120,
                    listOf("Action", "Thriller"),
                    "https://image.tmdb.org/t/p/w500/updated.jpg",
                )

            // When
            repository.save(film1) // Insert original film
            val updated = repository.update(film2) // Update existing film

            // Then
            assertTrue(updated) // Should return true indicating successful update
            val found = repository.findByTmdbId(550)
            assertNotNull(found)

            // Validate all updated fields
            assertEquals(film2.title, found.title) // Should have updated title
            assertEquals(film2.originalTitle, found.originalTitle) // Should have updated original title
            assertEquals(film2.releaseDate, found.releaseDate) // Should have updated release date
            assertEquals(film2.runtime, found.runtime) // Should have updated runtime
            assertEquals(film2.genres, found.genres) // Should have updated genres
            assertEquals(film2.posterPath, found.posterPath) // Should have updated poster path
        }

    @Test
    fun `update should return false when film does not exist`() =
        runBlocking {
            // Given
            val film = Film(999, "Non-existent Film", null, LocalDate.of(2023, 1, 1), null, null, null)

            // When
            val updated = repository.update(film)

            // Then
            assertFalse(updated) // Should return false indicating no update occurred
            val found = repository.findByTmdbId(999)
            assertNull(found) // Film should still not exist
        }

    @Test
    fun `findByTmdbId should return film when exists`() =
        runBlocking {
            // Given
            val film = Film(550, "Fight Club", null, LocalDate.of(1999, 10, 15), null, null, null)
            repository.save(film)

            // When
            val result = repository.findByTmdbId(550)

            // Then
            assertNotNull(result)
            assertEquals(film, result)
        }

    @Test
    fun `findByTmdbId should return null when not exists`() =
        runBlocking {
            // When
            val result = repository.findByTmdbId(999)

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
                    Film(
                        238,
                        "The Godfather",
                        "The Godfather",
                        LocalDate.of(1972, 3, 14),
                        175,
                        listOf("Crime", "Drama"),
                        "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg",
                    ),
                )
            films.forEach { repository.save(it) }

            // When
            val result = repository.findAll()

            // Then
            assertEquals(3, result.size)
            assertTrue(result.containsAll(films))
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
            assertEquals(film, result)

            val found = repository.findByTmdbId(550)
            assertNotNull(found)
            assertNull(found.originalTitle)
            assertNull(found.releaseDate)
            assertNull(found.runtime)
            assertNull(found.genres)
            assertNull(found.posterPath)
        }
}
