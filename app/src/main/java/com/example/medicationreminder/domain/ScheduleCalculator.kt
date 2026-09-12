package com.example.medicationreminder.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class ScheduleRule(
    val time: LocalTime,
    val weekdays: Set<DayOfWeek>,
    val startDate: LocalDate,
    val endDate: LocalDate?,
)

object ScheduleCalculator {
    fun nextOccurrence(
        rule: ScheduleRule,
        afterExclusive: Instant,
        zoneId: ZoneId,
    ): Instant? {
        val after = afterExclusive.atZone(zoneId)
        var date = maxOf(after.toLocalDate(), rule.startDate)
        val selectedDays = rule.weekdays.ifEmpty { DayOfWeek.entries.toSet() }

        repeat(MAX_SEARCH_DAYS) {
            if (rule.endDate != null && date.isAfter(rule.endDate)) return null

            if (date.dayOfWeek in selectedDays) {
                val candidate = date.atTime(rule.time).atZone(zoneId).toInstant()
                if (candidate.isAfter(afterExclusive)) return candidate
            }
            date = date.plusDays(1)
        }
        return null
    }

    private const val MAX_SEARCH_DAYS = 3660
}

fun Set<DayOfWeek>.toBitMask(): Int = fold(0) { mask, day ->
    mask or (1 shl (day.value - 1))
}

fun Int.toDaysOfWeek(): Set<DayOfWeek> = DayOfWeek.entries
    .filterTo(linkedSetOf()) { day -> this and (1 shl (day.value - 1)) != 0 }

