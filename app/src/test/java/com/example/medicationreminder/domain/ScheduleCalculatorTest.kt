package com.example.medicationreminder.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleCalculatorTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun `returns same day occurrence when time is still ahead`() {
        val rule = ScheduleRule(
            time = LocalTime.of(8, 0),
            weekdays = DayOfWeek.entries.toSet(),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = null,
        )

        val next = ScheduleCalculator.nextOccurrence(
            rule,
            Instant.parse("2026-09-07T23:30:00Z"),
            zone,
        )

        assertEquals(Instant.parse("2026-09-08T00:00:00Z"), next)
    }

    @Test
    fun `moves to next selected weekday after today's time passed`() {
        val rule = ScheduleRule(
            time = LocalTime.of(8, 0),
            weekdays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = null,
        )

        val next = ScheduleCalculator.nextOccurrence(
            rule,
            Instant.parse("2026-09-07T01:00:00Z"),
            zone,
        )

        assertEquals(Instant.parse("2026-09-09T00:00:00Z"), next)
    }

    @Test
    fun `occurrence is strictly after the supplied instant`() {
        val rule = ScheduleRule(
            time = LocalTime.of(8, 0),
            weekdays = DayOfWeek.entries.toSet(),
            startDate = LocalDate.of(2026, 9, 1),
            endDate = null,
        )

        val next = ScheduleCalculator.nextOccurrence(
            rule,
            Instant.parse("2026-09-08T00:00:00Z"),
            zone,
        )

        assertEquals(Instant.parse("2026-09-09T00:00:00Z"), next)
    }

    @Test
    fun `honors start and end date`() {
        val rule = ScheduleRule(
            time = LocalTime.NOON,
            weekdays = DayOfWeek.entries.toSet(),
            startDate = LocalDate.of(2026, 10, 1),
            endDate = LocalDate.of(2026, 10, 2),
        )

        val first = ScheduleCalculator.nextOccurrence(
            rule,
            Instant.parse("2026-09-08T00:00:00Z"),
            zone,
        )
        val afterWindow = ScheduleCalculator.nextOccurrence(
            rule,
            Instant.parse("2026-10-02T05:00:00Z"),
            zone,
        )

        assertEquals(Instant.parse("2026-10-01T04:00:00Z"), first)
        assertNull(afterWindow)
    }

    @Test
    fun `weekday mask round trips`() {
        val days = linkedSetOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
        assertEquals(days, days.toBitMask().toDaysOfWeek())
    }
}
