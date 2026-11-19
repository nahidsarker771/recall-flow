package com.studyretention.tracker.data

import android.content.Context

/**
 * One-time flags. Intentionally tiny and synchronous so the very first frame can
 * decide whether to show onboarding without a loading state.
 */
class OnboardingPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("recallflow_prefs", Context.MODE_PRIVATE)

    val isCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private companion object {
        const val KEY_COMPLETED = "onboarding_completed_v1"
    }
}
