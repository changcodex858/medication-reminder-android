package com.example.medicationreminder

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.medicationreminder.data.AppDatabase
import com.example.medicationreminder.data.MedicationRepository
import com.example.medicationreminder.data.PreferencesRepository
import com.example.medicationreminder.reminder.AlarmCoordinator
import com.example.medicationreminder.reminder.AlarmPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MedicationReminderApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "medication-reminder.db").build()
    }

    val medicationRepository: MedicationRepository by lazy { MedicationRepository(database) }
    val preferencesRepository: PreferencesRepository by lazy { PreferencesRepository(this) }
    val alarmCoordinator: AlarmCoordinator by lazy { AlarmCoordinator(this, medicationRepository) }

    override fun onCreate() {
        super.onCreate()
        AlarmPlaybackService.createNotificationChannel(this)
        applicationScope.launch { alarmCoordinator.synchronize() }
    }
}

val Context.appGraph: MedicationReminderApplication
    get() = applicationContext as MedicationReminderApplication

