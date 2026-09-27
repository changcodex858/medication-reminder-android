package com.example.medicationreminder.reminder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.medicationreminder.appGraph
import com.example.medicationreminder.domain.DoseOccurrence
import com.example.medicationreminder.domain.DoseStatus
import com.example.medicationreminder.ui.theme.MedicationReminderTheme
import java.time.Instant

class ReminderActivity : ComponentActivity() {
    private var lockScreenTest by mutableStateOf(false)
    private val viewModel: ReminderViewModel by viewModels {
        ReminderViewModel.Factory(
            eventIds(), appGraph.medicationRepository, appGraph.preferencesRepository,
            appGraph.alarmCoordinator, applicationContext,
        )
    }

    private fun eventIds() = intent.getLongArrayExtra(AlarmPlaybackService.EXTRA_EVENT_IDS)?.toList().orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockScreenTest = intent.getBooleanExtra(EXTRA_LOCK_SCREEN_TEST, false)
        showAboveLockScreen()
        setContent {
            MedicationReminderTheme(darkTheme = true) {
                if (lockScreenTest) {
                    var submitted by remember { mutableStateOf(false) }
                    val completeTest: (Boolean) -> Unit = { taken ->
                        if (!submitted) {
                            submitted = true
                            AlarmPlaybackService.confirmAction(this,
                                if (taken) "上滑确认测试成功。这是演练，没有记录真实用药。"
                                else "下滑稍后提醒测试成功。真实用药将按设置延后提醒。")
                            finish()
                        }
                    }
                    LockScreenReminderScreen(
                        state = ReminderUiState(loading = false, occurrences = listOf(demoOccurrence())),
                        isTest = true,
                        onTaken = { completeTest(true) },
                        onSnooze = { completeTest(false) },
                        onSkipped = { AlarmPlaybackService.stop(this); finish() },
                    )
                } else {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    LaunchedEffect(state.completed) {
                        if (state.completed) finish()
                    }
                    LockScreenReminderScreen(state, false,
                        viewModel::markTaken, viewModel::snooze, viewModel::markSkipped)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        lockScreenTest = intent.getBooleanExtra(EXTRA_LOCK_SCREEN_TEST, false)
        if (!lockScreenTest) viewModel.updateEventIds(eventIds())
    }

    private fun showAboveLockScreen() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        // Intentionally leave the keyguard locked; closing this task returns to it.
    }

    companion object {
        const val EXTRA_LOCK_SCREEN_TEST = "lock_screen_test"

        internal fun demoOccurrence() = DoseOccurrence(
            eventId = -1, scheduleId = -1, medicationId = -1,
            medicationName = "锁屏滑动演练", doseAmount = "", doseUnit = "",
            route = "", instructions = "仅测试唤醒屏幕和滑动操作，不记录真实用药。",
            foodRestrictions = "", scheduledAt = Instant.now(),
            status = DoseStatus.RINGING, actedAt = null, snoozedUntil = null,
        )
    }
}
