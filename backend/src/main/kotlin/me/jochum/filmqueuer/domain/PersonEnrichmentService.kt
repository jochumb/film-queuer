package me.jochum.filmqueuer.domain

import kotlinx.coroutines.delay
import me.jochum.filmqueuer.adapters.tmdb.TmdbService

class PersonEnrichmentService(
    private val personRepository: PersonRepository,
    private val tmdbService: TmdbService,
) {
    /**
     * Enriches existing persons in the database with missing TMDB data
     * Currently focuses on missing image paths
     */
    suspend fun enrichPersonsWithMissingData(): PersonEnrichmentResult {
        val allPersons = personRepository.findAll()
        val personsToEnrich = allPersons.filter { it.imagePath.isNullOrBlank() }

        if (personsToEnrich.isEmpty()) {
            return PersonEnrichmentResult(
                totalPersons = allPersons.size,
                personsToEnrich = 0,
                enrichedPersons = 0,
                failedPersons = 0,
                message = "No persons need enrichment",
            )
        }

        println("Found ${personsToEnrich.size} persons that need image enrichment out of ${allPersons.size} total persons")

        var enrichedCount = 0
        var failedCount = 0

        personsToEnrich.forEachIndexed { index, person ->
            try {
                println("Enriching person ${index + 1}/${personsToEnrich.size}: ${person.name} (TMDB ID: ${person.tmdbId})")

                // Get person details from TMDB
                val tmdbPerson = tmdbService.getPersonDetails(person.tmdbId)

                // Update person with image path if available
                val imagePath = tmdbPerson.profilePath?.let { "https://image.tmdb.org/t/p/w200$it" }

                if (imagePath != null) {
                    val updatedPerson = person.copy(imagePath = imagePath)
                    personRepository.save(updatedPerson)
                    enrichedCount++
                    println("  ✓ Added image path: $imagePath")
                } else {
                    println("  ⚠ No profile image available on TMDB")
                }

                // Rate limiting - be respectful to TMDB API
                if (index < personsToEnrich.size - 1) {
                    delay(250) // 250ms delay between requests (4 requests per second max)
                }
            } catch (e: Exception) {
                failedCount++
                println("  ✗ Failed to enrich ${person.name}: ${e.message}")
            }
        }

        return PersonEnrichmentResult(
            totalPersons = allPersons.size,
            personsToEnrich = personsToEnrich.size,
            enrichedPersons = enrichedCount,
            failedPersons = failedCount,
            message = "Enrichment completed: $enrichedCount enriched, $failedCount failed",
        )
    }
}

data class PersonEnrichmentResult(
    val totalPersons: Int,
    val personsToEnrich: Int,
    val enrichedPersons: Int,
    val failedPersons: Int,
    val message: String,
)
