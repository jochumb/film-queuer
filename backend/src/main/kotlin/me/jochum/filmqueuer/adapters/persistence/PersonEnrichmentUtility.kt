package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.PersonEnrichmentService

object PersonEnrichmentUtility {
    suspend fun enrichPersonsIfRequested(personEnrichmentService: PersonEnrichmentService) {
        val enrichmentMode = System.getenv("ENRICH_PERSONS")?.uppercase()

        when (enrichmentMode) {
            "TRUE", "YES", "1" -> {
                println("🔄 Person enrichment requested via ENRICH_PERSONS environment variable")
                println("Starting person enrichment process...")

                try {
                    val result = personEnrichmentService.enrichPersonsWithMissingData()

                    println("📊 Person Enrichment Results:")
                    println("   Total persons in database: ${result.totalPersons}")
                    println("   Persons needing enrichment: ${result.personsToEnrich}")
                    println("   Successfully enriched: ${result.enrichedPersons}")
                    println("   Failed to enrich: ${result.failedPersons}")
                    println("   ${result.message}")

                    if (result.enrichedPersons > 0) {
                        println("✅ Person enrichment completed successfully!")
                    } else if (result.personsToEnrich == 0) {
                        println("ℹ️  All persons already have complete data")
                    } else {
                        println("⚠️  Some persons could not be enriched - check TMDB API availability")
                    }
                } catch (e: Exception) {
                    println("❌ Person enrichment failed: ${e.message}")
                    e.printStackTrace()
                }
            }
            null -> {
                // No enrichment requested
            }
            else -> {
                println("ℹ️  To enable person enrichment, set ENRICH_PERSONS=true in your environment")
            }
        }
    }
}
