package me.jochum.filmqueuer.domain

import java.util.UUID

interface FilmRepository {
    suspend fun save(film: Film): Film

    suspend fun update(film: Film): Boolean

    suspend fun findByTmdbId(
        tmdbId: Int,
        tv: Boolean,
    ): Film?

    suspend fun findById(id: UUID): Film?

    suspend fun findAll(): List<Film>

    suspend fun updateSortTitle(
        id: UUID,
        sortTitle: String,
    ): Boolean
}
