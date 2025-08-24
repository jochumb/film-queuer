package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

object DatabasePurgeUtility {
    fun purgeAllTables() {
        println("Starting database purge...")

        transaction {
            val deletedQueues = QueueTable.deleteAll()
            val deletedPersons = PersonTable.deleteAll()

            println("Purged $deletedQueues records from queues table")
            println("Purged $deletedPersons records from persons table")
            println("Database purge completed successfully!")
        }
    }

    fun purgeQueueTable() {
        println("Purging queue table...")

        transaction {
            val deletedQueues = QueueTable.deleteAll()
            println("Purged $deletedQueues records from queues table")
        }
    }

    fun purgePersonTable() {
        println("Purging person table...")

        transaction {
            val deletedPersons = PersonTable.deleteAll()
            println("Purged $deletedPersons records from persons table")
        }
    }
}
