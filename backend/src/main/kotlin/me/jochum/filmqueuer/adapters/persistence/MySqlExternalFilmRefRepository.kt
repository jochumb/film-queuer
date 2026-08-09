package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.CollectionSortField
import me.jochum.filmqueuer.domain.ExternalFilmRef
import me.jochum.filmqueuer.domain.ExternalFilmRefRepository
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.coalesce
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.leftJoin
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class MySqlExternalFilmRefRepository : ExternalFilmRefRepository {
    override suspend fun save(ref: ExternalFilmRef): ExternalFilmRef =
        newSuspendedTransaction {
            ExternalFilmRefTable.insert {
                it[id] = ref.id
                it[sourceName] = ref.source
                it[title] = ref.title
                it[year] = ref.year
                it[filmTmdbId] = ref.filmTmdbId
                it[owned] = ref.owned
                it[watched] = ref.watched
                it[createdAt] = ref.createdAt
                it[removed] = ref.removed
            }
            ref
        }

    // Intentionally leaves `removed` untouched here - this update() is used for merging
    // owned/watched flags on re-import and for re-linking a match, neither of which should ever
    // be able to accidentally un-hide (or hide) an item. setRemoved() is the only way to change it.
    override suspend fun update(ref: ExternalFilmRef): Boolean =
        newSuspendedTransaction {
            ExternalFilmRefTable.update({ ExternalFilmRefTable.id eq ref.id }) {
                it[filmTmdbId] = ref.filmTmdbId
                it[owned] = ref.owned
                it[watched] = ref.watched
            } > 0
        }

    override suspend fun setRemoved(
        id: UUID,
        removed: Boolean,
    ): Boolean =
        newSuspendedTransaction {
            ExternalFilmRefTable.update({ ExternalFilmRefTable.id eq id }) {
                it[ExternalFilmRefTable.removed] = removed
            } > 0
        }

    override suspend fun findById(id: UUID): ExternalFilmRef? =
        newSuspendedTransaction {
            ExternalFilmRefTable.selectAll()
                .where { ExternalFilmRefTable.id eq id }
                .singleOrNull()
                ?.toExternalFilmRef()
        }

    override suspend fun findBySourceTitleYear(
        source: String,
        title: String,
        year: Int?,
    ): ExternalFilmRef? =
        newSuspendedTransaction {
            ExternalFilmRefTable.selectAll()
                .where {
                    val yearCondition = if (year == null) ExternalFilmRefTable.year.isNull() else ExternalFilmRefTable.year eq year
                    (ExternalFilmRefTable.sourceName eq source) and (ExternalFilmRefTable.title eq title) and yearCondition
                }
                .singleOrNull()
                ?.toExternalFilmRef()
        }

    override suspend fun findPage(
        owned: Boolean?,
        watched: Boolean?,
        unmatched: Boolean?,
        sortField: CollectionSortField,
        sortDescending: Boolean,
        offset: Int,
        limit: Int,
        removed: Boolean,
    ): List<ExternalFilmRef> =
        newSuspendedTransaction {
            val sortOrder = if (sortDescending) SortOrder.DESC else SortOrder.ASC
            val joined =
                ExternalFilmRefTable
                    .leftJoin(FilmTable, { filmTmdbId }, { tmdbId })
                    .leftJoin(
                        FilmDirectorTable,
                        { FilmTable.tmdbId },
                        { FilmDirectorTable.filmTmdbId },
                        { FilmDirectorTable.billingOrder eq 0 },
                    )
                    .leftJoin(PersonTable, { FilmDirectorTable.personTmdbId }, { PersonTable.tmdbId })

            val query = joined.selectAll().where { filterCondition(owned, watched, unmatched, removed) }
            when (sortField) {
                CollectionSortField.TITLE ->
                    query.orderBy(coalesce(FilmTable.sortTitle, ExternalFilmRefTable.title) to sortOrder)
                CollectionSortField.YEAR -> query.orderBy(ExternalFilmRefTable.year to sortOrder)
                // Director alone leaves same-director films in an arbitrary order; chaining
                // year then title as tiebreakers keeps a stable, predictable ordering within
                // a director's filmography instead of shuffling on every page load.
                CollectionSortField.DIRECTOR ->
                    query.orderBy(
                        PersonTable.sortName to sortOrder,
                        ExternalFilmRefTable.year to sortOrder,
                        coalesce(FilmTable.sortTitle, ExternalFilmRefTable.title) to sortOrder,
                    )
                CollectionSortField.ADDED -> query.orderBy(ExternalFilmRefTable.createdAt to sortOrder)
            }

            query.limit(limit, offset.toLong()).map { it.toExternalFilmRef() }
        }

    override suspend fun count(
        owned: Boolean?,
        watched: Boolean?,
        unmatched: Boolean?,
        removed: Boolean,
    ): Int =
        newSuspendedTransaction {
            ExternalFilmRefTable.selectAll()
                .where { filterCondition(owned, watched, unmatched, removed) }
                .count()
                .toInt()
        }

    private fun filterCondition(
        owned: Boolean?,
        watched: Boolean?,
        unmatched: Boolean?,
        removed: Boolean,
    ): Op<Boolean> {
        var condition: Op<Boolean> = ExternalFilmRefTable.removed eq removed
        if (owned != null) condition = condition and (ExternalFilmRefTable.owned eq owned)
        if (watched != null) condition = condition and (ExternalFilmRefTable.watched eq watched)
        if (unmatched != null) {
            condition =
                condition and
                if (unmatched) ExternalFilmRefTable.filmTmdbId.isNull() else ExternalFilmRefTable.filmTmdbId.isNotNull()
        }
        return condition
    }

    override suspend fun findByFilmTmdbId(tmdbId: Int): ExternalFilmRef? =
        newSuspendedTransaction {
            ExternalFilmRefTable.selectAll()
                .where { ExternalFilmRefTable.filmTmdbId eq tmdbId }
                .singleOrNull()
                ?.toExternalFilmRef()
        }

    private fun ResultRow.toExternalFilmRef() =
        ExternalFilmRef(
            id = this[ExternalFilmRefTable.id],
            source = this[ExternalFilmRefTable.sourceName],
            title = this[ExternalFilmRefTable.title],
            year = this[ExternalFilmRefTable.year],
            filmTmdbId = this[ExternalFilmRefTable.filmTmdbId],
            owned = this[ExternalFilmRefTable.owned],
            watched = this[ExternalFilmRefTable.watched],
            createdAt = this[ExternalFilmRefTable.createdAt],
            removed = this[ExternalFilmRefTable.removed],
        )
}
