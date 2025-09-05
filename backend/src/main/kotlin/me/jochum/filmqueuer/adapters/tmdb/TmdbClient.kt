package me.jochum.filmqueuer.adapters.tmdb

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class TmdbClient : TmdbService {
    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                    },
                )
            }
        }

    private val apiKey = System.getenv("TMDB_API_KEY") ?: ""
    private val baseUrl = "https://api.themoviedb.org/3"

    override suspend fun searchPerson(query: String): TmdbPersonSearchResponse {
        return httpClient.get("$baseUrl/search/person") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            parameter("query", query)
        }.body()
    }

    override suspend fun searchMovies(query: String): TmdbMovieSearchResponse {
        return httpClient.get("$baseUrl/search/movie") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            parameter("query", query)
        }.body()
    }

    override suspend fun searchTv(query: String): TmdbTvSearchResponse {
        return httpClient.get("$baseUrl/search/tv") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            parameter("query", query)
        }.body()
    }

    override suspend fun getPersonMovieCredits(personId: Int): TmdbPersonCreditsResponse {
        return httpClient.get("$baseUrl/person/$personId/movie_credits") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
    }

    override suspend fun getPersonDetails(personId: Int): TmdbPerson {
        return httpClient.get("$baseUrl/person/$personId") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
    }

    override suspend fun getMovieDetails(movieId: Int): TmdbMovieDetails {
        return httpClient.get("$baseUrl/movie/$movieId") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
    }

    override suspend fun getTvDetails(tvId: Int): TmdbTvDetails {
        return httpClient.get("$baseUrl/tv/$tvId") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
    }

    override suspend fun getTvSeasonDetails(
        tvId: Int,
        seasonNumber: Int,
    ): TmdbSeasonDetails {
        return httpClient.get("$baseUrl/tv/$tvId/season/$seasonNumber") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
    }

    fun close() {
        httpClient.close()
    }
}
