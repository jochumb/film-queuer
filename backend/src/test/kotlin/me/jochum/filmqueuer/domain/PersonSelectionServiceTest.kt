package me.jochum.filmqueuer.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class PersonSelectionServiceTest {
    private lateinit var personRepository: PersonRepository
    private lateinit var queueRepository: QueueRepository
    private lateinit var service: PersonSelectionService

    @BeforeEach
    fun setup() {
        personRepository = mockk()
        queueRepository = mockk()
        service = PersonSelectionService(personRepository, queueRepository)
    }

    @Test
    fun `should select person and create queue`() =
        runBlocking {
            // Given
            val person =
                Person(
                    tmdbId = 123,
                    name = "Tom Hanks",
                    department = Department.ACTING,
                )

            val queueId = UUID.randomUUID()
            val personQueue =
                PersonQueue(
                    id = queueId,
                    personTmdbId = 123,
                )

            coEvery { personRepository.save(person) } returns person
            coEvery { queueRepository.save(match<PersonQueue> { it.personTmdbId == person.tmdbId }) } returns personQueue

            // When
            val result = service.selectPerson(123, "Tom Hanks", Department.ACTING)

            // Then
            assertEquals(person, result.person)
            assertEquals(personQueue, result.queue)

            coVerify { personRepository.save(person) }
            coVerify { queueRepository.save(match<PersonQueue> { it.personTmdbId == person.tmdbId }) }
        }

    @Test
    fun `should handle different departments correctly`() =
        runBlocking {
            // Given
            val person =
                Person(
                    tmdbId = 456,
                    name = "Steven Spielberg",
                    department = Department.DIRECTING,
                )

            val queueId = UUID.randomUUID()
            val personQueue =
                PersonQueue(
                    id = queueId,
                    personTmdbId = 456,
                )

            coEvery { personRepository.save(person) } returns person
            coEvery { queueRepository.save(match<PersonQueue> { it.personTmdbId == person.tmdbId }) } returns personQueue

            // When
            val result = service.selectPerson(456, "Steven Spielberg", Department.DIRECTING)

            // Then
            assertEquals(Department.DIRECTING, result.person.department)
            assertEquals(456, result.queue.personTmdbId)

            coVerify { personRepository.save(person) }
            coVerify { queueRepository.save(match<PersonQueue> { it.personTmdbId == person.tmdbId }) }
        }
}
