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

    override suspend fun getPersonMovieCredits(personId: Int): TmdbPersonCreditsResponse {
        return httpClient.get("$baseUrl/person/$personId/movie_credits") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }.body()
    }

    fun close() {
        httpClient.close()
    }
}
