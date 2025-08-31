package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.Person
import me.jochum.filmqueuer.domain.PersonRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class MySqlPersonRepository : PersonRepository {
    override suspend fun save(person: Person): Person =
        newSuspendedTransaction {
            PersonTable.replace {
                it[tmdbId] = person.tmdbId
                it[name] = person.name
                it[department] = person.department
                it[imagePath] = person.imagePath
            }
            person
        }

    override suspend fun findByTmdbId(tmdbId: Int): Person? =
        newSuspendedTransaction {
            PersonTable.selectAll().where { PersonTable.tmdbId eq tmdbId }
                .singleOrNull()
                ?.let { row ->
                    Person(
                        tmdbId = row[PersonTable.tmdbId],
                        name = row[PersonTable.name],
                        department = row[PersonTable.department],
                        imagePath = row[PersonTable.imagePath],
                    )
                }
        }

    override suspend fun findAll(): List<Person> =
        newSuspendedTransaction {
            PersonTable.selectAll().map { row ->
                Person(
                    tmdbId = row[PersonTable.tmdbId],
                    name = row[PersonTable.name],
                    department = row[PersonTable.department],
                    imagePath = row[PersonTable.imagePath],
                )
            }
        }

    override suspend fun deleteByTmdbId(tmdbId: Int): Boolean =
        newSuspendedTransaction {
            PersonTable.deleteWhere { PersonTable.tmdbId eq tmdbId } > 0
        }
}
