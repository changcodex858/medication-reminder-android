package com.example.medicationreminder.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val doseAmount: String,
    val doseUnit: String,
    val route: String,
    val instructions: String,
    val foodRestrictions: String,
    val startEpochDay: Long,
    val endEpochDay: Long?,
    val enabled: Boolean = true,
)

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("medicationId")],
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val hour: Int,
    val minute: Int,
    val weekdaysMask: Int,
    val enabled: Boolean = true,
)

@Entity(
    tableName = "dose_events",
    indices = [
        Index(value = ["scheduleId", "scheduledAt"], unique = true),
        Index("medicationId"),
        Index("status"),
    ],
)
data class DoseEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long,
    val medicationId: Long,
    // Snapshot the instructions used for this occurrence. Editing a medication
    // later must not rewrite the historical dose or safety advice.
    val medicationName: String,
    val doseAmount: String,
    val doseUnit: String,
    val route: String,
    val instructions: String,
    val foodRestrictions: String,
    val scheduledAt: Long,
    val status: String,
    val actedAt: Long? = null,
    val snoozedUntil: Long? = null,
    val ringStartedAt: Long? = null,
)

data class MedicationWithSchedules(
    @Embedded val medication: MedicationEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicationId",
    )
    val schedules: List<ScheduleEntity>,
)

data class ActiveScheduleRow(
    val scheduleId: Long,
    val medicationId: Long,
    val hour: Int,
    val minute: Int,
    val weekdaysMask: Int,
    val medicationName: String,
    val doseAmount: String,
    val doseUnit: String,
    val route: String,
    val instructions: String,
    val foodRestrictions: String,
    val startEpochDay: Long,
    val endEpochDay: Long?,
)

data class DoseEventDetailsRow(
    val eventId: Long,
    val scheduleId: Long,
    val medicationId: Long,
    val medicationName: String,
    val doseAmount: String,
    val doseUnit: String,
    val route: String,
    val instructions: String,
    val foodRestrictions: String,
    val scheduledAt: Long,
    val status: String,
    val actedAt: Long?,
    val snoozedUntil: Long?,
)
