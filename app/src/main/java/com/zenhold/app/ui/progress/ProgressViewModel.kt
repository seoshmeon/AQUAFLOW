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
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    repository: RecordRepository,
) : ViewModel() {
    val state: StateFlow<ProgressUiState> = repository.observeRecords()
        .map(::buildProgressState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())
}

internal fun buildProgressState(records: List<BreathHoldRecord>): ProgressUiState {
        if (records.isEmpty()) return ProgressUiState()
        val sessions = records.groupBy { it.sessionId }.values
            .map { attempts ->
                SessionPoint(
                    timestamp = attempts.minOf { it.timestamp },
                    averageMillis = attempts.map { it.holdDurationMillis }.average().toLong(),
                    maximumMillis = attempts.maxOf { it.holdDurationMillis },
                )
            }
            .sortedBy { it.timestamp }
        val zone = ZoneId.systemDefault()
        val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru-RU"))
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
        return ProgressUiState(
            sessions = sessions,
            months = months,
            records = records.sortedByDescending { it.timestamp },
            personalBestMillis = records.maxOf { it.holdDurationMillis },
            recentAverageMillis = records.takeLast(10).map { it.holdDurationMillis }.average().toLong(),
        )
}
