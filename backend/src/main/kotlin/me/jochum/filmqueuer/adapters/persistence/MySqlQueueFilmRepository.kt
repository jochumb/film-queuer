package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.Film
import me.jochum.filmqueuer.domain.QueueFilm
import me.jochum.filmqueuer.domain.QueueFilmRepository
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class MySqlQueueFilmRepository : QueueFilmRepository {
    override suspend fun addFilmToQueue(
        queueId: UUID,
        filmTmdbId: Int,
    ): QueueFilm =
        newSuspendedTransaction {
            val addedAt = Instant.now()
            // Get the next sort order (max + 1)
            val nextSortOrder =
                QueueFilmTable.selectAll()
                    .where { QueueFilmTable.queueId eq queueId }
                    .maxOfOrNull { it[QueueFilmTable.sortOrder] + 1 } ?: 0

            QueueFilmTable.insert {
                it[QueueFilmTable.queueId] = queueId
                it[QueueFilmTable.filmTmdbId] = filmTmdbId
                it[QueueFilmTable.addedAt] = addedAt
                it[QueueFilmTable.sortOrder] = nextSortOrder
            }
            QueueFilm(queueId, filmTmdbId, addedAt, nextSortOrder)
        }

    override suspend fun removeFilmFromQueue(
        queueId: UUID,
        filmTmdbId: Int,
    ): Boolean =
        newSuspendedTransaction {
            QueueFilmTable.deleteWhere {
                (QueueFilmTable.queueId eq queueId) and (QueueFilmTable.filmTmdbId eq filmTmdbId)
            } > 0
        }

    override suspend fun findFilmsByQueueId(queueId: UUID): List<Film> =
        newSuspendedTransaction {
            (QueueFilmTable innerJoin FilmTable)
                .selectAll()
                .where { QueueFilmTable.queueId eq queueId }
                .orderBy(QueueFilmTable.sortOrder)
                .map { it.toFilm() }
        }

    override suspend fun isFilmInQueue(
        queueId: UUID,
        filmTmdbId: Int,
    ): Boolean =
        newSuspendedTransaction {
            QueueFilmTable.selectAll()
                .where { (QueueFilmTable.queueId eq queueId) and (QueueFilmTable.filmTmdbId eq filmTmdbId) }
                .count() > 0
        }

    override suspend fun reorderQueueFilms(
        queueId: UUID,
        filmOrder: List<Int>,
    ): Boolean =
        newSuspendedTransaction {
            // Update sort order for each film in the provided order
            filmOrder.forEachIndexed { index, filmTmdbId ->
                QueueFilmTable.update({
                    (QueueFilmTable.queueId eq queueId) and (QueueFilmTable.filmTmdbId eq filmTmdbId)
                }) {
                    it[sortOrder] = index
                }
            }
            true
        }

    private fun ResultRow.toFilm() =
        Film(
            tmdbId = this[FilmTable.tmdbId],
            title = this[FilmTable.title],
            originalTitle = this[FilmTable.originalTitle],
            releaseDate = this[FilmTable.releaseDate],
        )
}
