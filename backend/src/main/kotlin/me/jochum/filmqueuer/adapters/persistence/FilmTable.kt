package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object FilmTable : Table("films") {
    val id = uuid("id")
    val tmdbId = integer("tmdb_id")
    val title = varchar("title", 255)
    val originalTitle = varchar("original_title", 255).nullable()
    val releaseDate = date("release_date").nullable()
    val runtime = integer("runtime").nullable()
    val genres = varchar("genres", 500).nullable()
    val posterPath = varchar("poster_path", 500).nullable()
    val tv = bool("tv").default(false)
    val sortTitle = varchar("sort_title", 255)

    override val primaryKey = PrimaryKey(id)

    init {
        // (tmdbId, tv) is the business key TMDB lookups use - movie and TV ids are separate
        // namespaces and can collide on the same number, so tv must be part of the uniqueness.
        uniqueIndex(tmdbId, tv)
    }
}
