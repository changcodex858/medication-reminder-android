package com.example.medicationreminder.data

import androidx.room.withTransaction
import com.example.medicationreminder.domain.DoseOccurrence
import com.example.medicationreminder.domain.DoseStatus
import com.example.medicationreminder.domain.MedicationDraft
import com.example.medicationreminder.domain.MedicationPlan
import com.example.medicationreminder.domain.MedicationSchedule
import com.example.medicationreminder.domain.ScheduleCalculator
import com.example.medicationreminder.domain.ScheduleRule
import com.example.medicationreminder.domain.toBitMask
import com.example.medicationreminder.domain.toDaysOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicationRepository(private val database: AppDatabase) {
    private val medicationDao = database.medicationDao()
    private val scheduleDao = database.scheduleDao()
    private val eventDao = database.doseEventDao()

    fun observePlans(): Flow<List<MedicationPlan>> = medicationDao.observePlans().map { rows ->
        rows.map { it.toDomain() }
    }

    suspend fun getPlan(id: Long): MedicationPlan? = medicationDao.getPlan(id)?.toDomain()

    fun observeOccurrences(from: Instant, to: Instant): Flow<List<DoseOccurrence>> =
        eventDao.observeBetween(from.toEpochMilli(), to.toEpochMilli()).map { rows ->
            rows.map { it.toDomain() }
        }

    suspend fun saveMedication(draft: MedicationDraft): Long = database.withTransaction {
        require(draft.name.isNotBlank()) { "药品名称不能为空" }
        require(draft.doseAmount.isNotBlank()) { "每次药量不能为空" }
        require(draft.times.isNotEmpty()) { "至少需要一个提醒时间" }
        require(draft.weekdays.isNotEmpty()) { "至少需要选择一个星期" }
        require(draft.endDate == null || !draft.endDate.isBefore(draft.startDate)) {
            "结束日期不能早于开始日期"
        }
        require(draft.name.length <= MAX_NAME_LENGTH) { "药品名称不能超过 $MAX_NAME_LENGTH 个字符" }
        require(draft.doseAmount.length <= MAX_DOSE_LENGTH) { "每次药量不能超过 $MAX_DOSE_LENGTH 个字符" }
        require(draft.doseUnit.length <= MAX_UNIT_LENGTH) { "剂量单位不能超过 $MAX_UNIT_LENGTH 个字符" }
        require(draft.route.length <= MAX_SHORT_NOTE_LENGTH) { "服用方式不能超过 $MAX_SHORT_NOTE_LENGTH 个字符" }
        require(draft.instructions.length <= MAX_NOTE_LENGTH) { "重要提醒不能超过 $MAX_NOTE_LENGTH 个字符" }
        require(draft.foodRestrictions.length <= MAX_NOTE_LENGTH) { "忌口提醒不能超过 $MAX_NOTE_LENGTH 个字符" }

        val entity = MedicationEntity(
            id = draft.id ?: 0,
            name = draft.name.trim(),
            doseAmount = draft.doseAmount.trim(),
            doseUnit = draft.doseUnit.trim(),
            route = draft.route.trim(),
            instructions = draft.instructions.trim(),
            foodRestrictions = draft.foodRestrictions.trim(),
            startEpochDay = draft.startDate.toEpochDay(),
            endEpochDay = draft.endDate?.toEpochDay(),
            enabled = true,
        )

        val medicationId = if (draft.id == null) {
            medicationDao.insert(entity)
        } else {
            eventDao.deleteFutureForMedication(draft.id)
            scheduleDao.deleteForMedication(draft.id)
            medicationDao.update(entity)
            draft.id
        }

        val mask = draft.weekdays.toBitMask()
        scheduleDao.insertAll(
            draft.times.distinct().sorted().map { time ->
                ScheduleEntity(
                    medicationId = medicationId,
                    hour = time.hour,
                    minute = time.minute,
                    weekdaysMask = mask,
                )
            }
        )
        medicationId
    }

    suspend fun archiveMedication(id: Long) = database.withTransaction {
        medicationDao.setEnabled(id, false)
        eventDao.deleteFutureForMedication(id)
    }

    suspend fun ensureNextEvents(
        now: Instant,
        zoneId: ZoneId = ZoneId.systemDefault(),
        rebuildPendingEvents: Boolean = false,
    ) {
        database.withTransaction {
            if (rebuildPendingEvents) eventDao.deletePendingEvents()
            scheduleDao.getActiveRows().forEach { row ->
                if (!eventDao.hasPendingEvent(row.scheduleId)) {
                    val next = ScheduleCalculator.nextOccurrence(
                        rule = ScheduleRule(
                            time = LocalTime.of(row.hour, row.minute),
                            weekdays = row.weekdaysMask.toDaysOfWeek(),
                            startDate = LocalDate.ofEpochDay(row.startEpochDay),
                            endDate = row.endEpochDay?.let(LocalDate::ofEpochDay),
                        ),
                        // The event currently ringing may have the same schedule and timestamp.
                        // Always materialize the next strictly-future occurrence so the unique
                        // schedule/timestamp key cannot suppress tomorrow's reminder.
                        afterExclusive = now,
                        zoneId = zoneId,
                    )
                    if (next != null) {
                        eventDao.insertIgnore(
                            DoseEventEntity(
                                scheduleId = row.scheduleId,
                                medicationId = row.medicationId,
                                medicationName = row.medicationName,
                                doseAmount = row.doseAmount,
                                doseUnit = row.doseUnit,
                                route = row.route,
                                instructions = row.instructions,
                                foodRestrictions = row.foodRestrictions,
                                scheduledAt = next.toEpochMilli(),
                                status = DoseStatus.PENDING.name,
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun nextWakeAt(): Instant? = eventDao.findNextWakeAt()?.let(Instant::ofEpochMilli)

    suspend fun claimDueEvents(now: Instant): List<DoseOccurrence> = database.withTransaction {
        eventDao.markStaleMissed(
            cutoff = now.minusSeconds(MISSED_AFTER_SECONDS).toEpochMilli(),
            actedAt = now.toEpochMilli(),
        )
        val ids = eventDao.findDueIds(now.plusSeconds(DUE_TOLERANCE_SECONDS).toEpochMilli())
        if (ids.isEmpty()) return@withTransaction emptyList()
        eventDao.markRinging(ids, now.toEpochMilli())
        eventDao.getDetails(ids).map { it.toDomain() }
    }

    suspend fun getOccurrences(ids: List<Long>): List<DoseOccurrence> =
        if (ids.isEmpty()) emptyList() else eventDao.getDetails(ids).map { it.toDomain() }

    suspend fun getRingingOccurrences(): List<DoseOccurrence> =
        eventDao.getRingingDetails().map { it.toDomain() }

    suspend fun retryDelivery(ids: List<Long>, retryAt: Instant) {
        if (ids.isNotEmpty()) eventDao.requeueRinging(ids, retryAt.toEpochMilli())
    }

    suspend fun markTaken(ids: List<Long>, now: Instant = Instant.now()): Boolean = updateDoseStatus(
        ids = ids,
        status = DoseStatus.TAKEN,
        actedAt = now,
        snoozedUntil = null,
        allowedStatuses = RINGING_STATUS_NAMES,
    )

    suspend fun markTakenFromToday(ids: List<Long>, now: Instant = Instant.now()): Boolean =
        updateDoseStatus(
            ids = ids,
            status = DoseStatus.TAKEN,
            actedAt = now,
            snoozedUntil = null,
            allowedStatuses = TODAY_RESOLVABLE_STATUS_NAMES,
        )

    suspend fun markSkipped(ids: List<Long>, now: Instant = Instant.now()): Boolean = updateDoseStatus(
        ids = ids,
        status = DoseStatus.SKIPPED,
        actedAt = now,
        snoozedUntil = null,
        allowedStatuses = RINGING_STATUS_NAMES,
    )

    suspend fun markSkippedFromToday(ids: List<Long>, now: Instant = Instant.now()): Boolean =
        updateDoseStatus(
            ids = ids,
            status = DoseStatus.SKIPPED,
            actedAt = now,
            snoozedUntil = null,
            allowedStatuses = TODAY_RESOLVABLE_STATUS_NAMES,
        )

    suspend fun snooze(ids: List<Long>, minutes: Int, now: Instant = Instant.now()): Boolean =
        updateDoseStatus(
            ids = ids,
            status = DoseStatus.SNOOZED,
            actedAt = null,
            snoozedUntil = now.plusSeconds(minutes * 60L),
            allowedStatuses = RINGING_STATUS_NAMES,
        )

    suspend fun snoozeFromToday(
        ids: List<Long>,
        minutes: Int,
        now: Instant = Instant.now(),
    ): Boolean = updateDoseStatus(
        ids = ids,
        status = DoseStatus.SNOOZED,
        actedAt = null,
        snoozedUntil = now.plusSeconds(minutes * 60L),
        allowedStatuses = TODAY_RESOLVABLE_STATUS_NAMES,
    )

    private suspend fun updateDoseStatus(
        ids: List<Long>,
        status: DoseStatus,
        actedAt: Instant?,
        snoozedUntil: Instant?,
        allowedStatuses: List<String>,
    ): Boolean = ids.isNotEmpty() && eventDao.updateStatus(
        ids = ids,
        status = status.name,
        actedAt = actedAt?.toEpochMilli(),
        snoozedUntil = snoozedUntil?.toEpochMilli(),
        allowedStatuses = allowedStatuses,
    ) > 0

    suspend fun hasRingingEvents(): Boolean = eventDao.countRinging() > 0

    private fun MedicationWithSchedules.toDomain() = MedicationPlan(
        id = medication.id,
        name = medication.name,
        doseAmount = medication.doseAmount,
        doseUnit = medication.doseUnit,
        route = medication.route,
        instructions = medication.instructions,
        foodRestrictions = medication.foodRestrictions,
        startDate = LocalDate.ofEpochDay(medication.startEpochDay),
        endDate = medication.endEpochDay?.let(LocalDate::ofEpochDay),
        enabled = medication.enabled,
        schedules = schedules.sortedWith(compareBy(ScheduleEntity::hour, ScheduleEntity::minute)).map { schedule ->
            MedicationSchedule(
                id = schedule.id,
                time = LocalTime.of(schedule.hour, schedule.minute),
                weekdays = schedule.weekdaysMask.toDaysOfWeek(),
                enabled = schedule.enabled,
            )
        },
    )

    private fun DoseEventDetailsRow.toDomain() = DoseOccurrence(
        eventId = eventId,
        scheduleId = scheduleId,
        medicationId = medicationId,
        medicationName = medicationName,
        doseAmount = doseAmount,
        doseUnit = doseUnit,
        route = route,
        instructions = instructions,
        foodRestrictions = foodRestrictions,
        scheduledAt = Instant.ofEpochMilli(scheduledAt),
        status = DoseStatus.valueOf(status),
        actedAt = actedAt?.let(Instant::ofEpochMilli),
        snoozedUntil = snoozedUntil?.let(Instant::ofEpochMilli),
    )

    companion object {
        private const val DUE_TOLERANCE_SECONDS = 30L
        private const val MISSED_AFTER_SECONDS = 2 * 60 * 60L
        private const val MAX_NAME_LENGTH = 120
        private const val MAX_DOSE_LENGTH = 60
        private val RINGING_STATUS_NAMES = listOf(DoseStatus.RINGING.name)
        private val TODAY_RESOLVABLE_STATUS_NAMES = listOf(
            DoseStatus.RINGING.name,
            DoseStatus.SNOOZED.name,
            DoseStatus.MISSED.name,
        )
        private const val MAX_UNIT_LENGTH = 40
        private const val MAX_SHORT_NOTE_LENGTH = 160
        private const val MAX_NOTE_LENGTH = 600
    }
}
