package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table

object PersonTable : Table("persons") {
    val tmdbId = integer("tmdb_id")
    val name = varchar("name", 255)
    val department = enumerationByName("department", 50, me.jochum.filmqueuer.domain.Department::class)
    val imagePath = varchar("image_path", 500).nullable()

    override val primaryKey = PrimaryKey(tmdbId)
}
