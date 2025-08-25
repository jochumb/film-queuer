package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object QueueTable : Table("queues") {
    val id = uuid("id")
    val type = varchar("type", 50)
    val personTmdbId = integer("person_tmdb_id").nullable()
    val createdAt = timestamp("created_at").default(Instant.now())

    override val primaryKey = PrimaryKey(id)
}
