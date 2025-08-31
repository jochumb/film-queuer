package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object FilmTable : Table("films") {
    val tmdbId = integer("tmdb_id")
    val title = varchar("title", 255)
    val originalTitle = varchar("original_title", 255).nullable()
    val releaseDate = date("release_date").nullable()
    val runtime = integer("runtime").nullable()
    val genres = varchar("genres", 500).nullable()
    val posterPath = varchar("poster_path", 500).nullable()

    override val primaryKey = PrimaryKey(tmdbId)
}
