package com.studyretention.tracker.data

import android.content.Context
import androidx.room.Room
import com.studyretention.tracker.notifications.AlarmScheduler

/** Manual dependency container owned by [com.studyretention.tracker.RecallFlowApplication]. */
class AppContainer(context: Context) {
    val preferences = AppPreferences(context)

    val database: RecallFlowDatabase = Room.databaseBuilder(
        context,
        RecallFlowDatabase::class.java,
        "recallflow.db",
    ).build()

    val repository = StudyRepository(database, preferences)
    val onboarding = OnboardingPreferences(context)
    val alarmScheduler = AlarmScheduler(context, preferences, repository)
}
