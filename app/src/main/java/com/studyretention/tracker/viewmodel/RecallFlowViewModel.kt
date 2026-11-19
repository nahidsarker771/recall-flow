package com.studyretention.tracker.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyretention.tracker.RecallFlowApplication
import com.studyretention.tracker.data.ReviewSession
import com.studyretention.tracker.data.ScheduledEvent
import com.studyretention.tracker.data.ThemeMode
import com.studyretention.tracker.data.Topic
import com.studyretention.tracker.logic.Algorithm
import com.studyretention.tracker.logic.SpacedRepetitionScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecallFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as RecallFlowApplication).container
    private val repository = container.repository
    val scheduler = SpacedRepetitionScheduler()

    val topics: StateFlow<List<Topic>> = repository.topics.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val sessions: StateFlow<List<ReviewSession>> = repository.sessions.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val events: StateFlow<List<ScheduledEvent>> = repository.events.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val settings = container.preferences.settings

    fun onboardingCompleted(): Boolean = container.onboarding.isCompleted

    fun completeOnboarding() = container.onboarding.markCompleted()

    // ---- Topics ----

    fun addTopic(name: String, notes: String, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addTopic(name, notes, category)
            syncAlarm()
        }
    }

    fun updateTopic(topic: Topic) {
        viewModelScope.launch { repository.updateTopic(topic) }
    }

    fun deleteTopic(topic: Topic) {
        viewModelScope.launch {
            repository.deleteTopic(topic)
            syncAlarm()
        }
    }

    fun snoozeTopic(topicId: Long) {
        viewModelScope.launch {
            repository.snoozeTopic(topicId, SNOOZE_MILLIS)
            syncAlarm()
        }
    }

    fun saveReview(topicId: Long, rating: Int, durationSeconds: Int, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveReview(topicId, rating, durationSeconds)
            syncAlarm()
            withContext(Dispatchers.Main) { onSaved() }
        }
    }

    fun overrideInterval(topicId: Long, days: Int) {
        viewModelScope.launch {
            repository.overrideInterval(topicId, days)
            syncAlarm()
        }
    }

    // ---- Calendar ----

    fun addEvent(topicId: Long, scheduledTime: Long) {
        viewModelScope.launch { repository.addEvent(topicId, scheduledTime) }
    }

    fun moveEvent(eventId: Long, newTime: Long) {
        viewModelScope.launch { repository.moveEvent(eventId, newTime) }
    }

    fun setEventCompleted(eventId: Long, completed: Boolean) {
        viewModelScope.launch { repository.setEventCompleted(eventId, completed) }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch { repository.deleteEvent(eventId) }
    }

    fun setAutoSchedule(enabled: Boolean) {
        container.preferences.update { it.copy(autoSchedule = enabled) }
        viewModelScope.launch {
            if (enabled) repository.ensureAutoScheduleEvents()
            syncAlarm()
        }
    }

    // ---- Settings ----

    fun setDailyLimit(limit: Int) =
        container.preferences.update { it.copy(dailyLimit = limit.coerceAtLeast(0)) }

    fun setNotificationsEnabled(enabled: Boolean) {
        container.preferences.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch { syncAlarm() }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        container.preferences.update {
            it.copy(reminderHour = hour.coerceIn(0, 23), reminderMinute = minute.coerceIn(0, 59))
        }
        viewModelScope.launch { syncAlarm() }
    }

    fun setAlgorithm(algorithm: Algorithm) =
        container.preferences.update { it.copy(algorithm = algorithm) }

    fun setThemeMode(mode: ThemeMode) =
        container.preferences.update { it.copy(themeMode = mode) }

    fun setDynamicColor(enabled: Boolean) =
        container.preferences.update { it.copy(dynamicColor = enabled) }

    // ---- Backup ----

    fun exportData(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val json = repository.exportJson()
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("Could not open the selected file")
                stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }.isSuccess
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    fun importData(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val json = context.contentResolver.openInputStream(uri)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: return@runCatching false
                val imported = repository.importJson(json)
                if (imported) syncAlarm()
                imported
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    fun nextUpcoming(now: Long = System.currentTimeMillis()): Topic? =
        topics.value.filter { it.nextReviewAt > now }.minByOrNull { it.nextReviewAt }

    private suspend fun syncAlarm() {
        runCatching { container.alarmScheduler.sync() }
    }

    companion object {
        const val SNOOZE_MILLIS = 60 * 60 * 1000L
    }
}
