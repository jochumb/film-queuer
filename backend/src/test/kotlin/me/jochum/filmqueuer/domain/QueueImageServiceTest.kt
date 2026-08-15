package me.jochum.filmqueuer.domain

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueueImageServiceTest {
    private lateinit var queueRepository: QueueRepository
    private lateinit var queueImageStorage: QueueImageStorage
    private lateinit var service: QueueImageService

    @BeforeEach
    fun setup() {
        queueRepository = mockk()
        queueImageStorage = mockk()
        service = QueueImageService(queueRepository, queueImageStorage)
    }

    @Test
    fun `setImage should download, persist, and delete the previous file`() =
        runBlocking {
            val queueId = UUID.randomUUID()
            val existing = NamedQueue(id = queueId, name = "Weekend Watchlist", imagePath = "/images/queue/old.jpg")

            coEvery { queueRepository.findById(queueId) } returns existing
            coEvery { queueImageStorage.store("https://example.com/new.jpg") } returns "/images/queue/new.jpg"
            coEvery { queueRepository.updateImagePath(queueId, "/images/queue/new.jpg") } returns true
            coEvery { queueImageStorage.delete("/images/queue/old.jpg") } returns Unit

            val result = service.setImage(queueId, "https://example.com/new.jpg")

            assertTrue(result)
            coVerify { queueImageStorage.delete("/images/queue/old.jpg") }
        }

    @Test
    fun `setImage should not attempt to delete anything when there was no previous image`() =
        runBlocking {
            val queueId = UUID.randomUUID()
            val existing = NamedQueue(id = queueId, name = "Weekend Watchlist", imagePath = null)

            coEvery { queueRepository.findById(queueId) } returns existing
            coEvery { queueImageStorage.store("https://example.com/new.jpg") } returns "/images/queue/new.jpg"
            coEvery { queueRepository.updateImagePath(queueId, "/images/queue/new.jpg") } returns true

            val result = service.setImage(queueId, "https://example.com/new.jpg")

            assertTrue(result)
            coVerify(exactly = 0) { queueImageStorage.delete(any()) }
        }

    @Test
    fun `setImage should clean up the newly-stored file if the DB update fails`() =
        runBlocking {
            val queueId = UUID.randomUUID()
            val existing = NamedQueue(id = queueId, name = "Weekend Watchlist")

            coEvery { queueRepository.findById(queueId) } returns existing
            coEvery { queueImageStorage.store("https://example.com/new.jpg") } returns "/images/queue/new.jpg"
            coEvery { queueRepository.updateImagePath(queueId, "/images/queue/new.jpg") } returns false
            coEvery { queueImageStorage.delete("/images/queue/new.jpg") } returns Unit

            val result = service.setImage(queueId, "https://example.com/new.jpg")

            assertFalse(result)
            coVerify { queueImageStorage.delete("/images/queue/new.jpg") }
        }

    @Test
    fun `setImage should return false and not download anything for a non-existent queue`() =
        runBlocking {
            val queueId = UUID.randomUUID()
            coEvery { queueRepository.findById(queueId) } returns null

            val result = service.setImage(queueId, "https://example.com/new.jpg")

            assertFalse(result)
            coVerify(exactly = 0) { queueImageStorage.store(any()) }
        }

    @Test
    fun `setImage should return false for a PersonQueue without downloading anything`() =
        runBlocking {
            val queueId = UUID.randomUUID()
            coEvery { queueRepository.findById(queueId) } returns PersonQueue(id = queueId, personTmdbId = 123)

            val result = service.setImage(queueId, "https://example.com/new.jpg")

            assertFalse(result)
            coVerify(exactly = 0) { queueImageStorage.store(any()) }
        }

    @Test
    fun `clearImage should update the DB and delete the file`() =
        runBlocking {
            val queueId = UUID.randomUUID()
            val existing = NamedQueue(id = queueId, name = "Weekend Watchlist", imagePath = "/images/queue/old.jpg")

            coEvery { queueRepository.findById(queueId) } returns existing
            coEvery { queueRepository.updateImagePath(queueId, null) } returns true
            coEvery { queueImageStorage.delete("/images/queue/old.jpg") } returns Unit

            val result = service.clearImage(queueId)

            assertTrue(result)
            coVerify { queueImageStorage.delete("/images/queue/old.jpg") }
        }

    @Test
    fun `clearImage should return false for a non-existent queue`() =
        runBlocking {
            val queueId = UUID.randomUUID()
            coEvery { queueRepository.findById(queueId) } returns null

            val result = service.clearImage(queueId)

            assertFalse(result)
            coVerify(exactly = 0) { queueImageStorage.delete(any()) }
        }

    @Test
    fun `deleteImageFile should delete a named queue's stored file`() =
        runBlocking {
            val queue = NamedQueue(id = UUID.randomUUID(), name = "Weekend Watchlist", imagePath = "/images/queue/old.jpg")
            coEvery { queueImageStorage.delete("/images/queue/old.jpg") } returns Unit

            service.deleteImageFile(queue)

            coVerify { queueImageStorage.delete("/images/queue/old.jpg") }
        }

    @Test
    fun `deleteImageFile should do nothing for a named queue without an image`() =
        runBlocking {
            val queue = NamedQueue(id = UUID.randomUUID(), name = "Weekend Watchlist")

            service.deleteImageFile(queue)

            coVerify(exactly = 0) { queueImageStorage.delete(any()) }
        }

    @Test
    fun `deleteImageFile should do nothing for a PersonQueue`() =
        runBlocking {
            val queue = PersonQueue(id = UUID.randomUUID(), personTmdbId = 123)

            service.deleteImageFile(queue)

            coVerify(exactly = 0) { queueImageStorage.delete(any()) }
        }
}
