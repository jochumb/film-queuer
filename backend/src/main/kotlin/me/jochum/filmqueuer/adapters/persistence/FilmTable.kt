package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table

object FilmTable : Table("films") {
    val tmdbId = integer("tmdb_id")
    val title = varchar("title", 255)
    val originalTitle = varchar("original_title", 255).nullable()
    val releaseDate = varchar("release_date", 10).nullable()

    override val primaryKey = PrimaryKey(tmdbId)
}
