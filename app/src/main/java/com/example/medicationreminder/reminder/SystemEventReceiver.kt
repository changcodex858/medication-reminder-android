package com.example.medicationreminder.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.medicationreminder.appGraph
import java.time.Instant
import kotlinx.coroutines.launch

class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val graph = context.appGraph
        graph.applicationScope.launch {
            try {
                val now = Instant.now()
                if (intent.action == Intent.ACTION_TIME_CHANGED) {
                    // If the clock jumped forward, retain and surface a dose that just
                    // became due instead of deleting it during recalculation.
                    val due = graph.medicationRepository.claimDueEvents(now)
                    try {
                        if (due.isNotEmpty()) {
                            ReminderDelivery.deliver(
                                context = context,
                                occurrences = due,
                                allowBackgroundVoice = true,
                            )
                        }
                    } finally {
                        graph.alarmCoordinator.synchronize(now)
                    }
                } else {
                    graph.alarmCoordinator.synchronize(
                        now = now,
                        rebuildPendingEvents = intent.action == Intent.ACTION_TIMEZONE_CHANGED,
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
