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
                    QueueTable.insert {
                        it[id] = queue.id
                        it[type] = "PERSON"
                        it[personTmdbId] = queue.personTmdbId
                        it[createdAt] = queue.createdAt
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
            QueueTable.selectAll().map(::mapRowToQueue)
        }

    override suspend fun deleteById(id: UUID): Boolean =
        newSuspendedTransaction {
            QueueTable.deleteWhere { QueueTable.id eq id } > 0
        }
}
