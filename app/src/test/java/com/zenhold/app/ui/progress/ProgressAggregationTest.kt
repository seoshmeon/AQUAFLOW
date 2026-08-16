package com.zenhold.app.ui.progress

import com.zenhold.app.data.local.BreathHoldRecord
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressAggregationTest {
    @Test
    fun recordsAreAggregatedIntoCalendarMonths() {
        val records = listOf(
            record(40_000L, "jan-session", 1, 2026, 1, 12),
            record(50_000L, "jan-session", 2, 2026, 1, 12),
            record(70_000L, "feb-session", 1, 2026, 2, 3),
        )

        val state = buildProgressState(records)

        assertEquals(2, state.months.size)
        assertEquals(45_000L, state.months[0].averageMillis)
        assertEquals(50_000L, state.months[0].maximumMillis)
        assertEquals(2, state.months[0].attemptCount)
        assertEquals(1, state.months[0].sessionCount)
        assertEquals(70_000L, state.months[1].maximumMillis)
    }

    @Test
    fun comfortableAverage_usesOnlyEasyAndComfortableAttempts() {
        val records = listOf(
            record(40_000L, "session", 1, 2026, 1, 12).copy(comfortRating = 1),
            record(60_000L, "session", 2, 2026, 1, 12).copy(comfortRating = 2),
            record(90_000L, "session", 3, 2026, 1, 12).copy(comfortRating = 4),
        )

        val state = buildProgressState(records)

        assertEquals(50_000L, state.comfortableAverageMillis)
        assertEquals(3, state.ratedAttemptCount)
    }

    @Test
    fun monthPeriod_filtersChartAndComparesWithPreviousMonth() {
        val now = LocalDateTime.of(2026, 8, 16, 12, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val records = listOf(
            record(40_000L, "july", 1, 2026, 7, 10),
            record(60_000L, "august", 1, 2026, 8, 10),
        )

        val state = buildProgressState(records, ProgressPeriod.Month, now)

        assertEquals(1, state.sessions.size)
        assertEquals(60_000L, state.sessions.single().maximumMillis)
        assertEquals(50f, state.comparisonPercent ?: 0f, 0.01f)
    }

    @Test
    fun sessionSummary_keepsNoteAndUnlocksStableSession() {
        val records = listOf(
            record(60_000L, "session", 1, 2026, 8, 12).copy(sessionNote = "Спокойно"),
            record(64_000L, "session", 2, 2026, 8, 12),
            record(66_000L, "session", 3, 2026, 8, 12),
        )

        val state = buildProgressState(records)

        assertEquals("Спокойно", state.sessionSummaries.single().note)
        assertTrue(state.achievements.first { it.title == "Ровное дыхание" }.unlocked)
    }

    private fun record(
        duration: Long,
        sessionId: String,
        attempt: Int,
        year: Int,
        month: Int,
        day: Int,
    ) = BreathHoldRecord(
        holdDurationMillis = duration,
        recoveryDurationMillis = 30_000L,
        timestamp = LocalDateTime.of(year, month, day, 12, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
        sessionId = sessionId,
        attemptNumber = attempt,
    )
}
