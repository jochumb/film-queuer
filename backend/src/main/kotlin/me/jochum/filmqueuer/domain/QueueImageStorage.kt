package me.jochum.filmqueuer.domain

class InvalidImageUrlException(message: String) : Exception(message)

/**
 * Downloads and persists a local copy of an externally-hosted image (e.g. a named queue's
 * thumbnail), so the app doesn't break if the source URL later changes or disappears.
 * Returns/accepts a local serving path (e.g. "/images/queue/<file>.jpg"), not the original URL.
 */
interface QueueImageStorage {
    /**
     * @throws InvalidImageUrlException if the URL can't be fetched or isn't a supported image type.
     */
    suspend fun store(sourceUrl: String): String

    suspend fun delete(localPath: String)
}
