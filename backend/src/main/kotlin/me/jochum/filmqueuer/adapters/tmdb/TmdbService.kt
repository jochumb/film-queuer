package me.jochum.filmqueuer.adapters.tmdb

interface TmdbService {
    suspend fun searchPerson(query: String): TmdbPersonSearchResponse
}