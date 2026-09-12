package com.example.medicationreminder.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.medicationreminder.MainActivity
import com.example.medicationreminder.data.MedicationRepository
import java.time.Instant
import kotlin.math.max
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AlarmCoordinator(
    private val context: Context,
    private val repository: MedicationRepository,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val synchronizationMutex = Mutex()

    suspend fun synchronize(
        now: Instant = Instant.now(),
        rebuildPendingEvents: Boolean = false,
    ) = synchronizationMutex.withLock {
        repository.ensureNextEvents(now, rebuildPendingEvents = rebuildPendingEvents)
        val nextWakeAt = repository.nextWakeAt()
        if (nextWakeAt == null) {
            alarmManager.cancel(alarmPendingIntent(AlarmReceiver.ACTION_DUE, DUE_REQUEST_CODE, false))
            null
        } else {
            schedule(
                triggerAtMillis = max(nextWakeAt.toEpochMilli(), now.toEpochMilli() + MIN_TRIGGER_DELAY_MS),
                action = AlarmReceiver.ACTION_DUE,
                requestCode = DUE_REQUEST_CODE,
            )
        }
    }

    fun scheduleTest(delaySeconds: Long = 30): Boolean {
        if (!canScheduleExactAlarms()) return false
        return runCatching {
            scheduleExact(
                triggerAtMillis = System.currentTimeMillis() + delaySeconds * 1000,
                pendingIntent = alarmPendingIntent(
                    action = AlarmReceiver.ACTION_TEST,
                    requestCode = TEST_REQUEST_CODE,
                    allowVoice = true,
                ),
            )
        }.isSuccess
    }

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun schedule(
        triggerAtMillis: Long,
        action: String,
        requestCode: Int,
    ): SchedulePrecision {
        if (canScheduleExactAlarms()) {
            try {
                scheduleExact(
                    triggerAtMillis,
                    alarmPendingIntent(action, requestCode, allowVoice = true),
                )
                return SchedulePrecision.EXACT_VOICE
            } catch (_: SecurityException) {
                // Permission can be revoked between the capability check and this call.
            }
        }
        // Inexact alarms are not allowed to start a foreground service from the
        // background on modern Android. The receiver will use an audible notification
        // fallback and never pretend this path can guarantee spoken audio.
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            alarmPendingIntent(action, requestCode, allowVoice = false),
        )
        return SchedulePrecision.BEST_EFFORT_NOTIFICATION
    }

    private fun scheduleExact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerAtMillis, alarmDetailsPendingIntent()),
            pendingIntent,
        )
    }

    private fun alarmPendingIntent(
        action: String,
        requestCode: Int,
        allowVoice: Boolean,
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_ALLOW_VOICE, allowVoice)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmDetailsPendingIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        DETAILS_REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val DUE_REQUEST_CODE = 7001
        const val TEST_REQUEST_CODE = 7002
        const val DETAILS_REQUEST_CODE = 7003
        const val MIN_TRIGGER_DELAY_MS = 500L
    }
}

enum class SchedulePrecision {
    EXACT_VOICE,
    BEST_EFFORT_NOTIFICATION,
}
