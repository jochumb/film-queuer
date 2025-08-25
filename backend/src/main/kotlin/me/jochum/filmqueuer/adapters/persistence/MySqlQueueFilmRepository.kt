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
import java.time.LocalDateTime
import java.util.UUID

class MySqlQueueFilmRepository : QueueFilmRepository {
    override suspend fun addFilmToQueue(
        queueId: UUID,
        filmTmdbId: Int,
    ): QueueFilm =
        newSuspendedTransaction {
            val addedAt = LocalDateTime.now()
            QueueFilmTable.insert {
                it[QueueFilmTable.queueId] = queueId
                it[QueueFilmTable.filmTmdbId] = filmTmdbId
                it[QueueFilmTable.addedAt] = addedAt
            }
            QueueFilm(queueId, filmTmdbId, addedAt)
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
                .orderBy(QueueFilmTable.addedAt)
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

    private fun ResultRow.toFilm() =
        Film(
            tmdbId = this[FilmTable.tmdbId],
            title = this[FilmTable.title],
            originalTitle = this[FilmTable.originalTitle],
            releaseDate = this[FilmTable.releaseDate],
        )
}
