package me.jochum.filmqueuer.adapters.tmdb

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class TmdbClient : TmdbService {
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
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
    
    fun close() {
        httpClient.close()
    }
}