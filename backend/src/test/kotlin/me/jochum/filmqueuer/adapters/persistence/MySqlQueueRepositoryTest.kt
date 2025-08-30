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

    @Test
    fun `should reorder queues successfully`() =
        runBlocking {
            // Given
            val queue1 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 123)
            val queue2 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 456)
            val queue3 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 789)

            repository.save(queue1)
            repository.save(queue2)
            repository.save(queue3)

            // When - reorder to: queue3, queue1, queue2
            val reorderSuccess = repository.reorderQueues(listOf(queue3.id, queue1.id, queue2.id))
            val reorderedQueues = repository.findAll()

            // Then
            assertTrue(reorderSuccess)
            assertEquals(3, reorderedQueues.size)
            assertEquals(queue3.id, reorderedQueues[0].id)
            assertEquals(queue1.id, reorderedQueues[1].id)
            assertEquals(queue2.id, reorderedQueues[2].id)
        }

    @Test
    fun `should handle empty queue list for reordering`() =
        runBlocking {
            // Given - empty list
            val emptyList = emptyList<UUID>()

            // When
            val reorderSuccess = repository.reorderQueues(emptyList)

            // Then
            assertTrue(reorderSuccess)
        }

    @Test
    fun `should handle partial queue list for reordering`() =
        runBlocking {
            // Given
            val queue1 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 123)
            val queue2 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 456)
            val queue3 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 789)

            repository.save(queue1)
            repository.save(queue2)
            repository.save(queue3)

            // When - only reorder queue2 and queue3, swapping their positions
            val reorderSuccess = repository.reorderQueues(listOf(queue3.id, queue2.id))
            val allQueues = repository.findAll()

            // Then
            assertTrue(reorderSuccess)
            assertEquals(3, allQueues.size)
            // queue3 gets sort_order 0, queue2 gets sort_order 1, queue1 keeps sort_order 2
            assertEquals(queue3.id, allQueues[0].id)
            assertEquals(queue2.id, allQueues[1].id)
            assertEquals(queue1.id, allQueues[2].id)
        }

    @Test
    fun `should handle non-existent queue IDs in reorder list`() =
        runBlocking {
            // Given
            val queue1 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 123)
            val nonExistentId = UUID.randomUUID()

            repository.save(queue1)

            // When - try to reorder with non-existent ID
            val reorderSuccess = repository.reorderQueues(listOf(nonExistentId, queue1.id))

            // Then - should still succeed (updates what exists, ignores what doesn't)
            assertTrue(reorderSuccess)
            val allQueues = repository.findAll()
            assertEquals(1, allQueues.size)
            assertEquals(queue1.id, allQueues[0].id)
        }

    @Test
    fun `should maintain sort order after adding new queues`() =
        runBlocking {
            // Given - save queues in order
            val queue1 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 123)
            val queue2 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 456)

            repository.save(queue1)
            repository.save(queue2)

            // When - add a third queue
            val queue3 = PersonQueue(id = UUID.randomUUID(), personTmdbId = 789)
            repository.save(queue3)

            val allQueues = repository.findAll()

            // Then - should be in creation order (sort_order 0, 1, 2)
            assertEquals(3, allQueues.size)
            assertEquals(queue1.id, allQueues[0].id)
            assertEquals(queue2.id, allQueues[1].id)
            assertEquals(queue3.id, allQueues[2].id)
        }
}
