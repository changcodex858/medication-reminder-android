package com.example.medicationreminder.reminder

import android.app.Application
import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.medicationreminder.MedicationReminderApplication
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = MedicationReminderApplication::class)
class LockScreenNotificationTest {
    @Test fun `new alarms alert even if an earlier service notification is showing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val arriving = ReminderNotifications.reminder(context, longArrayOf(42), listOf("测试药"), "提醒", alert = true)
        val updating = ReminderNotifications.reminder(context, longArrayOf(42), listOf("测试药"), "更新")
        assertNotNull(arriving.fullScreenIntent)
        assertEquals(0, arriving.flags and Notification.FLAG_ONLY_ALERT_ONCE)
        assertTrue(updating.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)
        assertNotNull(updating.fullScreenIntent)
        val intent = shadowOf(arriving.fullScreenIntent).savedIntent
        assertEquals(ReminderActivity::class.java.name, intent.component?.className)
        assertArrayEquals(longArrayOf(42), intent.getLongArrayExtra(AlarmPlaybackService.EXTRA_EVENT_IDS))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test fun `scheduled lock screen test opens alarm surface but voice preview does not`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduled = ReminderNotifications.test(context, "测试", lockScreen = true, alert = true)
        assertNotNull(scheduled.fullScreenIntent)
        assertTrue(shadowOf(scheduled.fullScreenIntent).savedIntent.getBooleanExtra(ReminderActivity.EXTRA_LOCK_SCREEN_TEST, false))
        assertNull(ReminderNotifications.test(context, "试听").fullScreenIntent)
    }

    @Test fun `alarm surface can show and wake over keyguard without unlocking it`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        shadowOf(keyguard).setKeyguardLocked(true)
        val intent = Intent(context, ReminderActivity::class.java).putExtra(ReminderActivity.EXTRA_LOCK_SCREEN_TEST, true)
        val controller = Robolectric.buildActivity(ReminderActivity::class.java, intent).setup()
        try {
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(shadowOf(controller.get()).showWhenLocked)
            assertTrue(shadowOf(controller.get()).turnScreenOn)
            assertTrue(keyguard.isKeyguardLocked)
            controller.get().finish()
            assertTrue(keyguard.isKeyguardLocked)
        } finally { controller.pause().stop().destroy() }
    }
}
