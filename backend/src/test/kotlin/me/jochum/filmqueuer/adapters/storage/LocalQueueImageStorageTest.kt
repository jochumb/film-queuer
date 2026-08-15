package me.jochum.filmqueuer.adapters.storage

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import me.jochum.filmqueuer.domain.InvalidImageUrlException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalQueueImageStorageTest {
    private lateinit var storageDir: File

    @BeforeEach
    fun setup() {
        storageDir = Files.createTempDirectory("queue-image-test").toFile()
    }

    @AfterEach
    fun cleanup() {
        storageDir.deleteRecursively()
    }

    private fun storageWith(
        contentType: String = "image/jpeg",
        status: HttpStatusCode = HttpStatusCode.OK,
        bytes: ByteArray = byteArrayOf(1, 2, 3, 4),
        maxBytes: Long = 20 * 1024 * 1024,
    ): LocalQueueImageStorage {
        val engine =
            MockEngine {
                respond(
                    content = bytes,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, contentType),
                )
            }
        return LocalQueueImageStorage(
            storageDir = storageDir.path,
            httpClient = HttpClient(engine),
            maxBytes = maxBytes,
        )
    }

    @Test
    fun `store should download and write the image, returning a path under the public prefix`() =
        runBlocking {
            val storage = storageWith(contentType = "image/jpeg", bytes = byteArrayOf(1, 2, 3))

            val path = storage.store("https://example.com/photo.jpg")

            assertTrue(path.startsWith("/images/queue/"))
            assertTrue(path.endsWith(".jpg"))
            val filename = path.substringAfterLast('/')
            assertTrue(File(storageDir, filename).exists())
            assertEquals(3, File(storageDir, filename).readBytes().size)
        }

    @Test
    fun `store should pick the extension matching the response content type`() =
        runBlocking {
            val storage = storageWith(contentType = "image/png")
            val path = storage.store("https://example.com/photo")
            assertTrue(path.endsWith(".png"))
        }

    @Test
    fun `store should reject a non-image content type`() =
        runBlocking {
            val storage = storageWith(contentType = "text/html")
            assertFailsWith<InvalidImageUrlException> {
                runBlocking { storage.store("https://example.com/not-an-image") }
            }
        }

    @Test
    fun `store should reject a non-success HTTP status`() =
        runBlocking {
            val engine = MockEngine { respondError(HttpStatusCode.NotFound) }
            val storage = LocalQueueImageStorage(storageDir = storageDir.path, httpClient = HttpClient(engine))

            assertFailsWith<InvalidImageUrlException> {
                runBlocking { storage.store("https://example.com/missing.jpg") }
            }
        }

    @Test
    fun `store should reject an image larger than the configured max size`() =
        runBlocking {
            val storage = storageWith(bytes = ByteArray(100), maxBytes = 10)
            assertFailsWith<InvalidImageUrlException> {
                runBlocking { storage.store("https://example.com/huge.jpg") }
            }
        }

    @Test
    fun `store should wrap network failures as InvalidImageUrlException`() =
        runBlocking {
            val engine = MockEngine { throw RuntimeException("connection refused") }
            val storage = LocalQueueImageStorage(storageDir = storageDir.path, httpClient = HttpClient(engine))

            assertFailsWith<InvalidImageUrlException> {
                runBlocking { storage.store("https://example.com/unreachable.jpg") }
            }
        }

    @Test
    fun `delete should remove a stored file`() =
        runBlocking {
            val storage = storageWith()
            val path = storage.store("https://example.com/photo.jpg")

            storage.delete(path)

            val filename = path.substringAfterLast('/')
            assertFalse(File(storageDir, filename).exists())
        }

    @Test
    fun `delete should not throw for a file that does not exist`() =
        runBlocking {
            val storage = storageWith()
            storage.delete("/images/queue/does-not-exist.jpg")
        }
}
