package me.jochum.filmqueuer.domain

import java.util.UUID

/**
 * Orchestrates setting/clearing a named queue's thumbnail: downloads a local copy via
 * QueueImageStorage before persisting the new path, and cleans up the previous file so replaced
 * or cleared images don't leak orphaned files on disk.
 */
class QueueImageService(
    private val queueRepository: QueueRepository,
    private val queueImageStorage: QueueImageStorage,
) {
    suspend fun setImage(
        queueId: UUID,
        sourceUrl: String,
    ): Boolean {
        val existing = queueRepository.findById(queueId) as? NamedQueue ?: return false

        val newPath = queueImageStorage.store(sourceUrl)
        val updated = queueRepository.updateImagePath(queueId, newPath)

        if (updated) {
            existing.imagePath?.let { queueImageStorage.delete(it) }
        } else {
            queueImageStorage.delete(newPath)
        }

        return updated
    }

    suspend fun clearImage(queueId: UUID): Boolean {
        val existing = queueRepository.findById(queueId) as? NamedQueue ?: return false

        val updated = queueRepository.updateImagePath(queueId, null)
        if (updated) {
            existing.imagePath?.let { queueImageStorage.delete(it) }
        }

        return updated
    }

    /**
     * Cleans up a named queue's stored image file when the queue itself is deleted, so it
     * doesn't leak an orphaned file on disk. No-op for a PersonQueue or a queue without an image.
     */
    suspend fun deleteImageFile(queue: Queue) {
        if (queue is NamedQueue) {
            queue.imagePath?.let { queueImageStorage.delete(it) }
        }
    }
}
