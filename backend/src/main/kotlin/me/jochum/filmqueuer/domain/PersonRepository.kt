package me.jochum.filmqueuer.domain

interface PersonRepository {
    suspend fun save(person: Person): Person

    suspend fun findByTmdbId(tmdbId: Int): Person?

    suspend fun findAll(): List<Person>

    suspend fun deleteByTmdbId(tmdbId: Int): Boolean
}
