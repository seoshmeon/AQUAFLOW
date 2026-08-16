package com.zenhold.app.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenhold.app.audio.TrainingAudioController
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.TrainingState
import com.zenhold.app.domain.repository.RecordRepository
import com.zenhold.app.util.ElapsedRealtimeClock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BreathTrainingViewModel @Inject constructor(
    private val records: RecordRepository,
    private val audio: TrainingAudioController,
    private val clock: ElapsedRealtimeClock,
) : ViewModel() {
    private val _state = MutableStateFlow<TrainingState>(TrainingState.Idle)
    val state: StateFlow<TrainingState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var hiddenHoldTickerJob: Job? = null
    private var holdStartedAtMillis: Long? = null
    private var hiddenElapsedMillis: Long = 0L
    private var settings = TrainingSettings()
    private var currentAttempt = 1
    private var sessionId = ""
    private val sessionResults = mutableListOf<Long>()

    fun startTraining(selectedSettings: TrainingSettings) {
        if (_state.value !is TrainingState.Idle && _state.value !is TrainingState.Finished) return

        settings = selectedSettings
        currentAttempt = 1
        sessionId = UUID.randomUUID().toString()
        sessionResults.clear()
        startPreparation()
    }

    private fun startPreparation() {
        timerJob?.cancel()
        val startedAt = clock.nowMillis()
        _state.value = TrainingState.Preparation(
            remainingMillis = PREPARATION_MILLIS,
            totalMillis = PREPARATION_MILLIS,
            attempt = currentAttempt,
            totalAttempts = settings.attemptCount,
        )
        viewModelScope.launch { runCatching { audio.startPreparationMusic() } }

        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = (PREPARATION_MILLIS - (clock.nowMillis() - startedAt)).coerceAtLeast(0L)
                _state.value = TrainingState.Preparation(
                    remainingMillis = remaining,
                    totalMillis = PREPARATION_MILLIS,
                    attempt = currentAttempt,
                    totalAttempts = settings.attemptCount,
                )
                if (remaining == 0L) break
                delay(TICK_MILLIS)
            }
            beginHolding()
        }
    }

    private suspend fun beginHolding() {
        audio.stopPreparationMusic()
        runCatching { audio.playTransitionCue() }
        runCatching { audio.startHoldingMusic() }
        holdStartedAtMillis = clock.nowMillis()
        hiddenElapsedMillis = 0L
        _state.value = TrainingState.Holding(currentAttempt, settings.attemptCount)

        // Kept private by design: this timer must never become observable UI state.
        hiddenHoldTickerJob?.cancel()
        hiddenHoldTickerJob = viewModelScope.launch {
            while (true) {
                hiddenElapsedMillis = clock.nowMillis() - (holdStartedAtMillis ?: break)
                delay(HIDDEN_TIMER_TICK_MILLIS)
            }
        }
    }

    /** Idempotent: rapid extra taps cannot create duplicate records or recovery timers. */
    fun stopHolding() {
        val holdingState = _state.value as? TrainingState.Holding ?: return
        val startedAt = holdStartedAtMillis ?: return
        holdStartedAtMillis = null
        hiddenHoldTickerJob?.cancel()
        audio.stopHoldingMusic()

        val duration = (clock.nowMillis() - startedAt).coerceAtLeast(1L)
        val completedAttempt = holdingState.attempt
        sessionResults += duration

        viewModelScope.launch {
            runCatching {
                records.save(
                    BreathHoldRecord(
                        holdDurationMillis = duration,
                        recoveryDurationMillis = settings.recoveryDurationMillis,
                        timestamp = System.currentTimeMillis(),
                        sessionId = sessionId,
                        attemptNumber = completedAttempt,
                    ),
                )
            }
        }
        startRecovery(duration, completedAttempt)
    }

    private fun startRecovery(holdDuration: Long, completedAttempt: Int) {
        timerJob?.cancel()
        val startedAt = clock.nowMillis()
        _state.value = recoveryState(holdDuration, settings.recoveryDurationMillis, completedAttempt)
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = (settings.recoveryDurationMillis - (clock.nowMillis() - startedAt))
                    .coerceAtLeast(0L)
                _state.value = recoveryState(holdDuration, remaining, completedAttempt)
                if (remaining == 0L) break
                delay(TICK_MILLIS)
            }

            if (completedAttempt >= settings.attemptCount) {
                _state.value = TrainingState.Finished(sessionResults.toList())
            } else {
                currentAttempt = completedAttempt + 1
                beginHolding()
            }
        }
    }

    private fun recoveryState(holdDuration: Long, remaining: Long, attempt: Int) =
        TrainingState.Recovering(
            holdDurationMillis = holdDuration,
            remainingMillis = remaining,
            totalRecoveryMillis = settings.recoveryDurationMillis,
            completedAttempt = attempt,
            totalAttempts = settings.attemptCount,
        )

    fun finishNow() {
        timerJob?.cancel()
        hiddenHoldTickerJob?.cancel()
        holdStartedAtMillis = null
        audio.stopPreparationMusic()
        audio.stopHoldingMusic()
        _state.value = if (sessionResults.isEmpty()) TrainingState.Idle
        else TrainingState.Finished(sessionResults.toList())
    }

    fun returnHome() {
        finishNow()
        sessionResults.clear()
        _state.value = TrainingState.Idle
    }

    override fun onCleared() {
        timerJob?.cancel()
        hiddenHoldTickerJob?.cancel()
        audio.stopPreparationMusic()
        audio.stopHoldingMusic()
        super.onCleared()
    }

    companion object {
        const val PREPARATION_MILLIS = 180_000L
        private const val TICK_MILLIS = 100L
        private const val HIDDEN_TIMER_TICK_MILLIS = 50L
    }
}
