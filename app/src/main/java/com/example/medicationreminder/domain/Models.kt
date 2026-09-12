package com.example.medicationreminder.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class DoseStatus {
    PENDING,
    RINGING,
    SNOOZED,
    TAKEN,
    SKIPPED,
    MISSED,
}

enum class ReminderVoiceStyle(
    val storageId: String,
    val speechRate: Float,
    val speechPitch: Float,
    val speechVolume: Float,
) {
    GENTLE("gentle", 0.80f, 1.04f, 0.74f),
    PLAYFUL("playful", 0.82f, 1.12f, 0.78f),
    FIRM("firm", 0.94f, 0.96f, 0.90f),
    CLINICAL("clinical", 0.90f, 1.00f, 0.84f),
    CUSTOM("custom", 0.82f, 1.12f, 0.78f),
    ;

    companion object {
        val default: ReminderVoiceStyle = PLAYFUL

        fun fromStorageId(value: String?): ReminderVoiceStyle? =
            entries.firstOrNull { it.storageId == value }
    }
}

data class MedicationSchedule(
    val id: Long,
    val time: LocalTime,
    val weekdays: Set<DayOfWeek>,
    val enabled: Boolean,
)

data class MedicationPlan(
    val id: Long,
    val name: String,
    val doseAmount: String,
    val doseUnit: String,
    val route: String,
    val instructions: String,
    val foodRestrictions: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val enabled: Boolean,
    val schedules: List<MedicationSchedule>,
) {
    val doseLabel: String
        get() = listOf(doseAmount, doseUnit).filter(String::isNotBlank).joinToString(" ")
}

data class MedicationDraft(
    val id: Long? = null,
    val name: String,
    val doseAmount: String,
    val doseUnit: String,
    val route: String,
    val instructions: String,
    val foodRestrictions: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val times: List<LocalTime>,
    val weekdays: Set<DayOfWeek>,
)

data class DoseOccurrence(
    val eventId: Long,
    val scheduleId: Long,
    val medicationId: Long,
    val medicationName: String,
    val doseAmount: String,
    val doseUnit: String,
    val route: String,
    val instructions: String,
    val foodRestrictions: String,
    val scheduledAt: Instant,
    val status: DoseStatus,
    val actedAt: Instant?,
    val snoozedUntil: Instant?,
) {
    val doseLabel: String
        get() = listOf(doseAmount, doseUnit).filter(String::isNotBlank).joinToString(" ")
}

data class UserPreferences(
    val defaultSnoozeMinutes: Int = 10,
    val repeatIntervalSeconds: Int = 60,
    val voiceStyle: ReminderVoiceStyle = ReminderVoiceStyle.default,
    val speechRate: Float = ReminderVoiceStyle.default.speechRate,
    val speechPitch: Float = ReminderVoiceStyle.default.speechPitch,
    val speechVolume: Float = ReminderVoiceStyle.default.speechVolume,
)
