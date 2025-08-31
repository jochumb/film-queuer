package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.FilmEnrichmentService

object FilmEnrichmentUtility {
    suspend fun enrichFilmsIfRequested(filmEnrichmentService: FilmEnrichmentService) {
        val enrichmentMode = System.getenv("ENRICH_FILMS")?.uppercase()

        when (enrichmentMode) {
            "TRUE", "YES", "1" -> {
                println("🔄 Film enrichment requested via ENRICH_FILMS environment variable")
                println("Starting film enrichment process...")

                try {
                    val result = filmEnrichmentService.enrichFilmsWithMissingData()

                    println("📊 Film Enrichment Results:")
                    println("   Total films in database: ${result.totalFilms}")
                    println("   Films needing enrichment: ${result.filmsToEnrich}")
                    println("   Successfully enriched: ${result.enrichedFilms}")
                    println("   Failed to enrich: ${result.failedFilms}")
                    println("   ${result.message}")

                    if (result.enrichedFilms > 0) {
                        println("✅ Film enrichment completed successfully!")
                    } else if (result.filmsToEnrich == 0) {
                        println("ℹ️  All films already have complete data")
                    } else {
                        println("⚠️  Some films could not be enriched - check TMDB API availability")
                    }
                } catch (e: Exception) {
                    println("❌ Film enrichment failed: ${e.message}")
                    e.printStackTrace()
                }
            }
            null -> {
                // No enrichment requested
            }
            else -> {
                println("ℹ️  To enable film enrichment, set ENRICH_FILMS=true in your environment")
            }
        }
    }
}
