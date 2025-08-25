package me.jochum.filmqueuer.domain

interface FilmRepository {
    suspend fun save(film: Film): Film

    suspend fun findByTmdbId(tmdbId: Int): Film?

    suspend fun findAll(): List<Film>
}
