package com.zenhold.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.domain.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class SessionPoint(
    val timestamp: Long,
    val averageMillis: Long,
    val maximumMillis: Long,
)

data class ProgressUiState(
    val sessions: List<SessionPoint> = emptyList(),
    val records: List<BreathHoldRecord> = emptyList(),
    val personalBestMillis: Long = 0L,
    val recentAverageMillis: Long = 0L,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    repository: RecordRepository,
) : ViewModel() {
    val state: StateFlow<ProgressUiState> = repository.observeRecords()
        .map(::toProgressState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    private fun toProgressState(records: List<BreathHoldRecord>): ProgressUiState {
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
        return ProgressUiState(
            sessions = sessions,
            records = records.sortedByDescending { it.timestamp },
            personalBestMillis = records.maxOf { it.holdDurationMillis },
            recentAverageMillis = records.takeLast(10).map { it.holdDurationMillis }.average().toLong(),
        )
    }
}
