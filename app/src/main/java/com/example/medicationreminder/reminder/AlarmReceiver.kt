package com.example.medicationreminder.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicationreminder.appGraph
import java.time.Instant
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TEST) {
            val pendingResult = goAsync()
            val graph = context.appGraph
            graph.applicationScope.launch {
                try {
                    // A diagnostic must never replace a real medication alarm.
                    if (graph.medicationRepository.hasRingingEvents()) {
                        AlarmPlaybackService.refresh(context)
                    } else if (intent.getBooleanExtra(EXTRA_ALLOW_VOICE, false)) {
                        AlarmPlaybackService.startTest(context, lockScreen = true)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (intent.action != ACTION_DUE) return
        val pendingResult = goAsync()
        val graph = context.appGraph
        graph.applicationScope.launch {
            try {
                val due = graph.medicationRepository.claimDueEvents(Instant.now())
                try {
                    if (due.isNotEmpty()) {
                        ReminderDelivery.deliver(
                            context = context,
                            occurrences = due,
                            allowBackgroundVoice = intent.getBooleanExtra(EXTRA_ALLOW_VOICE, false),
                        )
                    }
                } finally {
                    graph.alarmCoordinator.synchronize()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DUE = "com.example.medicationreminder.action.DUE"
        const val ACTION_TEST = "com.example.medicationreminder.action.TEST"
        const val EXTRA_ALLOW_VOICE = "allow_background_voice"
    }
}
