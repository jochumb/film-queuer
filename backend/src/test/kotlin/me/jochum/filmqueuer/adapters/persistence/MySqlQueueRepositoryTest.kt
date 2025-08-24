package me.jochum.filmqueuer.adapters.persistence

import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.domain.PersonQueue
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MySqlQueueRepositoryTest {
    private lateinit var repository: MySqlQueueRepository

    @BeforeEach
    fun setup() {
        Database.connect("jdbc:h2:mem:test${System.nanoTime()};DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver")
        transaction {
            SchemaUtils.create(QueueTable)
        }
        repository = MySqlQueueRepository()
    }

    @Test
    fun `should save and retrieve PersonQueue`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val personQueue =
                PersonQueue(
                    id = queueId,
                    personTmdbId = 123,
                )

            // When
            val savedQueue = repository.save(personQueue)
            val retrievedQueue = repository.findById(queueId)

            // Then
            assertEquals(personQueue, savedQueue)
            assertNotNull(retrievedQueue)
            assertEquals(queueId, retrievedQueue.id)
            assertEquals(123, (retrievedQueue as PersonQueue).personTmdbId)
        }

    @Test
    fun `should return null when queue not found`() =
        runBlocking {
            // Given
            val nonExistentId = UUID.randomUUID()

            // When
            val result = repository.findById(nonExistentId)

            // Then
            assertNull(result)
        }

    @Test
    fun `should find all queues`() =
        runBlocking {
            // Given
            val queue1 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 123)
            val queue2 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 456)

            // When
            repository.save(queue1)
            repository.save(queue2)
            val allQueues = repository.findAll()

            // Then
            assertEquals(2, allQueues.size)
            assertTrue(allQueues.any { (it as PersonQueue).personTmdbId == 123 })
            assertTrue(allQueues.any { (it as PersonQueue).personTmdbId == 456 })
        }

    @Test
    fun `should delete queue by id`() =
        runBlocking {
            // Given
            val queueId = UUID.randomUUID()
            val personQueue = PersonQueue(id = queueId, personTmdbId = 123)
            repository.save(personQueue)

            // When
            val deleted = repository.deleteById(queueId)
            val retrievedQueue = repository.findById(queueId)

            // Then
            assertTrue(deleted)
            assertNull(retrievedQueue)
        }

    @Test
    fun `should return false when deleting non-existent queue`() =
        runBlocking {
            // Given
            val nonExistentId = UUID.randomUUID()

            // When
            val deleted = repository.deleteById(nonExistentId)

            // Then
            assertTrue(!deleted)
        }
}
