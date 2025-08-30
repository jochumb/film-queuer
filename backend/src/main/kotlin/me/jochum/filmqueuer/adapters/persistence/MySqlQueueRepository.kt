package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.PersonQueue
import me.jochum.filmqueuer.domain.Queue
import me.jochum.filmqueuer.domain.QueueRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class MySqlQueueRepository : QueueRepository {
    private fun mapRowToQueue(row: ResultRow): Queue {
        return when (row[QueueTable.type]) {
            "PERSON" ->
                PersonQueue(
                    id = row[QueueTable.id],
                    personTmdbId = row[QueueTable.personTmdbId]!!,
                    createdAt = row[QueueTable.createdAt],
                )
            else -> throw IllegalArgumentException("Unknown queue type: ${row[QueueTable.type]}")
        }
    }

    override suspend fun save(queue: Queue): Queue =
        newSuspendedTransaction {
            when (queue) {
                is PersonQueue -> {
                    // Get the next sort order (max + 1)
                    val nextSortOrder = QueueTable.selectAll().maxOfOrNull { it[QueueTable.sortOrder] }?.plus(1) ?: 0

                    QueueTable.insert {
                        it[id] = queue.id
                        it[type] = "PERSON"
                        it[personTmdbId] = queue.personTmdbId
                        it[createdAt] = queue.createdAt
                        it[sortOrder] = nextSortOrder
                    }
                    queue
                }
                else -> throw IllegalArgumentException("Unsupported queue type: ${queue::class.simpleName}")
            }
        }

    override suspend fun findById(id: UUID): Queue? =
        newSuspendedTransaction {
            QueueTable.selectAll().where { QueueTable.id eq id }
                .singleOrNull()
                ?.let(::mapRowToQueue)
        }

    override suspend fun findAll(): List<Queue> =
        newSuspendedTransaction {
            QueueTable.selectAll().orderBy(QueueTable.sortOrder).map(::mapRowToQueue)
        }

    override suspend fun deleteById(id: UUID): Boolean =
        newSuspendedTransaction {
            QueueTable.deleteWhere { QueueTable.id eq id } > 0
        }

    override suspend fun reorderQueues(queueIds: List<UUID>): Boolean =
        newSuspendedTransaction {
            val allIds = QueueTable.selectAll().map { it[QueueTable.id] }

            try {
                (queueIds + (allIds - queueIds.toSet())).forEachIndexed { index, queueId ->
                    QueueTable.update({ QueueTable.id eq queueId }) {
                        it[sortOrder] = index
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
}
