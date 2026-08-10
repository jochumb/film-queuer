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

    /**
     * Picks `count` random matched, non-removed refs from the full set matching the given
     * filters (e.g. owned + not watched + a max runtime), for a "what should I watch" homepage
     * widget. Always excludes unmatched rows, since there'd be no film data to show.
     */
    suspend fun findRandomPicks(
        owned: Boolean?,
        watched: Boolean?,
        maxRuntime: Int?,
        count: Int,
    ): List<ExternalFilmRef>
}
