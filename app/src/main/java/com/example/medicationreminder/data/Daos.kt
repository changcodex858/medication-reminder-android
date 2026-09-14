package com.example.medicationreminder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Transaction
    @Query("SELECT * FROM medications ORDER BY enabled DESC, name COLLATE NOCASE")
    fun observePlans(): Flow<List<MedicationWithSchedules>>

    @Transaction
    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getPlan(id: Long): MedicationWithSchedules?

    @Insert
    suspend fun insert(entity: MedicationEntity): Long

    @Update
    suspend fun update(entity: MedicationEntity)

    @Query("UPDATE medications SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}

@Dao
interface ScheduleDao {
    @Insert
    suspend fun insertAll(entities: List<ScheduleEntity>)

    @Query("DELETE FROM schedules WHERE medicationId = :medicationId")
    suspend fun deleteForMedication(medicationId: Long)

    @Query(
        """
        SELECT s.id AS scheduleId, s.medicationId, s.hour, s.minute,
               s.weekdaysMask, m.name AS medicationName, m.doseAmount,
               m.doseUnit, m.route, m.instructions, m.foodRestrictions,
               m.startEpochDay, m.endEpochDay
        FROM schedules s
        INNER JOIN medications m ON m.id = s.medicationId
        WHERE s.enabled = 1 AND m.enabled = 1
        """
    )
    suspend fun getActiveRows(): List<ActiveScheduleRow>
}

@Dao
interface DoseEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: DoseEventEntity): Long

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM dose_events
            WHERE scheduleId = :scheduleId AND status = 'PENDING'
        )
        """
    )
    suspend fun hasPendingEvent(scheduleId: Long): Boolean

    @Query(
        """
        SELECT MIN(
            CASE WHEN status = 'SNOOZED' THEN snoozedUntil ELSE scheduledAt END
        )
        FROM dose_events
        WHERE status IN ('PENDING', 'SNOOZED')
        """
    )
    suspend fun findNextWakeAt(): Long?

    @Query(
        """
        SELECT id FROM dose_events
        WHERE (status = 'PENDING' AND scheduledAt <= :dueBefore)
           OR (status = 'SNOOZED' AND snoozedUntil <= :dueBefore)
        ORDER BY scheduledAt
        """
    )
    suspend fun findDueIds(dueBefore: Long): List<Long>

    @Query(
        """
        UPDATE dose_events
        SET status = 'RINGING', ringStartedAt = :ringStartedAt, snoozedUntil = NULL
        WHERE id IN (:ids)
        """
    )
    suspend fun markRinging(ids: List<Long>, ringStartedAt: Long)

    @Query(
        """
        UPDATE dose_events
        SET status = 'SNOOZED', ringStartedAt = NULL, snoozedUntil = :retryAt
        WHERE id IN (:ids) AND status = 'RINGING'
        """
    )
    suspend fun requeueRinging(ids: List<Long>, retryAt: Long)

    @Query(
        """
        UPDATE dose_events
        SET status = 'MISSED', actedAt = :actedAt
        WHERE status IN ('PENDING', 'SNOOZED', 'RINGING')
          AND CASE
                WHEN status = 'RINGING' THEN COALESCE(ringStartedAt, scheduledAt)
                WHEN status = 'SNOOZED' THEN COALESCE(snoozedUntil, scheduledAt)
                ELSE scheduledAt
              END < :cutoff
        """
    )
    suspend fun markStaleMissed(cutoff: Long, actedAt: Long)

    @Query(
        """
        UPDATE dose_events
        SET status = :status, actedAt = :actedAt, snoozedUntil = :snoozedUntil
        WHERE id IN (:ids) AND status IN (:allowedStatuses)
        """
    )
    suspend fun updateStatus(
        ids: List<Long>,
        status: String,
        actedAt: Long?,
        snoozedUntil: Long?,
        allowedStatuses: List<String>,
    ): Int

    @Query("DELETE FROM dose_events WHERE medicationId = :medicationId AND status IN ('PENDING', 'SNOOZED')")
    suspend fun deleteFutureForMedication(medicationId: Long)

    @Query("DELETE FROM dose_events WHERE status = 'PENDING'")
    suspend fun deletePendingEvents()

    @Query("SELECT COUNT(*) FROM dose_events WHERE status = 'RINGING'")
    suspend fun countRinging(): Int

    @Query(
        """
        SELECT medicationId, MAX(actedAt) AS actedAt
        FROM dose_events
        WHERE medicationId IN (:medicationIds)
          AND status = 'TAKEN'
          AND actedAt IS NOT NULL
          AND actedAt < :beforeExclusive
        GROUP BY medicationId
        """
    )
    suspend fun findLastTakenByMedication(
        medicationIds: List<Long>,
        beforeExclusive: Long,
    ): List<MedicationLastTakenRow>

    @Query(
        """
        SELECT e.id AS eventId, e.scheduleId, e.medicationId,
               e.medicationName, e.doseAmount, e.doseUnit,
               e.route, e.instructions, e.foodRestrictions,
               e.scheduledAt, e.status, e.actedAt, e.snoozedUntil
        FROM dose_events e
        WHERE e.id IN (:ids)
        ORDER BY e.scheduledAt
        """
    )
    suspend fun getDetails(ids: List<Long>): List<DoseEventDetailsRow>

    @Query(
        """
        SELECT e.id AS eventId, e.scheduleId, e.medicationId,
               e.medicationName, e.doseAmount, e.doseUnit,
               e.route, e.instructions, e.foodRestrictions,
               e.scheduledAt, e.status, e.actedAt, e.snoozedUntil
        FROM dose_events e
        WHERE e.status = 'RINGING'
        ORDER BY e.scheduledAt
        """
    )
    suspend fun getRingingDetails(): List<DoseEventDetailsRow>

    @Query(
        """
        SELECT e.id AS eventId, e.scheduleId, e.medicationId,
               e.medicationName, e.doseAmount, e.doseUnit,
               e.route, e.instructions, e.foodRestrictions,
               e.scheduledAt, e.status, e.actedAt, e.snoozedUntil
        FROM dose_events e
        WHERE e.scheduledAt >= :fromInclusive AND e.scheduledAt < :toExclusive
        ORDER BY e.scheduledAt
        """
    )
    fun observeBetween(fromInclusive: Long, toExclusive: Long): Flow<List<DoseEventDetailsRow>>
}
