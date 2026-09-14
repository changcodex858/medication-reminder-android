package com.example.medicationreminder.reminder

import android.content.Context
import android.util.Log
import com.example.medicationreminder.appGraph
import com.example.medicationreminder.domain.DoseOccurrence
import java.time.Instant
import kotlinx.coroutines.flow.first

object ReminderDelivery {
    suspend fun deliver(
        context: Context,
        occurrences: List<DoseOccurrence>,
        allowBackgroundVoice: Boolean,
    ): Boolean {
        if (occurrences.isEmpty()) return true

        val graph = context.appGraph
        val remindedAt = Instant.now()
        val style = graph.preferencesRepository.preferences.first().voiceStyle
        val lastTakenAtByMedication = graph.medicationRepository.lastTakenAtByMedication(
            medicationIds = occurrences.map { it.medicationId },
            before = remindedAt,
        )
        val message = ReminderSpeechComposer.compose(
            items = occurrences,
            style = style,
            remindedAt = remindedAt,
            lastTakenAtByMedication = lastTakenAtByMedication,
        )
        if (allowBackgroundVoice) {
            try {
                AlarmPlaybackService.start(context, occurrences, message)
                return true
            } catch (error: SecurityException) {
                Log.e(TAG, "Spoken reminder was blocked; using notification fallback", error)
            } catch (error: RuntimeException) {
                Log.e(TAG, "Unable to start spoken reminder; using notification fallback", error)
            }
        }

        val notificationShown = runCatching {
            ReminderNotifications.showFallback(context, occurrences, message)
        }.onFailure { error ->
            Log.e(TAG, "Unable to show fallback reminder", error)
        }.getOrDefault(false)

        if (!notificationShown) {
            // Never strand claimed events in RINGING when neither speech nor the
            // fallback notification can reach the user. Retry on the next alarm.
            graph.medicationRepository.retryDelivery(
                ids = occurrences.map { it.eventId },
                retryAt = Instant.now().plusSeconds(RETRY_DELAY_SECONDS),
            )
        }
        return notificationShown
    }

    private const val TAG = "ReminderDelivery"
    private const val RETRY_DELAY_SECONDS = 5 * 60L
}
