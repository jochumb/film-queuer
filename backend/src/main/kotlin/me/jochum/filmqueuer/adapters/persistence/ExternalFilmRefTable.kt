package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object ExternalFilmRefTable : Table("external_film_refs") {
    val id = uuid("id")
    val sourceName = varchar("source", 50)
    val title = varchar("title", 255)
    val year = integer("year").nullable()
    val filmTmdbId = integer("film_tmdb_id").references(FilmTable.tmdbId).nullable()
    val owned = bool("owned").default(false)
    val watched = bool("watched").default(false)
    val createdAt = timestamp("created_at").default(Instant.now())
    val removed = bool("removed").default(false)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(sourceName, title, year)
    }
}
