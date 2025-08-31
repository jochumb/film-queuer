package me.jochum.filmqueuer.adapters.persistence

import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.domain.Department
import me.jochum.filmqueuer.domain.Person
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MySqlPersonRepositoryTest {
    private lateinit var repository: MySqlPersonRepository
    private lateinit var database: Database

    @BeforeEach
    fun setup() {
        // Use H2 in-memory database for testing
        database =
            Database.connect(
                "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=MySQL",
                driver = "org.h2.Driver",
            )

        transaction {
            SchemaUtils.create(PersonTable)
        }

        repository = MySqlPersonRepository()
    }

    @AfterEach
    fun cleanup() {
        transaction {
            SchemaUtils.drop(PersonTable)
        }
    }

    @Test
    fun `save should store person in database`() =
        runBlocking {
            // Given
            val person =
                Person(
                    tmdbId = 123,
                    name = "John Doe",
                    department = Department.ACTING,
                    imagePath = null,
                )

            // When
            val savedPerson = repository.save(person)

            // Then
            assertEquals(person, savedPerson)

            val foundPerson = repository.findByTmdbId(123)
            assertEquals(person, foundPerson)
        }

    @Test
    fun `save should replace existing person with same tmdbId`() =
        runBlocking {
            // Given
            val person1 =
                Person(
                    tmdbId = 123,
                    name = "John Doe",
                    department = Department.ACTING,
                    imagePath = null,
                )
            val person2 =
                Person(
                    tmdbId = 123,
                    name = "Jane Smith",
                    department = Department.DIRECTING,
                    imagePath = "https://image.tmdb.org/t/p/w200/profile.jpg",
                )

            // When
            repository.save(person1)
            repository.save(person2) // Should replace the first person

            // Then
            val foundPerson = repository.findByTmdbId(123)
            assertEquals(person2, foundPerson) // Should now be the second person
        }

    @Test
    fun `findByTmdbId should return null when person not found`() =
        runBlocking {
            // When
            val result = repository.findByTmdbId(999)

            // Then
            assertNull(result)
        }

    @Test
    fun `findAll should return all saved persons`() =
        runBlocking {
            // Given
            val person1 = Person(123, "John Doe", Department.ACTING, null)
            val person2 = Person(456, "Jane Smith", Department.DIRECTING, null)
            val person3 = Person(789, "Bob Writer", Department.WRITING, null)

            // When
            repository.save(person1)
            repository.save(person2)
            repository.save(person3)

            val allPersons = repository.findAll()

            // Then
            assertEquals(3, allPersons.size)
            assertTrue(allPersons.contains(person1))
            assertTrue(allPersons.contains(person2))
            assertTrue(allPersons.contains(person3))
        }

    @Test
    fun `findAll should return empty list when no persons exist`() =
        runBlocking {
            // When
            val allPersons = repository.findAll()

            // Then
            assertTrue(allPersons.isEmpty())
        }

    @Test
    fun `deleteByTmdbId should remove person and return true`() =
        runBlocking {
            // Given
            val person = Person(123, "John Doe", Department.ACTING, null)
            repository.save(person)

            // When
            val deleted = repository.deleteByTmdbId(123)

            // Then
            assertTrue(deleted)
            assertNull(repository.findByTmdbId(123))
        }

    @Test
    fun `deleteByTmdbId should return false when person not found`() =
        runBlocking {
            // When
            val deleted = repository.deleteByTmdbId(999)

            // Then
            assertTrue(!deleted)
        }

    @Test
    fun `save should update person with imagePath for enrichment`() =
        runBlocking {
            // Given
            val personWithoutImage =
                Person(
                    tmdbId = 123,
                    name = "John Doe",
                    department = Department.ACTING,
                    imagePath = null,
                )
            val personWithImage =
                Person(
                    tmdbId = 123,
                    name = "John Doe",
                    department = Department.ACTING,
                    imagePath = "https://image.tmdb.org/t/p/w200/profile.jpg",
                )

            // When
            repository.save(personWithoutImage)
            val foundBeforeEnrichment = repository.findByTmdbId(123)

            repository.save(personWithImage) // Enrich with image path
            val foundAfterEnrichment = repository.findByTmdbId(123)

            // Then
            assertEquals(personWithoutImage, foundBeforeEnrichment)
            assertEquals(personWithImage, foundAfterEnrichment)
            assertEquals("https://image.tmdb.org/t/p/w200/profile.jpg", foundAfterEnrichment?.imagePath)
        }

    @Test
    fun `should handle all department types correctly`() =
        runBlocking {
            // Given
            val persons =
                listOf(
                    Person(1, "Actor", Department.ACTING, null),
                    Person(2, "Director", Department.DIRECTING, null),
                    Person(3, "Writer", Department.WRITING, null),
                    Person(4, "Other", Department.OTHER, null),
                )

            // When
            persons.forEach { repository.save(it) }

            // Then
            persons.forEach { person ->
                val found = repository.findByTmdbId(person.tmdbId)
                assertEquals(person, found)
                assertEquals(person.department, found?.department)
            }
        }
}
