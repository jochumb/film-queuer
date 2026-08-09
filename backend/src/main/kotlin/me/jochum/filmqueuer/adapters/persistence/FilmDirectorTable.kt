package me.jochum.filmqueuer.adapters.persistence

import org.jetbrains.exposed.sql.Table

object FilmDirectorTable : Table("film_directors") {
    val filmTmdbId = integer("film_tmdb_id").references(FilmTable.tmdbId)
    val personTmdbId = integer("person_tmdb_id").references(PersonTable.tmdbId)

    // TMDB's crew-list order, preserved so a well-defined "primary director" (billingOrder = 0)
    // exists for sorting/display headline purposes on co-directed films.
    val billingOrder = integer("billing_order").default(0)

    override val primaryKey = PrimaryKey(filmTmdbId, personTmdbId)
}
