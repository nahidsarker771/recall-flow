package com.studyretention.tracker.focus

import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat

/**
 * Durable state for the stopwatch so a long (up to 90 min) focus sitting keeps
 * running while the screen is off, the app is in the background, or the activity
 * is recreated. The foreground service reads the same preferences.
 */
object FocusSession {

    const val TARGET_MINUTES = 90

    private const val PREFS = "focus_session"
    private const val KEY_ACTIVE = "active"
    private const val KEY_TOPIC_ID = "topic_id"
    private const val KEY_TOPIC_NAME = "topic_name"
    private const val KEY_START_ELAPSED = "start_elapsed"
    private const val KEY_START_WALL = "start_wall"

    data class State(
        val topicId: Long,
        val topicName: String,
        val startElapsed: Long,
        val startWall: Long,
    )

    data class Milestone(val minutes: Int, val message: String)

    val milestones: List<Milestone> = listOf(
        Milestone(1, "One minute in. Starting was the hard part — you did it."),
        Milestone(5, "Five minutes of focus. The first block is always the hardest."),
        Milestone(10, "Ten minutes. Your recall is getting stronger with every pass."),
        Milestone(15, "Fifteen minutes. Stay with it — this is where it starts to stick."),
        Milestone(30, "Thirty minutes. A third of a full focus block. Keep going."),
        Milestone(45, "Forty-five minutes. Halfway there — your future self says thanks."),
        Milestone(60, "One hour of unbroken focus. That is real progress."),
        Milestone(75, "Seventy-five minutes. Fifteen to go — finish strong."),
        Milestone(90, "Ninety minutes. Full focus session complete. Well earned."),
    )

    fun milestoneFor(minutes: Long): Milestone? =
        milestones.lastOrNull { minutes >= it.minutes }

    fun read(context: Context): State? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ACTIVE, false)) return null
        val topicId = prefs.getLong(KEY_TOPIC_ID, 0L)
        if (topicId <= 0L) return null
        val startElapsed = prefs.getLong(KEY_START_ELAPSED, 0L)
        if (startElapsed <= 0L) return null
        return State(
            topicId = topicId,
            topicName = prefs.getString(KEY_TOPIC_NAME, "").orEmpty(),
            startElapsed = startElapsed,
            startWall = prefs.getLong(KEY_START_WALL, System.currentTimeMillis()),
        )
    }

    /** Resumes the running session for this topic, or starts a fresh one. */
    fun ensure(context: Context, topicId: Long, topicName: String): State {
        val existing = read(context)
        if (existing != null && existing.topicId == topicId) return existing
        return start(context, topicId, topicName)
    }

    fun start(context: Context, topicId: Long, topicName: String): State {
        val state = State(
            topicId = topicId,
            topicName = topicName,
            startElapsed = SystemClock.elapsedRealtime(),
            startWall = System.currentTimeMillis(),
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putLong(KEY_TOPIC_ID, topicId)
            .putString(KEY_TOPIC_NAME, topicName)
            .putLong(KEY_START_ELAPSED, state.startElapsed)
            .putLong(KEY_START_WALL, state.startWall)
            .apply()
        try {
            ContextCompat.startForegroundService(
                context,
                android.content.Intent(context, FocusSessionService::class.java),
            )
        } catch (_: Exception) {
            // If the OS refuses a background start, the in-app timer still works.
        }
        return state
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun stop(context: Context) {
        clear(context)
        try {
            context.stopService(android.content.Intent(context, FocusSessionService::class.java))
        } catch (_: Exception) {
            // Nothing to stop.
        }
    }
}
