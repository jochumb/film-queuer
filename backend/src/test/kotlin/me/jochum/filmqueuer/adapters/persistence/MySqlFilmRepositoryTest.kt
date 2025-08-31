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
                )

            // When
            val result = repository.save(film)

            // Then
            assertEquals(film, result)

            // Verify it was saved
            val found = repository.findByTmdbId(550)
            assertNotNull(found)
            assertEquals(film, found)
        }

    @Test
    fun `save should handle duplicate tmdbId with replace`() =
        runBlocking {
            // Given
            val film1 = Film(550, "Fight Club", "Fight Club", LocalDate.of(1999, 10, 15))
            val film2 = Film(550, "Updated Title", "Updated Original", LocalDate.of(2000, 1, 1))

            // When
            repository.save(film1)
            repository.save(film2) // Should replace the existing record

            // Then
            val found = repository.findByTmdbId(550)
            assertNotNull(found)
            assertEquals(film2.title, found.title) // Should have updated title
            assertEquals(film2.originalTitle, found.originalTitle) // Should have updated original title
            assertEquals(film2.releaseDate, found.releaseDate) // Should have updated release date
        }

    @Test
    fun `findByTmdbId should return film when exists`() =
        runBlocking {
            // Given
            val film = Film(550, "Fight Club", null, LocalDate.of(1999, 10, 15))
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
                    Film(550, "Fight Club", "Fight Club", LocalDate.of(1999, 10, 15)),
                    Film(13, "Forrest Gump", null, LocalDate.of(1994, 7, 6)),
                    Film(238, "The Godfather", null, LocalDate.of(1972, 3, 14)),
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
                )

            // When
            val result = repository.save(film)

            // Then
            assertEquals(film, result)

            val found = repository.findByTmdbId(550)
            assertNotNull(found)
            assertNull(found.originalTitle)
            assertNull(found.releaseDate)
        }
}
