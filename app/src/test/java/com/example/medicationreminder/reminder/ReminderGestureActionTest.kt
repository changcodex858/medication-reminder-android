package com.example.medicationreminder.reminder

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.medicationreminder.data.AppDatabase
import com.example.medicationreminder.data.MedicationRepository
import com.example.medicationreminder.data.PreferencesRepository
import com.example.medicationreminder.domain.*
import java.time.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ReminderGestureActionTest {
    @Test fun `taken is persisted before one voice confirmation is requested`() = exercise(false)
    @Test fun `snooze persists selected minutes and speaks the same duration`() = exercise(true)

    private fun exercise(snooze: Boolean) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val context = ApplicationProvider.getApplicationContext<Application>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repository = MedicationRepository(database)
            val preferences = PreferencesRepository(context)
            preferences.setDefaultSnoozeMinutes(15)
            val now = Instant.now().minusSeconds(60)
            val local = now.atZone(ZoneId.systemDefault())
            repository.saveMedication(MedicationDraft(
                name = "测试药品", doseAmount = "1", doseUnit = "片", route = "口服",
                instructions = "测试", foodRestrictions = "", startDate = local.toLocalDate(), endDate = null,
                times = listOf(local.toLocalTime().withSecond(0).withNano(0)), weekdays = DayOfWeek.entries.toSet(),
            ))
            repository.ensureNextEvents(now.minusSeconds(120))
            val id = repository.claimDueEvents(Instant.now()).single().eventId
            val model = ReminderViewModel(listOf(id), repository, preferences, AlarmCoordinator(context, repository), context)
            model.state.first { !it.loading }
            if (snooze) { model.snooze(id); model.snooze(id) }
            else { model.markTaken(id); model.markTaken(id) }
            model.state.first { it.completed }
            val event = repository.getOccurrences(listOf(id)).single()
            assertEquals(if (snooze) DoseStatus.SNOOZED else DoseStatus.TAKEN, event.status)
            val intent = shadowOf(context).nextStartedService
            val message = intent.getStringExtra("action_feedback").orEmpty()
            assertTrue(if (snooze) "15分钟后" in message else "测试药品本次已经服用" in message)
            if (snooze) assertTrue(Duration.between(Instant.now(), event.snoozedUntil).seconds in 890..900)
            assertNull(shadowOf(context).nextStartedService)
        } finally {
            database.close()
            Dispatchers.resetMain()
        }
    }
}
