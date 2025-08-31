package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.Film
import me.jochum.filmqueuer.domain.FilmRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class MySqlFilmRepository : FilmRepository {
    override suspend fun save(film: Film): Film =
        newSuspendedTransaction {
            FilmTable.replace {
                it[tmdbId] = film.tmdbId
                it[title] = film.title
                it[originalTitle] = film.originalTitle
                it[releaseDate] = film.releaseDate
                it[runtime] = film.runtime
                it[genres] = film.genres
                it[posterPath] = film.posterPath
            }
            film
        }

    override suspend fun findByTmdbId(tmdbId: Int): Film? =
        newSuspendedTransaction {
            FilmTable.selectAll()
                .where { FilmTable.tmdbId eq tmdbId }
                .singleOrNull()
                ?.toFilm()
        }

    override suspend fun findAll(): List<Film> =
        newSuspendedTransaction {
            FilmTable.selectAll().map { it.toFilm() }
        }

    private fun ResultRow.toFilm() =
        Film(
            tmdbId = this[FilmTable.tmdbId],
            title = this[FilmTable.title],
            originalTitle = this[FilmTable.originalTitle],
            releaseDate = this[FilmTable.releaseDate],
            runtime = this[FilmTable.runtime],
            genres = this[FilmTable.genres],
            posterPath = this[FilmTable.posterPath],
        )
}
