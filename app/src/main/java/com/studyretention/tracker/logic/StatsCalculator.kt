package com.studyretention.tracker.logic

import com.studyretention.tracker.data.ReviewSession
import com.studyretention.tracker.data.Topic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class DayRetention(
    val date: LocalDate,
    val percent: Int,
    val reviews: Int,
)

data class StudyStats(
    val totalReviews: Int,
    val averageRetentionPercent: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalStudySeconds: Long,
    val reviewsToday: Int,
    val retentionByDay: List<DayRetention>,
)

/** Pure helpers for streaks, daily progress and chart series. */
object StatsCalculator {

    fun dateOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun currentStreak(sessions: List<ReviewSession>, today: LocalDate = LocalDate.now()): Int {
        val days = sessions.map { dateOf(it.timestamp) }.toSet()
        if (days.isEmpty()) return 0
        var cursor = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (cursor in days) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun longestStreak(sessions: List<ReviewSession>): Int {
        val days = sessions.map { dateOf(it.timestamp) }.distinct().sorted()
        if (days.isEmpty()) return 0
        var best = 1
        var run = 1
        for (index in 1 until days.size) {
            run = if (ChronoUnit.DAYS.between(days[index - 1], days[index]) == 1L) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    fun reviewsToday(
        sessions: List<ReviewSession>,
        today: LocalDate = LocalDate.now(),
    ): Int = sessions.count { dateOf(it.timestamp) == today }

    fun averageRetentionPercent(sessions: List<ReviewSession>): Int {
        if (sessions.isEmpty()) return 0
        return (sessions.sumOf { it.recallRating } * 100.0 / (sessions.size * 5)).toInt()
    }

    /** Average recall per day for the last [days] days, oldest first. */
    fun retentionByDay(
        sessions: List<ReviewSession>,
        today: LocalDate = LocalDate.now(),
        days: Int = 30,
    ): List<DayRetention> {
        val start = today.minusDays((days - 1).toLong())
        return sessions
            .filter { dateOf(it.timestamp) >= start }
            .groupBy { dateOf(it.timestamp) }
            .toSortedMap()
            .map { (date, daySessions) ->
                DayRetention(
                    date = date,
                    percent = (daySessions.sumOf { it.recallRating } * 100.0 / (daySessions.size * 5)).toInt(),
                    reviews = daySessions.size,
                )
            }
    }

    fun stats(sessions: List<ReviewSession>, topics: List<Topic>): StudyStats = StudyStats(
        totalReviews = sessions.size,
        averageRetentionPercent = averageRetentionPercent(sessions),
        currentStreak = currentStreak(sessions),
        longestStreak = longestStreak(sessions),
        totalStudySeconds = topics.sumOf { it.totalTimeStudied },
        reviewsToday = reviewsToday(sessions),
        retentionByDay = retentionByDay(sessions),
    )
}
