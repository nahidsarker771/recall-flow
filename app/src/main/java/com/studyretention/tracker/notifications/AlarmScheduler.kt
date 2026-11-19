package com.studyretention.tracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.studyretention.tracker.data.AppPreferences
import com.studyretention.tracker.data.StudyRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Keeps one exact alarm for the next due review. If the user hasn't granted
 * "Alarms & reminders" access on Android 12+, it falls back to an inexact
 * allow-while-idle alarm so reminders still fire.
 */
class AlarmScheduler(
    private val context: Context,
    private val preferences: AppPreferences,
    private val repository: StudyRepository,
) {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(AlarmManager::class.java)

    suspend fun sync() {
        if (!preferences.settings.value.notificationsEnabled) {
            cancel()
            return
        }
        val now = System.currentTimeMillis()
        val upcoming = repository.earliestTopic()
        if (upcoming == null) {
            cancel()
        } else {
            val settings = preferences.settings.value
            val at = reminderAt(
                dueAt = upcoming.nextReviewAt,
                now = now,
                hour = settings.reminderHour,
                minute = settings.reminderMinute,
            )
            scheduleAt(at, upcoming.id, upcoming.name)
        }
    }

    /**
     * Delivers a reminder at the user's chosen time of day, never before the topic
     * is due. Already-due topics get nudged at the next day's reminder instead of
     * re-firing every worker pass.
     */
    private fun reminderAt(dueAt: Long, now: Long, hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val reminder = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        val today = LocalDate.now(zone)
        val todayReminder = today.atTime(reminder).atZone(zone).toInstant().toEpochMilli()
        return when {
            dueAt > now -> maxOf(dueAt, todayReminder)
            todayReminder > now -> todayReminder
            else -> today.plusDays(1).atTime(reminder).atZone(zone).toInstant().toEpochMilli()
        }
    }

    fun scheduleAt(timeMillis: Long, topicId: Long, topicName: String) {
        val manager = alarmManager ?: return
        val triggerAt = maxOf(timeMillis, System.currentTimeMillis() + 5_000L)
        val operation = pendingIntent(topicId, topicName)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        } catch (security: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    fun cancel() {
        alarmManager?.cancel(pendingIntent(-1L, null))
    }

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
    }

    private fun pendingIntent(topicId: Long, topicName: String?): PendingIntent {
        val intent = Intent(context, ReviewAlarmReceiver::class.java).apply {
            action = ACTION_REVIEW_ALARM
            putExtra(NotificationHelper.EXTRA_TOPIC_ID, topicId)
            putExtra(NotificationHelper.EXTRA_TOPIC_NAME, topicName)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val ACTION_REVIEW_ALARM = "com.studyretention.tracker.action.REVIEW_ALARM"
        const val REQUEST_CODE = 501
    }
}
