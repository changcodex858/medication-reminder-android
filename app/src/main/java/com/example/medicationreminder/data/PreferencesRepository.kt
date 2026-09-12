package com.example.medicationreminder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.medicationreminder.domain.ReminderVoiceStyle
import com.example.medicationreminder.domain.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.medicationPreferences by preferencesDataStore(name = "medication_preferences")

class PreferencesRepository(private val context: Context) {
    private val defaults = UserPreferences()

    val preferences: Flow<UserPreferences> = context.medicationPreferences.data.map { values ->
        val storedStyle = ReminderVoiceStyle.fromStorageId(values[VOICE_STYLE])
        val hasLegacyVoiceValues = SPEECH_RATE in values ||
            SPEECH_PITCH in values || SPEECH_VOLUME in values
        val style = storedStyle ?: if (hasLegacyVoiceValues) {
            ReminderVoiceStyle.CUSTOM
        } else {
            ReminderVoiceStyle.default
        }
        UserPreferences(
            defaultSnoozeMinutes = values[SNOOZE_MINUTES] ?: defaults.defaultSnoozeMinutes,
            repeatIntervalSeconds = values[REPEAT_SECONDS] ?: defaults.repeatIntervalSeconds,
            voiceStyle = style,
            speechRate = values[SPEECH_RATE] ?: style.speechRate,
            speechPitch = values[SPEECH_PITCH] ?: style.speechPitch,
            speechVolume = values[SPEECH_VOLUME] ?: style.speechVolume,
        )
    }

    suspend fun setDefaultSnoozeMinutes(value: Int) {
        context.medicationPreferences.edit { it[SNOOZE_MINUTES] = value.coerceIn(5, 60) }
    }

    suspend fun setRepeatIntervalSeconds(value: Int) {
        context.medicationPreferences.edit { it[REPEAT_SECONDS] = value.coerceIn(30, 300) }
    }

    suspend fun setVoiceStyle(style: ReminderVoiceStyle) {
        require(style != ReminderVoiceStyle.CUSTOM) { "请选择一个预设语音风格" }
        context.medicationPreferences.edit {
            it[VOICE_STYLE] = style.storageId
            it[SPEECH_RATE] = style.speechRate
            it[SPEECH_PITCH] = style.speechPitch
            it[SPEECH_VOLUME] = style.speechVolume
        }
    }

    suspend fun setSpeechRate(value: Float) {
        context.medicationPreferences.edit {
            it[VOICE_STYLE] = ReminderVoiceStyle.CUSTOM.storageId
            it[SPEECH_RATE] = value.coerceIn(0.5f, 1.5f)
        }
    }

    suspend fun setSpeechPitch(value: Float) {
        context.medicationPreferences.edit {
            it[VOICE_STYLE] = ReminderVoiceStyle.CUSTOM.storageId
            it[SPEECH_PITCH] = value.coerceIn(0.8f, 1.3f)
        }
    }

    suspend fun setSpeechVolume(value: Float) {
        context.medicationPreferences.edit {
            it[VOICE_STYLE] = ReminderVoiceStyle.CUSTOM.storageId
            it[SPEECH_VOLUME] = value.coerceIn(0.4f, 1f)
        }
    }

    private companion object {
        val SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val REPEAT_SECONDS = intPreferencesKey("repeat_interval_seconds")
        val VOICE_STYLE = stringPreferencesKey("voice_style")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val SPEECH_VOLUME = floatPreferencesKey("speech_volume")
    }
}
