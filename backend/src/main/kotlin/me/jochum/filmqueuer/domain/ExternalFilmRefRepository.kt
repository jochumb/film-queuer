package me.jochum.filmqueuer.domain

import java.util.UUID

interface ExternalFilmRefRepository {
    suspend fun save(ref: ExternalFilmRef): ExternalFilmRef

    suspend fun update(ref: ExternalFilmRef): Boolean

    suspend fun findById(id: UUID): ExternalFilmRef?

    suspend fun findBySourceTitleYear(
        source: String,
        title: String,
        year: Int?,
    ): ExternalFilmRef?

    suspend fun findPage(
        owned: Boolean?,
        watched: Boolean?,
        unmatched: Boolean?,
        sortField: CollectionSortField,
        sortDescending: Boolean,
        offset: Int,
        limit: Int,
        removed: Boolean = false,
    ): List<ExternalFilmRef>

    suspend fun count(
        owned: Boolean?,
        watched: Boolean?,
        unmatched: Boolean?,
        removed: Boolean = false,
    ): Int

    suspend fun findByFilmTmdbId(tmdbId: Int): ExternalFilmRef?

    suspend fun setRemoved(
        id: UUID,
        removed: Boolean,
    ): Boolean
}
