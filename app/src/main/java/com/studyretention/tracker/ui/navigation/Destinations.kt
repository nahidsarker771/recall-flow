package com.studyretention.tracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

/** Every route in the app. */
object Routes {
    const val TODAY = "today"
    const val TOPICS = "topics"
    const val SCHEDULE = "schedule"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"

    const val TOPIC_DETAIL = "topic/{topicId}"
    const val REVIEW = "review/{topicId}"

    const val ARG_TOPIC_ID = "topicId"

    fun topicDetail(topicId: Long): String = "topic/$topicId"

    fun review(topicId: Long): String = "review/$topicId"
}

data class BottomTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/** The four primary destinations shown in the bottom navigation bar. */
val BottomTabs = listOf(
    BottomTab(Routes.TODAY, "Today", Icons.Filled.Today, Icons.Outlined.Today),
    BottomTab(Routes.TOPICS, "Topics", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    BottomTab(Routes.SCHEDULE, "Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomTab(Routes.STATS, "Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
)
