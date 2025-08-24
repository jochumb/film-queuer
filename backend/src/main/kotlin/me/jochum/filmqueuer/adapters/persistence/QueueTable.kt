package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object QueueTable : Table("queues") {
    val id = uuid("id")
    val type = varchar("type", 50)
    val personTmdbId = integer("person_tmdb_id").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}
