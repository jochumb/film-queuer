package me.jochum.filmqueuer.domain

import java.util.UUID

data class PersonSelectionResult(
    val person: Person,
    val queue: PersonQueue,
)

class PersonSelectionService(
    private val personRepository: PersonRepository,
    private val queueRepository: QueueRepository,
) {
    suspend fun selectPerson(
        tmdbId: Int,
        name: String,
        department: Department,
    ): PersonSelectionResult {
        val person =
            Person(
                tmdbId = tmdbId,
                name = name,
                department = department,
            )

        val savedPerson = personRepository.save(person)

        val personQueue =
            PersonQueue(
                id = UUID.randomUUID(),
                personTmdbId = person.tmdbId,
            )

        val savedQueue = queueRepository.save(personQueue)

        return PersonSelectionResult(
            person = savedPerson,
            queue = savedQueue as PersonQueue,
        )
    }
}
