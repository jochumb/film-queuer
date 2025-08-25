package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object QueueFilmTable : Table("queue_films") {
    val queueId = uuid("queue_id").references(QueueTable.id)
    val filmTmdbId = integer("film_tmdb_id").references(FilmTable.tmdbId)
    val addedAt = timestamp("added_at")
    val sortOrder = integer("sort_order").default(0)

    override val primaryKey = PrimaryKey(queueId, filmTmdbId)
}
