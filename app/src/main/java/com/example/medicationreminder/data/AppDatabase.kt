package com.example.medicationreminder.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MedicationEntity::class, ScheduleEntity::class, DoseEventEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun doseEventDao(): DoseEventDao
}

