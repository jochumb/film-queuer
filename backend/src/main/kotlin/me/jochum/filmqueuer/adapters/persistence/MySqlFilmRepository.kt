package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.Film
import me.jochum.filmqueuer.domain.FilmRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class MySqlFilmRepository : FilmRepository {
    // Insert-if-absent, matching FilmTable's own insertIgnore semantics: a film re-added via an
    // unrelated path (e.g. adding an already-known film to another queue) must never clobber
    // richer data (like directors) resolved elsewhere. Directors given here are added
    // additively — never removed — since an empty list here just means "caller doesn't know",
    // not "explicitly no directors".
    override suspend fun save(film: Film): Film =
        newSuspendedTransaction {
            // insertIgnore is a no-op if the row already exists, so an existing sortTitle
            // (default or manually corrected) is never clobbered here - only a genuinely new
            // row gets the freshly computed default. Look it up first so the returned Film
            // accurately reflects what's actually stored either way.
            val existingSortTitle =
                FilmTable.selectAll().where { FilmTable.tmdbId eq film.tmdbId }
                    .singleOrNull()
                    ?.get(FilmTable.sortTitle)
            val sortTitleToStore = existingSortTitle ?: film.sortTitle ?: Film.defaultSortTitle(film.title)
            FilmTable.insertIgnore {
                it[tmdbId] = film.tmdbId
                it[title] = film.title
                it[originalTitle] = film.originalTitle
                it[releaseDate] = film.releaseDate
                it[runtime] = film.runtime
                it[genres] = film.genres?.joinToString(", ")
                it[posterPath] = film.posterPath
                it[tv] = film.tv
                it[sortTitle] = sortTitleToStore
            }
            insertDirectors(film.tmdbId, film.directorTmdbIds)
            film.copy(sortTitle = sortTitleToStore)
        }

    // Full replace, used when a caller is explicitly re-resolving a film (e.g. re-linking a
    // collection match) and wants the stored data — directors included — to reflect the latest
    // TMDB data exactly, not accumulate stale associations from a previous match.
    override suspend fun update(film: Film): Boolean =
        newSuspendedTransaction {
            // Unlike insertIgnore, update() replaces every column - so an explicit-value-less
            // sortTitle must be preserved from the existing row (a manual correction) rather
            // than recomputed from the (possibly re-fetched) title, mirroring MySqlPersonRepository.
            val existingSortTitle =
                FilmTable.selectAll().where { FilmTable.tmdbId eq film.tmdbId }
                    .singleOrNull()
                    ?.get(FilmTable.sortTitle)
            val sortTitleToStore = film.sortTitle ?: existingSortTitle ?: Film.defaultSortTitle(film.title)
            val updateCount =
                FilmTable.update({ FilmTable.tmdbId eq film.tmdbId }) {
                    it[title] = film.title
                    it[originalTitle] = film.originalTitle
                    it[releaseDate] = film.releaseDate
                    it[runtime] = film.runtime
                    it[genres] = film.genres?.joinToString(", ")
                    it[posterPath] = film.posterPath
                    it[tv] = film.tv
                    it[sortTitle] = sortTitleToStore
                }
            FilmDirectorTable.deleteWhere { filmTmdbId eq film.tmdbId }
            insertDirectors(film.tmdbId, film.directorTmdbIds)
            updateCount > 0
        }

    override suspend fun updateSortTitle(
        tmdbId: Int,
        sortTitle: String,
    ): Boolean =
        newSuspendedTransaction {
            FilmTable.update({ FilmTable.tmdbId eq tmdbId }) {
                it[FilmTable.sortTitle] = sortTitle
            } > 0
        }

    private fun insertDirectors(
        filmTmdbId: Int,
        directorTmdbIds: List<Int>,
    ) {
        directorTmdbIds.forEachIndexed { index, personId ->
            FilmDirectorTable.insertIgnore {
                it[FilmDirectorTable.filmTmdbId] = filmTmdbId
                it[personTmdbId] = personId
                it[billingOrder] = index
            }
        }
    }

    override suspend fun findByTmdbId(tmdbId: Int): Film? =
        newSuspendedTransaction {
            val film =
                FilmTable.selectAll()
                    .where { FilmTable.tmdbId eq tmdbId }
                    .singleOrNull()
                    ?.toFilm() ?: return@newSuspendedTransaction null

            val directorIds =
                FilmDirectorTable.selectAll()
                    .where { FilmDirectorTable.filmTmdbId eq tmdbId }
                    .orderBy(FilmDirectorTable.billingOrder to SortOrder.ASC)
                    .map { it[FilmDirectorTable.personTmdbId] }

            film.copy(directorTmdbIds = directorIds)
        }

    override suspend fun findAll(): List<Film> =
        newSuspendedTransaction {
            val films = FilmTable.selectAll().map { it.toFilm() }
            val directorsByFilm =
                FilmDirectorTable.selectAll()
                    .orderBy(FilmDirectorTable.billingOrder to SortOrder.ASC)
                    .groupBy({ it[FilmDirectorTable.filmTmdbId] }, { it[FilmDirectorTable.personTmdbId] })

            films.map { film -> film.copy(directorTmdbIds = directorsByFilm[film.tmdbId] ?: emptyList()) }
        }

    private fun ResultRow.toFilm() =
        Film(
            tmdbId = this[FilmTable.tmdbId],
            title = this[FilmTable.title],
            originalTitle = this[FilmTable.originalTitle],
            releaseDate = this[FilmTable.releaseDate],
            runtime = this[FilmTable.runtime],
            genres = this[FilmTable.genres]?.split(", ")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() },
            posterPath = this[FilmTable.posterPath],
            tv = this[FilmTable.tv],
            sortTitle = this[FilmTable.sortTitle],
        )
}
