package com.example.medicationreminder.ui

import android.app.AlarmManager
import android.app.Application
import android.app.ActivityManager
import android.app.NotificationManager
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.medicationreminder.data.MedicationRepository
import com.example.medicationreminder.data.PreferencesRepository
import com.example.medicationreminder.domain.DoseOccurrence
import com.example.medicationreminder.domain.MedicationDraft
import com.example.medicationreminder.domain.MedicationPlan
import com.example.medicationreminder.domain.ReminderVoiceStyle
import com.example.medicationreminder.domain.UserPreferences
import com.example.medicationreminder.domain.Zodiac
import com.example.medicationreminder.reminder.AlarmCoordinator
import com.example.medicationreminder.reminder.AlarmPlaybackService
import com.example.medicationreminder.reminder.ReminderNotifications
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SystemHealth(
    val notificationsAllowed: Boolean = false,
    val alarmChannelAllowed: Boolean = false,
    val exactAlarmsAllowed: Boolean = false,
    val fullScreenAllowed: Boolean = false,
    val alarmVolumeAudible: Boolean = false,
    val backgroundRestricted: Boolean = false,
    val nextReminderAt: Instant? = null,
)

val SystemHealth.voiceReminderReady: Boolean
    get() = notificationsAllowed && alarmChannelAllowed && exactAlarmsAllowed &&
        alarmVolumeAudible && !backgroundRestricted

