package com.example.medicationreminder.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.medicationreminder.domain.Zodiac
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ZodiacPreferencesTest {
    @Test fun `covers persist independently without altering reminder settings`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = PreferencesRepository(context)
        repository.setDefaultSnoozeMinutes(15)
        repository.setZodiacCover(101, Zodiac.DRAGON)
        repository.setZodiacCover(102, Zodiac.RABBIT)
        val reloaded = PreferencesRepository(context).preferences.first()
        assertEquals(Zodiac.DRAGON, Zodiac.fromId(reloaded.zodiacCovers[101]))
        assertEquals(Zodiac.RABBIT, Zodiac.fromId(reloaded.zodiacCovers[102]))
        assertEquals(15, reloaded.defaultSnoozeMinutes)
        assertEquals(12, Zodiac.entries.size)
        assertEquals(Zodiac.RABBIT, Zodiac.fromId("invalid"))
    }
}
