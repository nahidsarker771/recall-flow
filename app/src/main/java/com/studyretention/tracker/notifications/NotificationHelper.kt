package com.studyretention.tracker.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.studyretention.tracker.MainActivity
import com.studyretention.tracker.R
import java.time.LocalDate

object NotificationHelper {

    const val CHANNEL_ID = "review_reminders"
    const val EXTRA_TOPIC_ID = "com.studyretention.tracker.extra.REVIEW_TOPIC_ID"
    const val EXTRA_TOPIC_NAME = "com.studyretention.tracker.extra.REVIEW_TOPIC_NAME"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = android.app.NotificationChannel(
            CHANNEL_ID,
            "Review reminders",
            android.app.NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Tells you when a topic is ready for review."
        }
        context.getSystemService(android.app.NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    fun showReview(context: Context, topicId: Long, topicName: String) {
        if (topicId <= 0) return
        if (!hasPermission(context)) return
        if (alreadyNotifiedToday(context, topicId)) return

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("recallflow://review/$topicId"),
            context,
            MainActivity::class.java,
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TOPIC_ID, topicId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            topicId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to review \"$topicName\"")
            .setContentText("A quick recall check keeps it fresh.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(topicId.toInt(), notification)
        markNotifiedToday(context, topicId)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun alreadyNotifiedToday(context: Context, topicId: Long): Boolean {
        val day = LocalDate.now().toEpochDay()
        return prefs(context).getLong(key(topicId), -1L) == day
    }

    private fun markNotifiedToday(context: Context, topicId: Long) {
        prefs(context).edit().putLong(key(topicId), LocalDate.now().toEpochDay()).apply()
    }

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences("recallflow_notifications", Context.MODE_PRIVATE)

    private fun key(topicId: Long) = "last_notified_day_$topicId"
}
