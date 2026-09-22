package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.Film
import me.jochum.filmqueuer.domain.FilmRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class MySqlFilmRepository : FilmRepository {
    // Insert-if-absent, matching FilmTable's own insertIgnore semantics: a film re-added via an
    // unrelated path (e.g. adding an already-known film to another queue) must never clobber
    // richer data (like directors) resolved elsewhere. Directors given here are added
    // additively — never removed — since an empty list here just means "caller doesn't know",
    // not "explicitly no directors".
    override suspend fun save(film: Film): Film =
        newSuspendedTransaction {
            // insertIgnore is a no-op on a (tmdb_id, tv) conflict - id is a freshly generated UUID
            // every time a Film is constructed, so it never collides on its own and can't be used
            // to detect "already exists" the way the old int tmdbId primary key could.
            FilmTable.insertIgnore {
                it[id] = film.id
                it[tmdbId] = film.tmdbId
                it[title] = film.title
                it[originalTitle] = film.originalTitle
                it[releaseDate] = film.releaseDate
                it[runtime] = film.runtime
                it[genres] = film.genres?.joinToString(", ")
                it[posterPath] = film.posterPath
                it[tv] = film.tv
                it[sortTitle] = film.sortTitle ?: Film.defaultSortTitle(film.title)
            }
            // Read back the authoritative stored row: on a conflict this is the pre-existing row
            // (its real id and any manually-corrected sortTitle must be preserved), and on a fresh
            // insert it's simply what was just written - either way this is what's actually stored.
            val stored =
                FilmTable.selectAll()
                    .where { (FilmTable.tmdbId eq film.tmdbId) and (FilmTable.tv eq film.tv) }
                    .single()
            val storedId = stored[FilmTable.id]
            insertDirectors(storedId, film.directorTmdbIds)
            film.copy(id = storedId, sortTitle = stored[FilmTable.sortTitle])
        }

    // Full replace, used when a caller is explicitly re-resolving a film (e.g. re-linking a
    // collection match) and wants the stored data — directors included — to reflect the latest
    // TMDB data exactly, not accumulate stale associations from a previous match. Keyed by id,
    // so callers must pass the film's existing id (e.g. from findByTmdbId), not a fresh one.
    override suspend fun update(film: Film): Boolean =
        newSuspendedTransaction {
            // Unlike insertIgnore, update() replaces every column - so an explicit-value-less
            // sortTitle must be preserved from the existing row (a manual correction) rather
            // than recomputed from the (possibly re-fetched) title, mirroring MySqlPersonRepository.
            val existingSortTitle =
                FilmTable.selectAll().where { FilmTable.id eq film.id }
                    .singleOrNull()
                    ?.get(FilmTable.sortTitle)
            val sortTitleToStore = film.sortTitle ?: existingSortTitle ?: Film.defaultSortTitle(film.title)
            val updateCount =
                FilmTable.update({ FilmTable.id eq film.id }) {
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
            FilmDirectorTable.deleteWhere { filmId eq film.id }
            insertDirectors(film.id, film.directorTmdbIds)
            updateCount > 0
        }

    override suspend fun updateSortTitle(
        id: UUID,
        sortTitle: String,
    ): Boolean =
        newSuspendedTransaction {
            FilmTable.update({ FilmTable.id eq id }) {
                it[FilmTable.sortTitle] = sortTitle
            } > 0
        }

    private fun insertDirectors(
        filmId: UUID,
        directorTmdbIds: List<Int>,
    ) {
        directorTmdbIds.forEachIndexed { index, personId ->
            FilmDirectorTable.insertIgnore {
                it[FilmDirectorTable.filmId] = filmId
                it[personTmdbId] = personId
                it[billingOrder] = index
            }
        }
    }

    override suspend fun findByTmdbId(
        tmdbId: Int,
        tv: Boolean,
    ): Film? =
        newSuspendedTransaction {
            val film =
                FilmTable.selectAll()
                    .where { (FilmTable.tmdbId eq tmdbId) and (FilmTable.tv eq tv) }
                    .singleOrNull()
                    ?.toFilm() ?: return@newSuspendedTransaction null

            film.copy(directorTmdbIds = directorsFor(film.id))
        }

    override suspend fun findById(id: UUID): Film? =
        newSuspendedTransaction {
            val film =
                FilmTable.selectAll()
                    .where { FilmTable.id eq id }
                    .singleOrNull()
                    ?.toFilm() ?: return@newSuspendedTransaction null

            film.copy(directorTmdbIds = directorsFor(film.id))
        }

    private fun directorsFor(filmId: UUID): List<Int> =
        FilmDirectorTable.selectAll()
            .where { FilmDirectorTable.filmId eq filmId }
            .orderBy(FilmDirectorTable.billingOrder to SortOrder.ASC)
            .map { it[FilmDirectorTable.personTmdbId] }

    override suspend fun findAll(): List<Film> =
        newSuspendedTransaction {
            val films = FilmTable.selectAll().map { it.toFilm() }
            val directorsByFilm =
                FilmDirectorTable.selectAll()
                    .orderBy(FilmDirectorTable.billingOrder to SortOrder.ASC)
                    .groupBy({ it[FilmDirectorTable.filmId] }, { it[FilmDirectorTable.personTmdbId] })

            films.map { film -> film.copy(directorTmdbIds = directorsByFilm[film.id] ?: emptyList()) }
        }

    private fun ResultRow.toFilm() =
        Film(
            id = this[FilmTable.id],
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
