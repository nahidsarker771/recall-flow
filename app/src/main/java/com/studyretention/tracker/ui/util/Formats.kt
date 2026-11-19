package com.studyretention.tracker.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val zone: ZoneId get() = ZoneId.systemDefault()

fun LocalDate.toEpochMillis(time: LocalTime = LocalTime.NOON): Long =
    atTime(time).atZone(zone).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()

fun formatTime(epochMillis: Long): String =
    epochMillis.toLocalDateTime().format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

fun formatTime(hour: Int, minute: Int): String =
    LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))

fun formatMonthYear(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

fun formatDayLong(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))

fun formatDayShort(epochMillis: Long): String =
    epochMillis.toLocalDate().format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

fun formatDateShort(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

/**
 * Human next-review label: "Overdue 2d", "Due now", "Today, 6:00 PM",
 * "Tomorrow", "In 5 days", or "Nov 24".
 */
fun relativeDueLabel(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (epochMillis <= nowMillis) {
        val daysLate = ChronoUnit.DAYS.between(toLocalDateOf(epochMillis), toLocalDateOf(nowMillis))
        return if (daysLate >= 1) "Overdue ${daysLate}d" else "Due now"
    }
    val dueDate = toLocalDateOf(epochMillis)
    val today = toLocalDateOf(nowMillis)
    val days = ChronoUnit.DAYS.between(today, dueDate)
    return when {
        days == 0L -> "Today, ${formatTime(epochMillis)}"
        days == 1L -> "Tomorrow"
        days in 2..6 -> "In $days days"
        else -> formatDayShort(epochMillis)
    }
}

private fun toLocalDateOf(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

/** "45s", "12m", "1h 05m", "3h 20m". */
fun durationLabel(seconds: Long): String {
    val total = seconds.coerceAtLeast(0)
    if (total < 60) return "${total}s"
    val minutes = total / 60
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    val rem = minutes % 60
    return if (rem == 0L) "${hours}h" else "${hours}h ${rem.toString().padStart(2, '0')}m"
}

/** "12:34" style stopwatch display. */
fun stopwatchLabel(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

/** Live stopwatch display with tenths: "00:12.3". */
fun stopwatchTenthsLabel(millis: Long): String {
    val safe = millis.coerceAtLeast(0)
    val totalSeconds = safe / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val tenths = (safe % 1000) / 100
    return "%02d:%02d.%d".format(minutes, seconds, tenths)
}

fun ratingLabel(rating: Int): String = when (rating.coerceIn(0, 5)) {
    0 -> "Blackout"
    1 -> "Wrong"
    2 -> "Hard to recall"
    3 -> "Shaky"
    4 -> "Good"
    else -> "Perfect"
}

fun intervalLabel(days: Int): String = when (days) {
    0 -> "now"
    1 -> "tomorrow"
    else -> "in $days days"
}
