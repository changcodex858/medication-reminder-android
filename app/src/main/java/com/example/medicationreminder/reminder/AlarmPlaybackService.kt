package com.example.medicationreminder.reminder

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import androidx.core.app.ServiceCompat
import com.example.medicationreminder.appGraph
import com.example.medicationreminder.domain.DoseOccurrence
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AlarmPlaybackService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var textToSpeech: TextToSpeech? = null
    private var ringtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var speechText = ""
    private var repeatIntervalMillis = 60_000L
    private var speechVolume = 0.78f
    private var stopped = false
    private var requestGeneration = 0L
    private var initializationTimeout: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isTest = intent?.getBooleanExtra(EXTRA_TEST, false) == true
        val generation = ++requestGeneration
        val initialEventIds = intent?.getLongArrayExtra(EXTRA_EVENT_IDS) ?: longArrayOf()
        val initialNames = intent?.getStringArrayExtra(EXTRA_MEDICATION_NAMES)?.toList().orEmpty()
        val initialMessage = intent?.getStringExtra(EXTRA_INITIAL_MESSAGE).orEmpty()
        val initialNotification = if (!isTest && initialEventIds.isNotEmpty() && initialMessage.isNotBlank()) {
            ReminderNotifications.reminder(this, initialEventIds, initialNames, initialMessage)
        } else {
            ReminderNotifications.loading(this, isTest)
        }
        promoteToForeground(initialNotification)

        serviceScope.launch {
            val preferences = appGraph.preferencesRepository.preferences.first()
            if (generation != requestGeneration || stopped) return@launch
            repeatIntervalMillis = preferences.repeatIntervalSeconds * 1000L
            speechVolume = preferences.speechVolume

            if (isTest) {
                speechText = ReminderSpeechComposer.testMessage(preferences.voiceStyle)
                promoteToForeground(ReminderNotifications.test(this@AlarmPlaybackService, speechText))
                initializeSpeech(preferences.speechRate, preferences.speechPitch, generation)
            } else {
                // Always aggregate every currently ringing dose. This keeps overlapping
                // reminders together and removes already-resolved medicines from speech.
                val occurrences = appGraph.medicationRepository.getRingingOccurrences()
                if (generation != requestGeneration || stopped) return@launch
                if (occurrences.isEmpty()) {
                    stopPlaybackAndSelf()
                    return@launch
                }
                speechText = ReminderSpeechComposer.compose(occurrences, preferences.voiceStyle)
                promoteToForeground(
                    ReminderNotifications.reminder(this@AlarmPlaybackService, occurrences, speechText)
                )
                initializeSpeech(preferences.speechRate, preferences.speechPitch, generation)
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        initializationTimeout = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ringtone?.stop()
        ringtone = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun initializeSpeech(speechRate: Float, speechPitch: Float, generation: Long) {
        handler.removeCallbacksAndMessages(null)
        ringtone?.stop()
        ringtone = null
        textToSpeech?.shutdown()
        val timeout = Runnable {
            if (generation == requestGeneration && !stopped) startFallbackRingtone(generation)
        }
        initializationTimeout = timeout
        handler.postDelayed(timeout, TTS_INITIALIZATION_TIMEOUT_MS)
        textToSpeech = TextToSpeech(this) { status ->
            if (generation != requestGeneration || stopped) return@TextToSpeech
            initializationTimeout?.let(handler::removeCallbacks)
            initializationTimeout = null
            if (status != TextToSpeech.SUCCESS) {
                startFallbackRingtone(generation)
                return@TextToSpeech
            }

            val tts = textToSpeech ?: return@TextToSpeech
            val languageResult = tts.setLanguage(Locale.SIMPLIFIED_CHINESE)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                startFallbackRingtone(generation)
                return@TextToSpeech
            }

            ringtone?.stop()
            ringtone = null

            runCatching {
                tts.voices
                    ?.asSequence()
                    ?.filter { voice ->
                        voice.locale.language == Locale.SIMPLIFIED_CHINESE.language &&
                            !voice.isNetworkConnectionRequired
                    }
                    ?.sortedByDescending { it.locale.country == Locale.SIMPLIFIED_CHINESE.country }
                    ?.firstOrNull()
                    ?.let { tts.voice = it }
            }

            tts.setSpeechRate(speechRate)
            tts.setPitch(speechPitch)
            tts.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    handler.postDelayed({ speak(generation) }, repeatIntervalMillis)
                }

                @Deprecated("Deprecated by Android")
                override fun onError(utteranceId: String?) {
                    handler.post { startFallbackRingtone(generation) }
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    handler.post { startFallbackRingtone(generation) }
                }
            })
            handler.postDelayed({ speak(generation) }, INITIAL_SPEECH_DELAY_MS)
        }
    }

    private fun speak(generation: Long) {
        if (generation != requestGeneration || stopped || speechText.isBlank()) return
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, speechVolume)
        }
        val result = textToSpeech?.speak(
            speechText,
            TextToSpeech.QUEUE_FLUSH,
            params,
            UTTERANCE_ID,
        )
        if (result == TextToSpeech.ERROR) startFallbackRingtone(generation)
    }

    private fun startFallbackRingtone(generation: Long) {
        if (generation != requestGeneration || stopped || ringtone?.isPlaying == true) return
        textToSpeech?.stop()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                volume = FALLBACK_RINGTONE_VOLUME
            }
            play()
        }
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (generation == requestGeneration && !stopped) {
                    if (ringtone?.isPlaying != true) ringtone?.play()
                    handler.postDelayed(this, FALLBACK_REPEAT_INTERVAL_MS)
                }
            }
        }, FALLBACK_REPEAT_INTERVAL_MS)
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(PowerManager::class.java)
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MedicationReminder:AlarmPlayback",
        ).apply { acquire(MAX_WAKE_LOCK_MS) }
    }

    private fun stopPlaybackAndSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun promoteToForeground(notification: android.app.Notification) {
        ServiceCompat.startForeground(
            this,
            ReminderNotifications.NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
    }

    companion object {
        const val EXTRA_EVENT_IDS = "event_ids"
        private const val EXTRA_TEST = "test"
        private const val EXTRA_MEDICATION_NAMES = "medication_names"
        private const val EXTRA_INITIAL_MESSAGE = "initial_message"
        private const val ACTION_START = "com.example.medicationreminder.action.START_PLAYBACK"
        const val CHANNEL_ID = ReminderNotifications.VOICE_CHANNEL_ID
        private const val UTTERANCE_ID = "medication-reminder"
        private const val MAX_WAKE_LOCK_MS = 30 * 60 * 1000L
        private const val INITIAL_SPEECH_DELAY_MS = 800L
        private const val TTS_INITIALIZATION_TIMEOUT_MS = 4_000L
        private const val FALLBACK_RINGTONE_VOLUME = 0.55f
        private const val FALLBACK_REPEAT_INTERVAL_MS = 12_000L

        fun start(
            context: Context,
            occurrences: List<DoseOccurrence> = emptyList(),
            initialMessage: String = "",
        ) {
            val intent = Intent(context, AlarmPlaybackService::class.java).apply {
                action = ACTION_START
                if (occurrences.isNotEmpty()) {
                    putExtra(EXTRA_EVENT_IDS, occurrences.map { it.eventId }.toLongArray())
                    putExtra(EXTRA_MEDICATION_NAMES, occurrences.map { it.medicationName }.toTypedArray())
                    putExtra(EXTRA_INITIAL_MESSAGE, initialMessage)
                }
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun startTest(context: Context) {
            val intent = Intent(context, AlarmPlaybackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TEST, true)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmPlaybackService::class.java))
        }

        fun refresh(context: Context) {
            val intent = Intent(context, AlarmPlaybackService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun createNotificationChannel(context: Context) {
            ReminderNotifications.createChannels(context)
        }
    }
}
