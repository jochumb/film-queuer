package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object QueueFilmTable : Table("queue_films") {
    val queueId = uuid("queue_id").references(QueueTable.id)
    val filmTmdbId = integer("film_tmdb_id").references(FilmTable.tmdbId)
    val addedAt = datetime("added_at")

    override val primaryKey = PrimaryKey(queueId, filmTmdbId)
}
