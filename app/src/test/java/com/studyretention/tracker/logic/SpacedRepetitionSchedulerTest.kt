package com.studyretention.tracker.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionSchedulerTest {

    private val scheduler = SpacedRepetitionScheduler()
    private val now = 1_700_000_000_000L

    @Test
    fun `first review is 26 hours after learning`() {
        assertEquals(now + 26 * 3_600_000L, scheduler.firstReviewAt(now))
    }

    @Test
    fun `perfect recall raises the ease factor`() {
        assertEquals(2.6, scheduler.updatedEaseFactor(2.5, 5), 0.0001)
    }

    @Test
    fun `quality 3 lowers the ease factor`() {
        assertEquals(2.36, scheduler.updatedEaseFactor(2.5, 3), 0.0001)
    }

    @Test
    fun `ease factor never drops below 1_3`() {
        var ef = 2.5
        repeat(10) { ef = scheduler.updatedEaseFactor(ef, 0) }
        assertEquals(1.3, ef, 0.0001)
    }

    @Test
    fun `guided ladder follows 1 7 16 35 days`() {
        var state = ReviewState()
        val expected = listOf(1, 7, 16, 35)
        expected.forEach { days ->
            val result = scheduler.schedule(state, rating = 4, now, Algorithm.GUIDED)
            assertEquals(days, result.intervalDays)
            assertEquals(days * SpacedRepetitionScheduler.DAY_MS + now, result.nextReviewAt)
            state = result.toState()
        }
        assertEquals(4, state.repetitions)
    }

    @Test
    fun `guided ladder grows by ease factor after 35 days`() {
        var state = ReviewState(intervalDays = 35, repetitions = 4)
        val result = scheduler.schedule(state, rating = 5, now, Algorithm.GUIDED)
        // EF 2.5 + 0.1 = 2.6; 35 * 2.6 = 91 days
        assertEquals(91, result.intervalDays)
        assertEquals(2.6, result.easeFactor, 0.0001)
    }

    @Test
    fun `classic sm2 uses 1 then 6 then ease factor`() {
        var state = ReviewState()
        val first = scheduler.schedule(state, rating = 5, now, Algorithm.SM2)
        assertEquals(1, first.intervalDays)
        val second = scheduler.schedule(first.toState(), rating = 5, now, Algorithm.SM2)
        assertEquals(6, second.intervalDays)
        val third = scheduler.schedule(second.toState(), rating = 5, now, Algorithm.SM2)
        // EF after two perfect recalls: 2.7; 6 * 2.7 = 16.2 -> 16
        assertEquals(16, third.intervalDays)
    }

    @Test
    fun `failed recall resets to one day tomorrow`() {
        val state = ReviewState(intervalDays = 35, repetitions = 4, easeFactor = 2.5)
        val result = scheduler.schedule(state, rating = 2, now, Algorithm.GUIDED)
        assertEquals(1, result.intervalDays)
        assertEquals(0, result.repetitions)
        assertEquals(now + SpacedRepetitionScheduler.DAY_MS, result.nextReviewAt)
    }

    @Test
    fun `mastery stays within 0 and 100`() {
        val low = scheduler.schedule(ReviewState(), rating = 0, now, Algorithm.GUIDED)
        assertTrue(low.masteryLevel in 0..100)
        val high = scheduler.schedule(
            ReviewState(intervalDays = 365, repetitions = 12, easeFactor = 3.0),
            rating = 5, now, Algorithm.GUIDED,
        )
        assertEquals(100, high.masteryLevel)
    }

    @Test
    fun `manual override clamps the interval`() {
        val result = scheduler.overrideInterval(ReviewState(easeFactor = 2.4), days = 9999, now)
        assertEquals(SpacedRepetitionScheduler.MAX_INTERVAL_DAYS, result.intervalDays)
        assertEquals(now + SpacedRepetitionScheduler.MAX_INTERVAL_DAYS * SpacedRepetitionScheduler.DAY_MS, result.nextReviewAt)
        assertEquals(2.4, result.easeFactor, 0.0001)
    }

    @Test
    fun `preview matches the committed interval`() {
        val state = ReviewState(intervalDays = 7, repetitions = 2)
        assertEquals(
            scheduler.schedule(state, 4, now, Algorithm.GUIDED).intervalDays,
            scheduler.previewIntervalDays(state, 4, Algorithm.GUIDED),
        )
    }

    private fun ScheduleResult.toState() = ReviewState(
        easeFactor = easeFactor,
        intervalDays = intervalDays,
        repetitions = repetitions,
        masteryLevel = masteryLevel,
    )
}
