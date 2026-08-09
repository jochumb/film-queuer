package me.jochum.filmqueuer.adapters.persistence

import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.domain.Department
import me.jochum.filmqueuer.domain.Person
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
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
            PersonTable.deleteAll()
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

            // Then - save() fills in the default "Lastname, Firstname" sort name since none was
            // given
            val expected = person.copy(sortName = "Doe, John")
            assertEquals(expected, savedPerson)

            val foundPerson = repository.findByTmdbId(123)
            assertEquals(expected, foundPerson)
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

            // Then - name/department/imagePath are replaced, but the sort name computed for
            // person1 ("Doe, John") is preserved rather than recomputed for person2's name,
            // matching the same-real-person-refreshed-by-tmdbId semantics save() is built around.
            val foundPerson = repository.findByTmdbId(123)
            assertEquals(person2.copy(sortName = "Doe, John"), foundPerson)
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
            assertTrue(allPersons.contains(person1.copy(sortName = "Doe, John")))
            assertTrue(allPersons.contains(person2.copy(sortName = "Smith, Jane")))
            assertTrue(allPersons.contains(person3.copy(sortName = "Writer, Bob")))
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
            assertEquals(personWithoutImage.copy(sortName = "Doe, John"), foundBeforeEnrichment)
            assertEquals(personWithImage.copy(sortName = "Doe, John"), foundAfterEnrichment)
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

            // Then - single-word names have no space, so the default sort name equals the name
            persons.forEach { person ->
                val found = repository.findByTmdbId(person.tmdbId)
                assertEquals(person.copy(sortName = person.name), found)
                assertEquals(person.department, found?.department)
            }
        }

    @Test
    fun `updateSortName should overwrite the computed default and survive a later save`() =
        runBlocking {
            // Given
            val person = Person(tmdbId = 1, name = "Guillermo del Toro", department = Department.DIRECTING)
            repository.save(person) // default sortName becomes "Toro, Guillermo del"

            // When
            val updated = repository.updateSortName(1, "del Toro")

            // Then
            assertTrue(updated)
            assertEquals("del Toro", repository.findByTmdbId(1)?.sortName)

            // And a later re-save (e.g. re-resolving this person from another film match)
            // must not clobber the manual correction
            repository.save(person.copy(imagePath = "https://image.tmdb.org/t/p/w200/new.jpg"))
            assertEquals("del Toro", repository.findByTmdbId(1)?.sortName)
        }

    @Test
    fun `updateSortName should return false when person does not exist`() =
        runBlocking {
            // When
            val updated = repository.updateSortName(999, "Nobody")

            // Then
            assertTrue(!updated)
        }
}
