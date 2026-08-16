package com.zenhold.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.domain.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProgressPeriod { Week, Month, Quarter, Year, All }

data class CalendarDay(
    val dayOfMonth: Int,
    val sessionCount: Int,
    val bestMillis: Long,
)

data class Achievement(
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

data class SessionSummary(
    val sessionId: String,
    val timestamp: Long,
    val attempts: List<BreathHoldRecord>,
    val averageMillis: Long,
    val maximumMillis: Long,
    val note: String,
)

data class SessionPoint(
    val timestamp: Long,
    val averageMillis: Long,
    val maximumMillis: Long,
)

data class MonthPoint(
    val yearMonth: YearMonth,
    val label: String,
    val averageMillis: Long,
    val maximumMillis: Long,
    val attemptCount: Int,
    val sessionCount: Int,
)

data class ProgressUiState(
    val sessions: List<SessionPoint> = emptyList(),
    val months: List<MonthPoint> = emptyList(),
    val records: List<BreathHoldRecord> = emptyList(),
    val personalBestMillis: Long = 0L,
    val recentAverageMillis: Long = 0L,
    val comfortableAverageMillis: Long = 0L,
    val ratedAttemptCount: Int = 0,
    val selectedPeriod: ProgressPeriod = ProgressPeriod.Month,
    val comparisonPercent: Float? = null,
    val calendarMonthLabel: String = "",
    val calendarDays: List<CalendarDay> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val sessionSummaries: List<SessionSummary> = emptyList(),
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: RecordRepository,
) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(ProgressPeriod.Month)

    val state: StateFlow<ProgressUiState> = combine(
        repository.observeRecords(),
        selectedPeriod,
    ) { records, period -> buildProgressState(records, period) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun selectPeriod(period: ProgressPeriod) {
        selectedPeriod.value = period
    }

    fun saveSessionNote(sessionId: String, note: String) {
        viewModelScope.launch {
            repository.updateSessionNote(sessionId, note.trim().take(500))
        }
    }
}

internal fun buildProgressState(
    records: List<BreathHoldRecord>,
    selectedPeriod: ProgressPeriod = ProgressPeriod.All,
    nowMillis: Long = System.currentTimeMillis(),
): ProgressUiState {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru-RU"))
        if (records.isEmpty()) return ProgressUiState(
            selectedPeriod = selectedPeriod,
            calendarMonthLabel = YearMonth.from(now).format(monthFormatter)
                .replaceFirstChar { it.titlecase() },
            achievements = defaultAchievements(),
        )
        val (periodStart, previousStart) = periodBoundaries(selectedPeriod, now)
        val periodRecords = periodStart?.let { start ->
            records.filter { it.timestamp >= start.toInstant().toEpochMilli() }
        } ?: records
        val previousRecords = if (periodStart != null && previousStart != null) {
            records.filter { record ->
                record.timestamp >= previousStart.toInstant().toEpochMilli() &&
                    record.timestamp < periodStart.toInstant().toEpochMilli()
            }
        } else emptyList()
        val sessions = periodRecords.groupBy { it.sessionId }.values
            .map { attempts ->
                SessionPoint(
                    timestamp = attempts.minOf { it.timestamp },
                    averageMillis = attempts.map { it.holdDurationMillis }.average().toLong(),
                    maximumMillis = attempts.maxOf { it.holdDurationMillis },
                )
            }
            .sortedBy { it.timestamp }
        val months = records.groupBy { record ->
            YearMonth.from(Instant.ofEpochMilli(record.timestamp).atZone(zone))
        }.map { (yearMonth, attempts) ->
            MonthPoint(
                yearMonth = yearMonth,
                label = yearMonth.format(monthFormatter).replaceFirstChar { it.titlecase() },
                averageMillis = attempts.map { it.holdDurationMillis }.average().toLong(),
                maximumMillis = attempts.maxOf { it.holdDurationMillis },
                attemptCount = attempts.size,
                sessionCount = attempts.map { it.sessionId }.distinct().size,
            )
        }.sortedBy { it.yearMonth }
        val comfortableRecords = records.filter { it.comfortRating == 1 || it.comfortRating == 2 }
        val currentMonth = YearMonth.from(now)
        val calendarDays = records
            .filter { YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) == currentMonth }
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).dayOfMonth }
            .map { (day, attempts) ->
                CalendarDay(
                    dayOfMonth = day,
                    sessionCount = attempts.map { it.sessionId }.distinct().size,
                    bestMillis = attempts.maxOf { it.holdDurationMillis },
                )
            }
            .sortedBy { it.dayOfMonth }
        val currentAverage = periodRecords.takeIf { it.isNotEmpty() }
            ?.map { it.holdDurationMillis }?.average()
        val previousAverage = previousRecords.takeIf { it.isNotEmpty() }
            ?.map { it.holdDurationMillis }?.average()
        val comparison = if (currentAverage != null && previousAverage != null && previousAverage > 0.0) {
            ((currentAverage - previousAverage) / previousAverage * 100.0).toFloat()
        } else null
        val allSessions = records.groupBy { it.sessionId }.map { (id, attempts) ->
            SessionSummary(
                sessionId = id,
                timestamp = attempts.minOf { it.timestamp },
                attempts = attempts.sortedBy { it.attemptNumber },
                averageMillis = attempts.map { it.holdDurationMillis }.average().toLong(),
                maximumMillis = attempts.maxOf { it.holdDurationMillis },
                note = attempts.firstNotNullOfOrNull { attempt ->
                    attempt.sessionNote.takeIf { it.isNotBlank() }
                }.orEmpty(),
            )
        }.sortedByDescending { it.timestamp }
        val comfortableCount = records.count { it.comfortRating == 1 || it.comfortRating == 2 }
        val stableSession = allSessions.any { summary ->
            val min = summary.attempts.minOfOrNull { it.holdDurationMillis } ?: 0L
            val max = summary.maximumMillis
            summary.attempts.size >= 3 && max > 0L && (max - min).toFloat() / max <= .15f
        }
        val achievements = listOf(
            Achievement("Первый спокойный шаг", "Завершить первую тренировку", allSessions.isNotEmpty()),
            Achievement("Ритм практики", "Провести 5 тренировок", allSessions.size >= 5),
            Achievement("Комфорт прежде всего", "Отметить 5 комфортных подходов", comfortableCount >= 5),
            Achievement("Ровное дыхание", "Три стабильных подхода в одной сессии", stableSession),
        )
        return ProgressUiState(
            sessions = sessions,
            months = months,
            records = records.sortedByDescending { it.timestamp },
            personalBestMillis = records.maxOf { it.holdDurationMillis },
            recentAverageMillis = records.sortedByDescending { it.timestamp }
                .take(10)
                .map { it.holdDurationMillis }
                .average()
                .toLong(),
            comfortableAverageMillis = comfortableRecords
                .takeIf { it.isNotEmpty() }
                ?.map { it.holdDurationMillis }
                ?.average()
                ?.toLong()
                ?: 0L,
            ratedAttemptCount = records.count { it.comfortRating != 0 },
            selectedPeriod = selectedPeriod,
            comparisonPercent = comparison,
            calendarMonthLabel = currentMonth.format(monthFormatter).replaceFirstChar { it.titlecase() },
            calendarDays = calendarDays,
            achievements = achievements,
            sessionSummaries = allSessions,
        )
}

private fun defaultAchievements() = listOf(
    Achievement("Первый спокойный шаг", "Завершить первую тренировку", false),
    Achievement("Ритм практики", "Провести 5 тренировок", false),
    Achievement("Комфорт прежде всего", "Отметить 5 комфортных подходов", false),
    Achievement("Ровное дыхание", "Три стабильных подхода в одной сессии", false),
)

private fun periodBoundaries(
    period: ProgressPeriod,
    now: ZonedDateTime,
): Pair<ZonedDateTime?, ZonedDateTime?> = when (period) {
    ProgressPeriod.Week -> now.minusDays(7) to now.minusDays(14)
    ProgressPeriod.Month -> now.withDayOfMonth(1).toLocalDate().atStartOfDay(now.zone).let { start ->
        start to start.minusMonths(1)
    }
    ProgressPeriod.Quarter -> now.minusMonths(3) to now.minusMonths(6)
    ProgressPeriod.Year -> now.withDayOfYear(1).toLocalDate().atStartOfDay(now.zone).let { start ->
        start to start.minusYears(1)
    }
    ProgressPeriod.All -> null to null
}
