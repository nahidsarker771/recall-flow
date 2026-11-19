package com.studyretention.tracker.logic

import kotlin.math.roundToInt

/** Scheduler choices exposed in Settings. Stored by `name`. */
enum class Algorithm(val label: String) {
    GUIDED("Guided SM-2"),
    SM2("Classic SM-2"),
}

/** Minimal, framework-free scheduler state (mirrors the Topic columns). */
data class ReviewState(
    val easeFactor: Double = SpacedRepetitionScheduler.DEFAULT_EASE_FACTOR,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val masteryLevel: Int = 0,
)

data class ScheduleResult(
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val masteryLevel: Int,
    val nextReviewAt: Long,
)

/**
 * SM-2 spaced-repetition engine. Pure Kotlin: no Android types, no clock, no DB —
 * every decision is a function of its arguments, which keeps it easy to test and
 * lets FSRS be swapped in later behind the same shape.
 *
 * Two strategies are supported:
 * - [Algorithm.GUIDED] (default): the spec's gentle ladder 1 → 7 → 16 → 35 days for
 *   the first four successful reviews, then `interval * easeFactor`.
 * - [Algorithm.SM2]: textbook SM-2 intervals (1 day, 6 days, then `interval * EF`).
 *
 * A failed review (rating < 3) always restarts at 1 day with repetitions reset.
 * Ease factor follows `EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))`,
 * clamped at 1.3.
 */
class SpacedRepetitionScheduler {

    /** First review ~26 h after learning, when retention has dropped to roughly 80%. */
    fun firstReviewAt(fromMillis: Long): Long = fromMillis + FIRST_REVIEW_HOURS * HOUR_MS

    fun schedule(
        state: ReviewState,
        rating: Int,
        nowMillis: Long,
        algorithm: Algorithm = Algorithm.GUIDED,
    ): ScheduleResult {
        val quality = rating.coerceIn(0, 5)
        val easeFactor = updatedEaseFactor(state.easeFactor, quality)

        if (quality < 3) {
            val interval = 1
            return ScheduleResult(
                easeFactor = easeFactor,
                intervalDays = interval,
                repetitions = 0,
                masteryLevel = masteryFor(interval, 0),
                nextReviewAt = nowMillis + interval * DAY_MS,
            )
        }

        val repetitions = state.repetitions + 1
        val interval = when (algorithm) {
            Algorithm.GUIDED ->
                if (repetitions <= GUIDED_LADDER.size) {
                    GUIDED_LADDER[repetitions - 1]
                } else {
                    scaledInterval(state.intervalDays, easeFactor)
                }

            Algorithm.SM2 -> when (repetitions) {
                1 -> 1
                2 -> 6
                else -> scaledInterval(state.intervalDays, state.easeFactor)
            }
        }

        return ScheduleResult(
            easeFactor = easeFactor,
            intervalDays = interval,
            repetitions = repetitions,
            masteryLevel = masteryFor(interval, repetitions),
            nextReviewAt = nowMillis + interval * DAY_MS,
        )
    }

    /** Preview without committing: what interval would this rating produce? */
    fun previewIntervalDays(
        state: ReviewState,
        rating: Int,
        algorithm: Algorithm = Algorithm.GUIDED,
    ): Int = schedule(state, rating, 0L, algorithm).intervalDays

    /** Manual override from the topic detail screen; ease and repetitions are kept. */
    fun overrideInterval(
        state: ReviewState,
        days: Int,
        nowMillis: Long,
    ): ScheduleResult {
        val interval = days.coerceIn(1, MAX_INTERVAL_DAYS)
        return ScheduleResult(
            easeFactor = state.easeFactor,
            intervalDays = interval,
            repetitions = state.repetitions,
            masteryLevel = masteryFor(interval, state.repetitions),
            nextReviewAt = nowMillis + interval * DAY_MS,
        )
    }

    fun updatedEaseFactor(current: Double, quality: Int): Double {
        val q = quality.coerceIn(0, 5)
        val delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        return (current + delta).coerceAtLeast(MIN_EASE_FACTOR)
    }

    private fun scaledInterval(currentIntervalDays: Int, easeFactor: Double): Int {
        val base = if (currentIntervalDays <= 0) 1 else currentIntervalDays
        return (base * easeFactor).roundToInt().coerceIn(1, MAX_INTERVAL_DAYS)
    }

    /** 0–100 mastery: interval progress toward ~90 days plus a repetition bonus. */
    private fun masteryFor(intervalDays: Int, repetitions: Int): Int =
        (intervalDays * 100 / 90 + repetitions * 5).coerceIn(0, 100)

    companion object {
        const val DEFAULT_EASE_FACTOR = 2.5
        const val MIN_EASE_FACTOR = 1.3
        const val FIRST_REVIEW_HOURS = 26L
        const val DAY_MS = 86_400_000L
        const val HOUR_MS = 3_600_000L
        const val MAX_INTERVAL_DAYS = 365

        /** The spec's recommended early ladder, in days. */
        val GUIDED_LADDER = listOf(1, 7, 16, 35)
    }
}
