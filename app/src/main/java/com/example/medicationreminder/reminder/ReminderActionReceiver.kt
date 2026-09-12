package com.example.medicationreminder.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicationreminder.appGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_TEST) {
            AlarmPlaybackService.stop(context)
            return
        }

        val ids = intent.getLongArrayExtra(EXTRA_EVENT_IDS)?.toList().orEmpty()
        if (ids.isEmpty()) return

        val pendingResult = goAsync()
        val graph = context.appGraph
        graph.applicationScope.launch {
            try {
                when (intent.action) {
                    ACTION_TAKEN -> graph.medicationRepository.markTaken(ids)
                    ACTION_SKIPPED -> graph.medicationRepository.markSkipped(ids)
                    ACTION_SNOOZE -> {
                        val fallback = graph.preferencesRepository.preferences.first().defaultSnoozeMinutes
                        val minutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, fallback)
                        graph.medicationRepository.snooze(ids, minutes)
                    }
                }
                try {
                    graph.alarmCoordinator.synchronize()
                } finally {
                    AlarmPlaybackService.refresh(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TAKEN = "com.example.medicationreminder.action.TAKEN"
        const val ACTION_SNOOZE = "com.example.medicationreminder.action.SNOOZE"
        const val ACTION_SKIPPED = "com.example.medicationreminder.action.SKIPPED"
        const val ACTION_STOP_TEST = "com.example.medicationreminder.action.STOP_TEST"
        const val EXTRA_EVENT_IDS = "event_ids"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }
}
