package me.jochum.filmqueuer.adapters.storage

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import me.jochum.filmqueuer.domain.InvalidImageUrlException
import me.jochum.filmqueuer.domain.QueueImageStorage
import java.io.File
import java.util.UUID

/**
 * Stores a local copy of a queue thumbnail on disk under [storageDir], served back by
 * ImageController at [publicPathPrefix]. [storageDir] should be a Docker-mounted volume so
 * copies survive container rebuilds/redeploys.
 */
class LocalQueueImageStorage(
    val storageDir: String = System.getenv("QUEUE_IMAGE_DIR") ?: "data/queue-images",
    private val publicPathPrefix: String = "/images/queue",
    private val httpClient: HttpClient = HttpClient(CIO),
    private val maxBytes: Long = 20 * 1024 * 1024,
) : QueueImageStorage {
    init {
        File(storageDir).mkdirs()
    }

    override suspend fun store(sourceUrl: String): String {
        val response =
            try {
                httpClient.get(sourceUrl)
            } catch (e: Exception) {
                throw InvalidImageUrlException("Could not fetch image from URL: ${e.message}")
            }

        if (!response.status.isSuccess()) {
            throw InvalidImageUrlException("Image URL returned HTTP ${response.status.value}")
        }

        val extension = extensionFor(response) ?: throw InvalidImageUrlException("URL did not point to a supported image type")

        val bytes = response.body<ByteArray>()
        if (bytes.size.toLong() > maxBytes) {
            throw InvalidImageUrlException("Image is too large (max ${maxBytes / (1024 * 1024)}MB)")
        }

        val filename = "${UUID.randomUUID()}.$extension"
        File(storageDir, filename).writeBytes(bytes)
        return "$publicPathPrefix/$filename"
    }

    override suspend fun delete(localPath: String) {
        val filename = localPath.substringAfterLast('/')
        File(storageDir, filename).delete()
    }

    private fun extensionFor(response: HttpResponse): String? =
        when (response.contentType()?.let { "${it.contentType}/${it.contentSubtype}" }) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> null
        }
}
