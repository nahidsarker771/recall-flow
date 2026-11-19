package com.studyretention.tracker.focus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studyretention.tracker.MainActivity
import com.studyretention.tracker.R
import com.studyretention.tracker.notifications.NotificationHelper

/**
 * Keeps the focus timer alive for the whole session: an ongoing stopwatch
 * notification (with a Stop action) plus a 15 s tick that upgrades the text with
 * milestone compliments. The elapsed time itself is derived from the stored start
 * timestamp, so it survives activity recreation and screen-off.
 */
class FocusSessionService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastMilestone = -1

    private val tick = object : Runnable {
        override fun run() {
            val state = FocusSession.read(this@FocusSessionService)
            if (state == null) {
                stopSelf()
                return
            }
            val minutes = elapsedMinutes(state)
            val milestone = FocusSession.milestoneFor(minutes)
            if (milestone != null && milestone.minutes > lastMilestone) {
                lastMilestone = milestone.minutes
                notify(buildNotification(state, milestone.message))
                if (milestone.minutes >= FocusSession.TARGET_MINUTES) {
                    notifyTargetReached(state)
                }
            }
            handler.postDelayed(this, TICK_MILLIS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            FocusSession.clear(this)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val state = FocusSession.read(this)
        if (state == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        lastMilestone = FocusSession.milestoneFor(elapsedMinutes(state))?.minutes ?: -1
        startForeground(NOTIFICATION_ID, buildNotification(state, null))
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, TICK_MILLIS)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        super.onDestroy()
    }

    private fun elapsedMinutes(state: FocusSession.State): Long =
        ((SystemClock.elapsedRealtime() - state.startElapsed).coerceAtLeast(0L)) / 60_000L

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val ongoing = NotificationChannel(
            CHANNEL_ONGOING,
            "Focus session",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing stopwatch while a review is running."
            setShowBadge(false)
        }
        val updates = NotificationChannel(
            CHANNEL_UPDATES,
            "Focus milestones",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Encouragement when you reach a focus milestone."
        }
        manager.createNotificationChannel(ongoing)
        manager.createNotificationChannel(updates)
    }

    private fun buildNotification(state: FocusSession.State, message: String?): Notification {
        val openIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("recallflow://review/${state.topicId}"),
            this,
            MainActivity::class.java,
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationHelper.EXTRA_TOPIC_ID, state.topicId)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FocusSessionService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = state.topicName.ifBlank { "Review" }
        val text = message ?: "$title · focus timer running — tap to return"

        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Focusing on \"$title\"")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setWhen(state.startWall)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun notifyTargetReached(state: FocusSession.State) {
        val openIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("recallflow://review/${state.topicId}"),
            this,
            MainActivity::class.java,
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationHelper.EXTRA_TOPIC_ID, state.topicId)
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            2,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("90-minute focus complete")
            .setContentText("Save your review to lock in the next interval.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        notify(notification, completion = true)
    }

    private fun notify(notification: Notification, completion: Boolean = false) {
        if (!NotificationHelper.hasPermission(this)) return
        val id = if (completion) COMPLETION_NOTIFICATION_ID else NOTIFICATION_ID
        NotificationManagerCompat.from(this).notify(id, notification)
    }

    companion object {
        const val ACTION_STOP = "com.studyretention.tracker.action.STOP_FOCUS"
        private const val CHANNEL_ONGOING = "focus_session"
        private const val CHANNEL_UPDATES = "focus_milestones"
        private const val NOTIFICATION_ID = 7100
        private const val COMPLETION_NOTIFICATION_ID = 7101
        private const val TICK_MILLIS = 15_000L
    }
}
