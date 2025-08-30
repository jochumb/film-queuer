package me.jochum.filmqueuer.domain

import java.util.UUID

interface QueueRepository {
    suspend fun save(queue: Queue): Queue

    suspend fun findById(id: UUID): Queue?

    suspend fun findAll(): List<Queue>

    suspend fun deleteById(id: UUID): Boolean

    suspend fun reorderQueues(queueIds: List<UUID>): Boolean
}
