package com.zenhold.app.ui.progress

import com.zenhold.app.data.local.BreathHoldRecord
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
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
