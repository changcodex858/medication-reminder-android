package com.example.medicationreminder.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.medicationreminder.domain.DoseStatus
import com.example.medicationreminder.domain.MedicationDraft
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MedicationRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MedicationRepository
    private val shanghai = ZoneId.of("Asia/Shanghai")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MedicationRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `claiming today's dose materializes tomorrow's dose`() = runTest {
        val dueAt = Instant.parse("2026-09-08T00:00:00Z")
        repository.saveMedication(draft())
        repository.ensureNextEvents(dueAt.minusSeconds(1), shanghai)

        assertEquals(dueAt, repository.nextWakeAt())
        val claimed = repository.claimDueEvents(dueAt)
        assertEquals(1, claimed.size)
        assertEquals(DoseStatus.RINGING, claimed.single().status)

        repository.ensureNextEvents(dueAt, shanghai)

        assertEquals(Instant.parse("2026-09-09T00:00:00Z"), repository.nextWakeAt())
    }

    @Test
    fun `snoozed dose wakes before the next scheduled dose`() = runTest {
        val dueAt = Instant.parse("2026-09-08T00:00:00Z")
        repository.saveMedication(draft())
        repository.ensureNextEvents(dueAt.minusSeconds(1), shanghai)
        val eventId = repository.claimDueEvents(dueAt).single().eventId

        repository.snooze(listOf(eventId), minutes = 10, now = dueAt)
        repository.ensureNextEvents(dueAt, shanghai)

        assertEquals(dueAt.plusSeconds(600), repository.nextWakeAt())
        assertEquals(DoseStatus.SNOOZED, repository.getOccurrences(listOf(eventId)).single().status)
    }

    @Test
    fun `editing a plan preserves the historical instruction snapshot`() = runTest {
        val dueAt = Instant.parse("2026-09-08T00:00:00Z")
        val medicationId = repository.saveMedication(draft())
        repository.ensureNextEvents(dueAt.minusSeconds(1), shanghai)
        val eventId = repository.claimDueEvents(dueAt).single().eventId
        repository.markTaken(listOf(eventId), dueAt.plusSeconds(30))

        repository.saveMedication(
            draft().copy(
                id = medicationId,
                name = "新药名",
                doseAmount = "2",
                instructions = "新的说明",
            )
        )
        repository.ensureNextEvents(dueAt, shanghai)

        val occurrences = repository.observeOccurrences(
            dueAt.minusSeconds(1),
            dueAt.plusSeconds(2 * 24 * 60 * 60L),
        ).first()
        val historical = occurrences.first { it.eventId == eventId }
        val future = occurrences.first { it.eventId != eventId }

        assertEquals("阿司匹林", historical.medicationName)
        assertEquals("1", historical.doseAmount)
        assertEquals("随餐服用", historical.instructions)
        assertEquals("新药名", future.medicationName)
        assertEquals("2", future.doseAmount)
        assertEquals("新的说明", future.instructions)
    }

    @Test
    fun `rebuilding pending events applies the new time zone`() = runTest {
        val now = Instant.parse("2026-09-08T00:00:00Z")
        repository.saveMedication(draft())
        repository.ensureNextEvents(now, shanghai)
        assertEquals(Instant.parse("2026-09-09T00:00:00Z"), repository.nextWakeAt())

        repository.ensureNextEvents(
            now,
            ZoneId.of("UTC"),
            rebuildPendingEvents = true,
        )

        assertEquals(Instant.parse("2026-09-08T08:00:00Z"), repository.nextWakeAt())
    }

    @Test
    fun `a stale action cannot overwrite an already taken dose`() = runTest {
        val dueAt = Instant.parse("2026-09-08T00:00:00Z")
        repository.saveMedication(draft())
        repository.ensureNextEvents(dueAt.minusSeconds(1), shanghai)
        val eventId = repository.claimDueEvents(dueAt).single().eventId

        assertTrue(repository.markTaken(listOf(eventId), dueAt.plusSeconds(5)))
        assertFalse(repository.snooze(listOf(eventId), minutes = 10, now = dueAt.plusSeconds(6)))
        assertFalse(repository.markSkipped(listOf(eventId), dueAt.plusSeconds(7)))

        assertEquals(DoseStatus.TAKEN, repository.getOccurrences(listOf(eventId)).single().status)
    }

    @Test
    fun `a missed dose can be corrected once and then remains terminal`() = runTest {
        val dueAt = Instant.parse("2026-09-08T00:00:00Z")
        repository.saveMedication(draft())
        repository.ensureNextEvents(dueAt.minusSeconds(1), shanghai)
        val eventId = repository.claimDueEvents(dueAt).single().eventId

        repository.claimDueEvents(dueAt.plusSeconds(2 * 60 * 60L + 1))
        assertEquals(DoseStatus.MISSED, repository.getOccurrences(listOf(eventId)).single().status)

        val correctedAt = dueAt.plusSeconds(2 * 60 * 60L + 5)
        assertFalse(repository.markTaken(listOf(eventId), correctedAt))
        assertTrue(repository.markTakenFromToday(listOf(eventId), correctedAt))
        assertFalse(repository.markTakenFromToday(listOf(eventId), correctedAt.plusSeconds(1)))
        assertFalse(repository.markSkippedFromToday(listOf(eventId), correctedAt.plusSeconds(2)))

        val corrected = repository.getOccurrences(listOf(eventId)).single()
        assertEquals(DoseStatus.TAKEN, corrected.status)
        assertEquals(correctedAt, corrected.actedAt)
    }

    @Test
    fun `a snoozed dose can be marked taken before it rings again`() = runTest {
        val dueAt = Instant.parse("2026-09-08T00:00:00Z")
        repository.saveMedication(draft())
        repository.ensureNextEvents(dueAt.minusSeconds(1), shanghai)
        val eventId = repository.claimDueEvents(dueAt).single().eventId

        assertTrue(repository.snooze(listOf(eventId), minutes = 10, now = dueAt))
        assertFalse(repository.markTaken(listOf(eventId), dueAt.plusSeconds(60)))
        assertTrue(repository.markTakenFromToday(listOf(eventId), dueAt.plusSeconds(60)))

        val occurrence = repository.getOccurrences(listOf(eventId)).single()
        assertEquals(DoseStatus.TAKEN, occurrence.status)
        assertEquals(null, occurrence.snoozedUntil)
    }

    @Test
    fun `a future pending dose cannot be resolved early`() = runTest {
        val dueAt = Instant.parse("2026-09-08T00:00:00Z")
        repository.saveMedication(draft())
        repository.ensureNextEvents(dueAt.minusSeconds(1), shanghai)
        val pending = repository.observeOccurrences(
            dueAt.minusSeconds(1),
            dueAt.plusSeconds(1),
        ).first().single()

        assertEquals(DoseStatus.PENDING, pending.status)
        assertFalse(repository.markTakenFromToday(listOf(pending.eventId), dueAt.minusSeconds(1)))
        assertFalse(repository.markSkippedFromToday(listOf(pending.eventId), dueAt.minusSeconds(1)))
        assertEquals(
            DoseStatus.PENDING,
            repository.getOccurrences(listOf(pending.eventId)).single().status,
        )
    }

    @Test
    fun `failed delivery is requeued instead of remaining ringing`() = runTest {
        val dueAt = Instant.parse("2026-09-08T00:00:00Z")
        repository.saveMedication(draft())
        repository.ensureNextEvents(dueAt.minusSeconds(1), shanghai)
        val eventId = repository.claimDueEvents(dueAt).single().eventId

        repository.retryDelivery(listOf(eventId), dueAt.plusSeconds(300))

        val retried = repository.getOccurrences(listOf(eventId)).single()
        assertEquals(DoseStatus.SNOOZED, retried.status)
        assertEquals(dueAt.plusSeconds(300), retried.snoozedUntil)
        assertEquals(dueAt.plusSeconds(300), repository.nextWakeAt())
    }

    @Test
    fun `a recently re-rung snoozed dose is not marked missed from its original time`() = runTest {
        val originalDueAt = Instant.parse("2026-09-08T00:00:00Z")
        val reRingAt = originalDueAt.plusSeconds(5 * 60 * 60L)
        repository.saveMedication(draft())
        repository.ensureNextEvents(originalDueAt.minusSeconds(1), shanghai)
        val eventId = repository.claimDueEvents(originalDueAt).single().eventId
        repository.snooze(
            listOf(eventId),
            minutes = 10,
            now = reRingAt.minusSeconds(10 * 60L),
        )

        assertEquals(eventId, repository.claimDueEvents(reRingAt).single().eventId)
        repository.claimDueEvents(reRingAt.plusSeconds(60 * 60L))

        assertEquals(DoseStatus.RINGING, repository.getOccurrences(listOf(eventId)).single().status)
    }

    private fun draft() = MedicationDraft(
        name = "阿司匹林",
        doseAmount = "1",
        doseUnit = "片",
        route = "口服",
        instructions = "随餐服用",
        foodRestrictions = "避免饮酒",
        startDate = LocalDate.of(2026, 9, 1),
        endDate = null,
        times = listOf(LocalTime.of(8, 0)),
        weekdays = DayOfWeek.entries.toSet(),
    )
}
