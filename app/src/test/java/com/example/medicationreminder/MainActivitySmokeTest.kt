package com.example.medicationreminder

import android.os.Looper
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = MedicationReminderApplication::class)
class MainActivitySmokeTest {
    @Test
    fun `main activity starts without crashing`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()

        assertFalse(controller.get().isFinishing)

        controller.pause().stop().destroy()
    }
}
