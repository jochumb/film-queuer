package me.jochum.filmqueuer.adapters.web

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals

class ImageControllerTest {
    private lateinit var storageDir: File

    @BeforeEach
    fun setup() {
        storageDir = Files.createTempDirectory("image-controller-test").toFile()
    }

    @AfterEach
    fun cleanup() {
        storageDir.deleteRecursively()
    }

    @Test
    fun `GET images queue filename should return the file contents when it exists`() =
        testApplication {
            File(storageDir, "photo.jpg").writeBytes(byteArrayOf(1, 2, 3))

            routing {
                configureImageRoutes(storageDir.path)
            }

            val response = client.get("/images/queue/photo.jpg")

            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf<Byte>(1, 2, 3), response.bodyAsBytes().toList())
        }

    @Test
    fun `GET images queue filename should return 404 when the file does not exist`() =
        testApplication {
            routing {
                configureImageRoutes(storageDir.path)
            }

            val response = client.get("/images/queue/missing.jpg")

            assertEquals(HttpStatusCode.NotFound, response.status)
        }

    @Test
    fun `GET images queue filename should reject a path traversal attempt`() =
        testApplication {
            routing {
                configureImageRoutes(storageDir.path)
            }

            val response = client.get("/images/queue/..%2F..%2Fetc%2Fpasswd")

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }
}
