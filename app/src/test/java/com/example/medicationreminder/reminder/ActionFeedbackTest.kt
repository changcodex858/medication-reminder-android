package com.example.medicationreminder.reminder

import android.app.Application
import android.app.Service
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.test.core.app.ApplicationProvider
import com.example.medicationreminder.MedicationReminderApplication
import java.time.Duration
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = MedicationReminderApplication::class)
class ActionFeedbackTest {
    @Test fun `confirmation speaks once without an activity and resumes normal reminder handling`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        AlarmPlaybackService.confirmAction(context, "好的，已记录本次已经服用。")
        val intent = shadowOf(context).nextStartedService
        val controller = Robolectric.buildService(AlarmPlaybackService::class.java).create()
        try {
            val result = controller.get().onStartCommand(intent, 0, 1)
            assertEquals(Service.START_NOT_STICKY, result)
            awaitTts()
            ShadowTextToSpeech.addLanguageAvailability(Locale.SIMPLIFIED_CHINESE)
            val tts = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())
            tts.onInitListener.onInit(TextToSpeech.SUCCESS)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(900))
            assertEquals("好的，已记录本次已经服用。", tts.lastSpokenText)
            tts.utteranceProgressListener.onDone("medication-reminder")
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(2))
            assertEquals(1, tts.spokenTextList.size)
            val refresh = shadowOf(context).nextStartedService
            assertNotNull(refresh)
            assertFalse(refresh.hasExtra("action_feedback"))
        } finally { controller.destroy() }
    }

    private fun awaitTts() {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (ShadowTextToSpeech.getLastTextToSpeechInstance() == null && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertNotNull(ShadowTextToSpeech.getLastTextToSpeechInstance())
    }
}
