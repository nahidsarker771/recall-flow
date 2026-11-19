package com.studyretention.tracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyretention.tracker.RecallFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReviewAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val topicId = intent.getLongExtra(NotificationHelper.EXTRA_TOPIC_ID, -1L)
        val topicName = intent.getStringExtra(NotificationHelper.EXTRA_TOPIC_NAME)
        if (topicId <= 0L || topicName.isNullOrBlank()) return

        val app = context.applicationContext as? RecallFlowApplication ?: return
        if (!app.container.preferences.settings.value.notificationsEnabled) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                NotificationHelper.showReview(context, topicId, topicName)
                app.container.alarmScheduler.sync()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
