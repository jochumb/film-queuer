package me.jochum.filmqueuer.adapters.persistence

import me.jochum.filmqueuer.domain.Department
import me.jochum.filmqueuer.domain.Person
import me.jochum.filmqueuer.domain.PersonRepository
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class MySqlPersonRepository : PersonRepository {
    // Preserve any manually-corrected sortName across re-saves (e.g. a director re-resolved via
    // another film match): only a true first insert gets the computed default.
    override suspend fun save(person: Person): Person =
        newSuspendedTransaction {
            val existingSortName =
                PersonTable.selectAll().where { PersonTable.tmdbId eq person.tmdbId }
                    .singleOrNull()
                    ?.get(PersonTable.sortName)
            val sortNameToStore = person.sortName ?: existingSortName ?: Person.defaultSortName(person.name)
            PersonTable.replace {
                it[tmdbId] = person.tmdbId
                it[name] = person.name
                it[department] = person.department
                it[imagePath] = person.imagePath
                it[sortName] = sortNameToStore
            }
            person.copy(sortName = sortNameToStore)
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
                        sortName = row[PersonTable.sortName],
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
                    sortName = row[PersonTable.sortName],
                )
            }
        }

    override suspend fun deleteByTmdbId(tmdbId: Int): Boolean =
        newSuspendedTransaction {
            PersonTable.deleteWhere { PersonTable.tmdbId eq tmdbId } > 0
        }

    override suspend fun updateDepartment(
        tmdbId: Int,
        department: Department,
    ): Boolean =
        newSuspendedTransaction {
            val updateCount =
                PersonTable.update({ PersonTable.tmdbId eq tmdbId }) {
                    it[PersonTable.department] = department
                }
            updateCount > 0
        }

    override suspend fun updateSortName(
        tmdbId: Int,
        sortName: String,
    ): Boolean =
        newSuspendedTransaction {
            val updateCount =
                PersonTable.update({ PersonTable.tmdbId eq tmdbId }) {
                    it[PersonTable.sortName] = sortName
                }
            updateCount > 0
        }
}
