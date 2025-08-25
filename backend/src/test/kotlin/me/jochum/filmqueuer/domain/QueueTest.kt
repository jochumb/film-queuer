package me.jochum.filmqueuer.domain

import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QueueTest {
    @Test
    fun `PersonQueue should inherit from Queue`() {
        // Given
        val id = UUID.randomUUID()
        val personTmdbId = 123
        val createdAt = Instant.now()

        // When
        val personQueue =
            PersonQueue(
                id = id,
                personTmdbId = personTmdbId,
                createdAt = createdAt,
            )

        // Then
        assertTrue(personQueue is Queue)
        assertEquals(id, personQueue.id)
        assertEquals(personTmdbId, personQueue.personTmdbId)
        assertEquals(createdAt, personQueue.createdAt)
    }

    @Test
    fun `PersonQueue should have default createdAt`() {
        // Given
        val id = UUID.randomUUID()
        val personTmdbId = 123

        // When
        val personQueue =
            PersonQueue(
                id = id,
                personTmdbId = personTmdbId,
            )

        // Then
        assertNotNull(personQueue.createdAt)
        assertTrue(personQueue.createdAt.isBefore(Instant.now().plusSeconds(1)))
        assertTrue(personQueue.createdAt.isAfter(Instant.now().minusSeconds(1)))
    }

    @Test
    fun `PersonQueue should be equal when all properties match`() {
        // Given
        val id = UUID.randomUUID()
        val personTmdbId = 123
        val createdAt = Instant.now()

        // When
        val queue1 = PersonQueue(id = id, personTmdbId = personTmdbId, createdAt = createdAt)
        val queue2 = PersonQueue(id = id, personTmdbId = personTmdbId, createdAt = createdAt)

        // Then
        assertEquals(queue1, queue2)
        assertEquals(queue1.hashCode(), queue2.hashCode())
    }

    @Test
    fun `PersonQueue should have proper string representation`() {
        // Given
        val id = UUID.randomUUID()
        val personTmdbId = 123
        val createdAt = Instant.now()

        // When
        val personQueue = PersonQueue(id = id, personTmdbId = personTmdbId, createdAt = createdAt)
        val stringRepresentation = personQueue.toString()

        // Then
        assertTrue(stringRepresentation.contains("PersonQueue"))
        assertTrue(stringRepresentation.contains(id.toString()))
        assertTrue(stringRepresentation.contains(personTmdbId.toString()))
        assertTrue(stringRepresentation.contains(createdAt.toString()))
    }
}
