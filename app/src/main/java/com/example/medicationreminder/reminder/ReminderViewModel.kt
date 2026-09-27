package com.example.medicationreminder.reminder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.medicationreminder.data.MedicationRepository
import com.example.medicationreminder.data.PreferencesRepository
import com.example.medicationreminder.domain.DoseOccurrence
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderUiState(
    val loading: Boolean = true,
    val occurrences: List<DoseOccurrence> = emptyList(),
    val completed: Boolean = false,
    val resolvingIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
)

class ReminderViewModel(
    private var eventIds: List<Long>,
    private val repository: MedicationRepository,
    private val preferencesRepository: PreferencesRepository,
    private val coordinator: AlarmCoordinator,
    private val applicationContext: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(ReminderUiState())
    private var reloadGeneration = 0L
    val state: StateFlow<ReminderUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun updateEventIds(ids: List<Long>) {
        eventIds = (eventIds + ids).distinct()
        if (_state.value.resolvingIds.isEmpty()) reload()
    }

    fun markTaken(id: Long) = resolve(id) {
        if (repository.markTaken(listOf(id))) {
            val name = _state.value.occurrences.firstOrNull { it.eventId == id }?.medicationName.orEmpty()
            "好的，已记录${name}本次已经服用。"
        } else null
    }

    fun markSkipped(id: Long) = resolve(id) {
        if (repository.markSkipped(listOf(id))) "已记录跳过本次用药。" else null
    }

    fun snooze(id: Long) = resolve(id) {
        val minutes = preferencesRepository.preferences.first().defaultSnoozeMinutes
        if (repository.snooze(listOf(id), minutes)) "好的，休息一下，我会在${minutes}分钟后再次提醒你。" else null
    }

    private fun resolve(id: Long, action: suspend () -> String?) {
        if (_state.value.resolvingIds.isNotEmpty()) return
        _state.update {
            it.copy(
                resolvingIds = it.resolvingIds + id,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            var changed = false
            var feedback: String? = null
            var errorMessage: String? = null
            try {
                feedback = action()
                changed = feedback != null
                coordinator.synchronize()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                errorMessage = "本次操作没有完成，请检查后重试"
            } finally {
                runCatching {
                    if (changed) AlarmPlaybackService.confirmAction(applicationContext, feedback.orEmpty())
                    else AlarmPlaybackService.refresh(applicationContext)
                }
                // A new alarm may have arrived while this action was being saved.
                // Reload the combined IDs before deciding whether the lock screen can close.
                reload(errorMessage)
            }
        }
    }

    private fun reload(errorMessage: String? = null) {
        val generation = ++reloadGeneration
        viewModelScope.launch {
            val requestedIds = eventIds
            val occurrences = repository.getOccurrences(requestedIds)
                .filter { it.status.name == "RINGING" }
            if (generation != reloadGeneration) return@launch
            if (requestedIds != eventIds) {
                reload(errorMessage)
                return@launch
            }
            _state.value = ReminderUiState(
                loading = false,
                occurrences = occurrences,
                completed = occurrences.isEmpty(),
                resolvingIds = emptySet(),
                errorMessage = errorMessage,
            )
        }
    }

    class Factory(
        private val eventIds: List<Long>,
        private val repository: MedicationRepository,
        private val preferencesRepository: PreferencesRepository,
        private val coordinator: AlarmCoordinator,
        private val applicationContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ReminderViewModel(
                eventIds,
                repository,
                preferencesRepository,
                coordinator,
                applicationContext,
            ) as T
    }
}
