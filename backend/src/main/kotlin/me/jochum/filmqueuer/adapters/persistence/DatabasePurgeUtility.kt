package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction

object DatabasePurgeUtility {
    fun purgeAllTables() {
        println("Starting database purge...")

        transaction {
            // Delete in order to respect foreign key constraints
            val deletedQueueFilms = QueueFilmTable.deleteAll()
            val deletedFilms = FilmTable.deleteAll()
            val deletedQueues = QueueTable.deleteAll()
            val deletedPersons = PersonTable.deleteAll()

            println("Purged $deletedQueueFilms records from queue_films table")
            println("Purged $deletedFilms records from films table")
            println("Purged $deletedQueues records from queues table")
            println("Purged $deletedPersons records from persons table")
            println("Database purge completed successfully!")
        }
    }

    fun purgeQueueTable() {
        println("Purging queue table...")

        transaction {
            // Delete queue films first due to foreign key constraint
            val deletedQueueFilms = QueueFilmTable.deleteAll()
            val deletedQueues = QueueTable.deleteAll()
            println("Purged $deletedQueueFilms records from queue_films table")
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

    fun purgeFilmTables() {
        println("Purging film tables...")

        transaction {
            val deletedQueueFilms = QueueFilmTable.deleteAll()
            val deletedFilms = FilmTable.deleteAll()
            println("Purged $deletedQueueFilms records from queue_films table")
            println("Purged $deletedFilms records from films table")
        }
    }
}
