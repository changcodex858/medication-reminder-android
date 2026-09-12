package com.example.medicationreminder.reminder

import com.example.medicationreminder.domain.DoseOccurrence
import com.example.medicationreminder.domain.DoseStatus
import com.example.medicationreminder.domain.ReminderVoiceStyle
import com.example.medicationreminder.domain.UserPreferences
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSpeechComposerTest {
    @Test
    fun `default playful speech retains every medication safety detail`() {
        val speech = ReminderSpeechComposer.compose(
            listOf(
                occurrence(
                    medicationName = "阿司匹林",
                    doseAmount = "一",
                    doseUnit = "片",
                    route = "口服",
                    instructions = "随餐服用",
                    foodRestrictions = "不要饮酒",
                )
            )
        )

        assertTrue(speech.startsWith("嗨呀，小小提醒来啦。"))
        assertTrue("阿司匹林" in speech)
        assertTrue("一 片" in speech)
        assertTrue("口服" in speech)
        assertTrue("随餐服用" in speech)
        assertTrue("不要饮酒" in speech)
        assertTrue(speech.endsWith("现在不方便的话，就让我稍后再来提醒你。"))
        assertFalse(speech.startsWith("现在是用药时间"))
    }

    @Test
    fun `default voice profile is soft and slightly higher pitched`() {
        val preferences = UserPreferences()

        assertEquals(0.82f, preferences.speechRate)
        assertEquals(1.12f, preferences.speechPitch)
        assertEquals(0.78f, preferences.speechVolume)
        assertEquals(60, preferences.repeatIntervalSeconds)
        assertEquals(ReminderVoiceStyle.PLAYFUL, preferences.voiceStyle)
    }

    @Test
    fun `every preset voice style retains medication safety details`() {
        val item = occurrence(
            medicationName = "阿司匹林",
            doseAmount = "一",
            doseUnit = "片",
            route = "口服",
            instructions = "随餐服用",
            foodRestrictions = "不要饮酒",
        )

        ReminderVoiceStyle.entries.filterNot { it == ReminderVoiceStyle.CUSTOM }.forEach { style ->
            val speech = ReminderSpeechComposer.compose(listOf(item), style)
            assertTrue("$style should include medication name", "阿司匹林" in speech)
            assertTrue("$style should include dose", "一 片" in speech)
            assertTrue("$style should include route", "口服" in speech)
            assertTrue("$style should include instructions", "随餐服用" in speech)
            assertTrue("$style should include restrictions", "不要饮酒" in speech)
        }
    }

    @Test
    fun `voice presets have distinct safe preview messages`() {
        val presets = ReminderVoiceStyle.entries.filterNot { it == ReminderVoiceStyle.CUSTOM }
        val messages = presets.map(ReminderSpeechComposer::testMessage)

        assertEquals(presets.size, messages.distinct().size)
        messages.forEach { message ->
            assertFalse("威胁" in message)
            assertFalse("危险" in message)
        }
    }

    private fun occurrence(
        medicationName: String,
        doseAmount: String,
        doseUnit: String,
        route: String,
        instructions: String,
        foodRestrictions: String,
    ) = DoseOccurrence(
        eventId = 1,
        scheduleId = 2,
        medicationId = 3,
        medicationName = medicationName,
        doseAmount = doseAmount,
        doseUnit = doseUnit,
        route = route,
        instructions = instructions,
        foodRestrictions = foodRestrictions,
        scheduledAt = Instant.EPOCH,
        status = DoseStatus.RINGING,
        actedAt = null,
        snoozedUntil = null,
    )
}
