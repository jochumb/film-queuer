package me.jochum.filmqueuer.adapters.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig {
    fun init() {
        val config =
            HikariConfig().apply {
                driverClassName = "com.mysql.cj.jdbc.Driver"
                jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:mysql://database:3306/filmqueuer"
                username = System.getenv("DATABASE_USER") ?: "root"
                password = System.getenv("DATABASE_PASSWORD") ?: "password"
                maximumPoolSize = 10
                connectionTimeout = 30000
                validationTimeout = 5000
                leakDetectionThreshold = 60000
            }

        println("Attempting to connect to database: ${config.jdbcUrl}")

        runBlocking {
            var attempts = 0
            val maxAttempts = 10

            while (attempts < maxAttempts) {
                try {
                    val dataSource = HikariDataSource(config)
                    Database.connect(dataSource)

                    // Test the connection and create schema
                    transaction {
                        SchemaUtils.create(PersonTable, QueueTable, FilmTable, QueueFilmTable)
                    }

                    println("Successfully connected to database and created schema")
                    break
                } catch (e: Exception) {
                    attempts++
                    println("Database connection attempt $attempts failed: ${e.message}")

                    if (attempts >= maxAttempts) {
                        throw Exception("Failed to connect to database after $maxAttempts attempts", e)
                    }

                    println("Retrying in 5 seconds...")
                    delay(5000)
                }
            }

            try {
                when (System.getenv("PURGE_MODE")) {
                    "queues" -> DatabasePurgeUtility.purgeQueueTable()
                    "persons" -> DatabasePurgeUtility.purgePersonTable()
                    "all" -> DatabasePurgeUtility.purgeAllTables()
                    else -> return@runBlocking
                }
            } catch (e: Exception) {
                println("Error during database purge: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
