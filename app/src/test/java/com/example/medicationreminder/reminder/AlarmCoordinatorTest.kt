package com.example.medicationreminder.reminder

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.medicationreminder.data.AppDatabase
import com.example.medicationreminder.data.MedicationRepository
import com.example.medicationreminder.domain.MedicationDraft
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AlarmCoordinatorTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: MedicationRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = MedicationRepository(database)
        ShadowAlarmManager.reset()
    }

    @After
    fun tearDown() {
        database.close()
        ShadowAlarmManager.reset()
    }

    @Test
    fun `exact access schedules an alarm clock that may start spoken delivery`() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        repository.saveMedication(draft())
        val coordinator = AlarmCoordinator(context, repository)

        coordinator.synchronize(Instant.parse("2026-09-08T00:00:00Z"))

        val scheduled = requireNotNull(
            shadowOf(context.getSystemService(AlarmManager::class.java))
                .peekNextScheduledAlarm()
        )
        assertNotNull(scheduled.showIntent)
        assertTrue(
            shadowOf(scheduled.operation).savedIntent
                .getBooleanExtra(AlarmReceiver.EXTRA_ALLOW_VOICE, false)
        )
    }

    @Test
    fun `without exact access schedule falls back without background voice`() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        repository.saveMedication(draft())
        val coordinator = AlarmCoordinator(context, repository)

        coordinator.synchronize(Instant.parse("2026-09-08T00:00:00Z"))

        val scheduled = requireNotNull(
            shadowOf(context.getSystemService(AlarmManager::class.java))
                .peekNextScheduledAlarm()
        )
        assertNull(scheduled.showIntent)
        assertFalse(
            shadowOf(scheduled.operation).savedIntent
                .getBooleanExtra(AlarmReceiver.EXTRA_ALLOW_VOICE, true)
        )
        assertFalse(coordinator.scheduleTest())
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