class AppViewModel(
    application: Application,
    private val repository: MedicationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val alarmCoordinator: AlarmCoordinator,
) : AndroidViewModel(application) {
    val plans: StateFlow<List<MedicationPlan>> = repository.observePlans().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserPreferences(),
    )

    private val currentDayBounds = MutableStateFlow(calculateDayBounds())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val todayOccurrences: StateFlow<List<DoseOccurrence>> = currentDayBounds.flatMapLatest { bounds ->
        repository.observeOccurrences(bounds.first, bounds.second)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _health = MutableStateFlow(SystemHealth())
    val health: StateFlow<SystemHealth> = _health.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isSavingMedication = MutableStateFlow(false)
    val isSavingMedication: StateFlow<Boolean> = _isSavingMedication.asStateFlow()

    init {
        refreshHealth()
        viewModelScope.launch { alarmCoordinator.synchronize() }
        viewModelScope.launch {
            while (isActive) {
                val zone = ZoneId.systemDefault()
                val now = ZonedDateTime.now(zone)
                val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(zone)
                delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1_000L) + 1_000L)
                currentDayBounds.value = calculateDayBounds()
            }
        }
    }

    fun saveMedication(draft: MedicationDraft, onSaved: () -> Unit) {
        if (_isSavingMedication.value) return
        _isSavingMedication.value = true
        viewModelScope.launch {
            try {
                repository.saveMedication(draft)
                alarmCoordinator.synchronize()
                _message.value = "用药计划已保存"
                onSaved()
            } catch (error: Exception) {
                _message.value = error.message ?: "保存失败，请稍后重试"
            } finally {
                _isSavingMedication.value = false
            }
        }
    }

    fun archiveMedication(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.archiveMedication(id)
                alarmCoordinator.synchronize()
            }.onSuccess {
                _message.value = "该用药计划已停用，历史记录仍保留"
            }.onFailure {
                _message.value = it.message ?: "停用失败"
            }
        }
    }

    fun markOccurrenceTaken(id: Long, onComplete: (Boolean) -> Unit = {}) {
        resolveOccurrence(
            successMessage = "已记录：已经服用",
            action = { repository.markTakenFromToday(listOf(id)) },
            onComplete = onComplete,
        )
    }

    fun snoozeOccurrence(id: Long, onComplete: (Boolean) -> Unit = {}) {
        resolveOccurrence(
            successMessage = "已设置：稍后再次提醒",
            action = {
                val minutes = preferencesRepository.preferences.first().defaultSnoozeMinutes
                repository.snoozeFromToday(
                    ids = listOf(id),
                    minutes = minutes,
                )
            },
            onComplete = onComplete,
        )
    }

    fun skipOccurrence(id: Long, onComplete: (Boolean) -> Unit = {}) {
        resolveOccurrence(
            successMessage = "已记录：跳过本次",
            action = { repository.markSkippedFromToday(listOf(id)) },
            onComplete = onComplete,
        )
    }

    fun scheduleTestReminder() {
        runCatching {
            check(alarmCoordinator.scheduleTest()) { "请先开启“精确闹钟”权限" }
        }
            .onSuccess { _message.value = "后台测试已设置：请锁屏或划掉应用，约 30 秒后提醒" }
            .onFailure { _message.value = it.message ?: "测试提醒设置失败" }
    }

    fun previewVoice() {
        viewModelScope.launch {
            if (repository.hasRingingEvents()) {
                _message.value = "当前有正在进行的用药提醒，暂不能试听"
                return@launch
            }
            runCatching { AlarmPlaybackService.startTest(getApplication()) }
                .onSuccess { _message.value = "正在试听当前语音风格" }
                .onFailure { _message.value = it.message ?: "语音试听启动失败" }
        }
    }

    fun setSnoozeMinutes(value: Int) {
        viewModelScope.launch { preferencesRepository.setDefaultSnoozeMinutes(value) }
    }

    fun setZodiacCover(id: Long, zodiac: Zodiac) {
        viewModelScope.launch {
            runCatching { preferencesRepository.setZodiacCover(id, zodiac) }
                .onFailure { _message.value = "卡面未保存，请重试" }
        }
    }

    fun setRepeatSeconds(value: Int) {
        viewModelScope.launch { preferencesRepository.setRepeatIntervalSeconds(value) }
    }

    fun setVoiceStyle(value: ReminderVoiceStyle) {
        viewModelScope.launch { preferencesRepository.setVoiceStyle(value) }
    }

    fun setSpeechRate(value: Float) {
        viewModelScope.launch { preferencesRepository.setSpeechRate(value) }
    }

    fun setSpeechPitch(value: Float) {
        viewModelScope.launch { preferencesRepository.setSpeechPitch(value) }
    }

    fun setSpeechVolume(value: Float) {
        viewModelScope.launch { preferencesRepository.setSpeechVolume(value) }
    }

    fun refreshHealth() {
        val app = getApplication<Application>()
        val alarmManager = app.getSystemService(AlarmManager::class.java)
        val activityManager = app.getSystemService(ActivityManager::class.java)
        val notificationManager = app.getSystemService(NotificationManager::class.java)
        val audioManager = app.getSystemService(AudioManager::class.java)
        val alarmChannelAllowed =
            notificationManager.getNotificationChannel(AlarmPlaybackService.CHANNEL_ID)?.importance
                ?.let { it != NotificationManager.IMPORTANCE_NONE } == true
        _health.value = SystemHealth(
            notificationsAllowed = NotificationManagerCompat.from(app).areNotificationsEnabled(),
            alarmChannelAllowed = alarmChannelAllowed,
            exactAlarmsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager.canScheduleExactAlarms(),
            fullScreenAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                notificationManager.canUseFullScreenIntent(),
            alarmVolumeAudible = audioManager.getStreamVolume(AudioManager.STREAM_ALARM) > 0,
            backgroundRestricted = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                activityManager.isBackgroundRestricted,
        )
        viewModelScope.launch {
            val nextReminder = repository.nextWakeAt()
            _health.update { it.copy(nextReminderAt = nextReminder) }
        }
        currentDayBounds.value = calculateDayBounds()
    }

    fun onAppResumed() {
        refreshHealth()
        viewModelScope.launch { alarmCoordinator.synchronize() }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun resolveOccurrence(
        successMessage: String,
        action: suspend () -> Boolean,
        onComplete: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { action() }
                .onSuccess { changed ->
                    if (!changed) {
                        _message.value = "这次提醒的状态刚刚发生了变化，请返回列表查看"
                        refreshHealth()
                        onComplete(false)
                        return@onSuccess
                    }

                    val schedulingFailure = runCatching {
                        alarmCoordinator.synchronize()
                    }.exceptionOrNull()
                    val playbackFailure = runCatching {
                        refreshAlarmPlayback()
                    }.exceptionOrNull()
                    refreshHealth()

                    _message.value = if (schedulingFailure == null && playbackFailure == null) {
                        successMessage
                    } else {
                        "$successMessage，但后台提醒刷新失败，请到设置中检查提醒状态"
                    }
                    onComplete(true)
                }
                .onFailure {
                    _message.value = it.message ?: "操作失败，请稍后再试"
                    onComplete(false)
                }
        }
    }

    private suspend fun refreshAlarmPlayback() {
        val app = getApplication<Application>()
        if (repository.hasRingingEvents()) {
            AlarmPlaybackService.refresh(app)
        } else {
            AlarmPlaybackService.stop(app)
            ReminderNotifications.cancel(app)
        }
    }

    companion object {
        fun factory(
            application: Application,
            repository: MedicationRepository,
            preferencesRepository: PreferencesRepository,
            alarmCoordinator: AlarmCoordinator,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AppViewModel(application, repository, preferencesRepository, alarmCoordinator)
            }
        }

        private fun calculateDayBounds(zoneId: ZoneId = ZoneId.systemDefault()): Pair<Instant, Instant> {
            val today = LocalDate.now(zoneId)
            return today.atStartOfDay(zoneId).toInstant() to
                today.plusDays(1).atStartOfDay(zoneId).toInstant()
        }
    }
}
