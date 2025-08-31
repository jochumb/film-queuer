package me.jochum.filmqueuer.adapters.tmdb

interface TmdbService {
    suspend fun searchPerson(query: String): TmdbPersonSearchResponse

    suspend fun getPersonMovieCredits(personId: Int): TmdbPersonCreditsResponse

    suspend fun getPersonDetails(personId: Int): TmdbPerson
}
