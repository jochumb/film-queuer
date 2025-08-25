package me.jochum.filmqueuer.adapters.web

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun String?.toLocalDate(): LocalDate? {
    return this?.let { dateString ->
        try {
            LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: DateTimeParseException) {
            null
        }
    }
}

fun LocalDate?.toDateString(): String? {
    return this?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
}
