package me.jochum.filmqueuer.adapters.tmdb

interface TmdbService {
    suspend fun searchPerson(query: String): TmdbPersonSearchResponse

    suspend fun searchMovies(query: String): TmdbMovieSearchResponse

    suspend fun searchTv(query: String): TmdbTvSearchResponse

    suspend fun getPersonMovieCredits(personId: Int): TmdbPersonCreditsResponse

    suspend fun getPersonDetails(personId: Int): TmdbPerson

    suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails

    suspend fun getTvDetails(tvId: Int): TmdbTvDetails

    suspend fun getTvSeasonDetails(
        tvId: Int,
        seasonNumber: Int,
    ): TmdbSeasonDetails
}
