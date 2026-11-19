package com.studyretention.tracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.studyretention.tracker.RecallFlowApplication
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Background safety net: every 15 minutes, notify about due topics (at most three,
 * once per topic per day) and refresh the exact alarm for the next review.
 */
class DueCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RecallFlowApplication ?: return Result.success()
        val settings = app.container.preferences.settings.value

        val reminderPassed = LocalTime.now() >=
            LocalTime.of(settings.reminderHour, settings.reminderMinute)

        if (settings.notificationsEnabled && reminderPassed) {
            val now = System.currentTimeMillis()
            val limit = if (settings.dailyLimit > 0) settings.dailyLimit else Int.MAX_VALUE
            val topics = app.container.repository.topics.first()
            topics.asSequence()
                .filter { it.nextReviewAt <= now }
                .take(minOf(limit, 3))
                .forEach { topic ->
                    NotificationHelper.showReview(applicationContext, topic.id, topic.name)
                }
        }

        app.container.alarmScheduler.sync()
        return Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "recallflow_due_check"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DueCheckWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
