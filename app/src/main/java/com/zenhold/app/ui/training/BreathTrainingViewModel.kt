package com.zenhold.app.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenhold.app.audio.TrainingAudioController
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.data.local.TrainingSessionEntity
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.TrainingState
import com.zenhold.app.domain.model.SessionCheckIn
import com.zenhold.app.domain.model.ComfortRating
import com.zenhold.app.domain.model.ReadinessLevel
import com.zenhold.app.domain.model.RecoveryStopReason
import com.zenhold.app.domain.model.TrainingProgram
import com.zenhold.app.domain.model.buildAdaptiveTrainingPlan
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
    private val _resumableSession = MutableStateFlow<TrainingSessionEntity?>(null)
    val resumableSession: StateFlow<TrainingSessionEntity?> = _resumableSession.asStateFlow()

    private var timerJob: Job? = null
    private var hiddenHoldTickerJob: Job? = null
    private var holdGuardJob: Job? = null
    private var sessionCreateJob: Job? = null
    private var holdStartedAtMillis: Long? = null
    private var hiddenElapsedMillis: Long = 0L
    private var firstDiscomfortElapsedMillis: Long = 0L
    private var recoveryDeadlineMillis: Long = 0L
    private var recoveryTotalMillis: Long = 0L
    private var recoveryStartedAtMillis: Long = 0L
    private var settings = TrainingSettings()
    private var checkIn = SessionCheckIn()
    private var currentAttempt = 1
    private var sessionId = ""
    private var sessionFinalized = false
    private val sessionResults = mutableListOf<Long>()
    private val savedRecordIds = mutableMapOf<Int, Long>()
    private val pendingComfortRatings = mutableMapOf<Int, ComfortRating>()
    private val sessionRatings = mutableMapOf<Int, ComfortRating>()
    private val pendingStopReasons = mutableMapOf<Int, RecoveryStopReason>()
    private val pendingActualRecovery = mutableMapOf<Int, Long>()
    private var currentReadiness = ReadinessLevel.Optimal

    init {
        viewModelScope.launch { _resumableSession.value = records.getActiveSession() }
    }

    fun startTraining(selectedSettings: TrainingSettings, sessionCheckIn: SessionCheckIn = SessionCheckIn()) {
        if (_state.value !is TrainingState.Idle && _state.value !is TrainingState.Finished) return

        val adaptivePlan = buildAdaptiveTrainingPlan(selectedSettings, sessionCheckIn)
        if (!adaptivePlan.canStart) return

        _resumableSession.value?.let { previous ->
            viewModelScope.launch {
                records.finishSession(
                    previous.sessionId,
                    TrainingSessionEntity.STATUS_STOPPED,
                    "Начата новая тренировка",
                )
            }
            _resumableSession.value = null
        }
        settings = adaptivePlan.settings
        currentReadiness = adaptivePlan.readiness
        audio.configure(
            musicVolumePercent = settings.musicVolumePercent,
            cueVolumePercent = settings.cueVolumePercent,
            vibrationEnabled = settings.vibrationEnabled,
            cueStyle = settings.cueStyle,
            vibrationStrength = settings.vibrationStrength,
        )
        checkIn = sessionCheckIn
        currentAttempt = 1
        sessionId = UUID.randomUUID().toString()
        sessionFinalized = false
        sessionResults.clear()
        savedRecordIds.clear()
        pendingComfortRatings.clear()
        sessionRatings.clear()
        pendingStopReasons.clear()
        pendingActualRecovery.clear()
        sessionCreateJob = viewModelScope.launch {
            records.startSession(
                TrainingSessionEntity(
                    sessionId = sessionId,
                    startedAt = System.currentTimeMillis(),
                    plannedAttempts = settings.attemptCount,
                    preparationDurationMillis = settings.preparationDurationMillis,
                    recoveryDurationMillis = settings.recoveryDurationMillis,
                    energyLevel = checkIn.energyLevel,
                    stressLevel = checkIn.stressLevel,
                    sleepQuality = checkIn.sleepQuality,
                    program = checkIn.program.name.uppercase(),
                    readinessLevel = currentReadiness.name.uppercase(),
                ),
            )
        }
        startPreparation()
    }

    /** Resumes only from a fresh preparation phase; an interrupted hold is never resumed. */
    fun resumeTraining(selectedSettings: TrainingSettings) {
        val session = _resumableSession.value ?: return
        if (_state.value !is TrainingState.Idle) return
        _state.value = TrainingState.Preparation(
            remainingMillis = session.preparationDurationMillis,
            totalMillis = session.preparationDurationMillis,
            attempt = (session.completedAttempts + 1).coerceAtMost(session.plannedAttempts),
            totalAttempts = session.plannedAttempts,
        )
        viewModelScope.launch {
            val previousRecords = records.getSessionRecords(session.sessionId)
            if (session.completedAttempts >= session.plannedAttempts) {
                records.finishSession(session.sessionId, TrainingSessionEntity.STATUS_COMPLETED)
                _resumableSession.value = null
                _state.value = TrainingState.Finished(previousRecords.map { it.holdDurationMillis })
                return@launch
            }
            settings = selectedSettings.copy(
                attemptCount = session.plannedAttempts,
                preparationDurationMillis = session.preparationDurationMillis,
                recoveryDurationMillis = session.recoveryDurationMillis,
            )
            audio.configure(
                settings.musicVolumePercent,
                settings.cueVolumePercent,
                settings.vibrationEnabled,
                settings.cueStyle,
                settings.vibrationStrength,
            )
            checkIn = SessionCheckIn(
                energyLevel = session.energyLevel,
                stressLevel = session.stressLevel,
                sleepQuality = session.sleepQuality,
                program = TrainingProgram.entries.firstOrNull {
                    it.name.equals(session.program, ignoreCase = true)
                } ?: TrainingProgram.Adaptive,
            )
            currentReadiness = ReadinessLevel.entries.firstOrNull {
                it.name.equals(session.readinessLevel, ignoreCase = true)
            } ?: ReadinessLevel.Optimal
            sessionId = session.sessionId
            currentAttempt = session.completedAttempts + 1
            sessionFinalized = false
            sessionCreateJob = null
            sessionResults.clear()
            sessionResults += previousRecords.map { it.holdDurationMillis }
            savedRecordIds.clear()
            previousRecords.forEach { savedRecordIds[it.attemptNumber] = it.id }
            pendingComfortRatings.clear()
            sessionRatings.clear()
            pendingStopReasons.clear()
            pendingActualRecovery.clear()
            _resumableSession.value = null
            startPreparation()
        }
    }

    fun discardResumableSession() {
        val session = _resumableSession.value ?: return
        _resumableSession.value = null
        viewModelScope.launch {
            records.finishSession(
                session.sessionId,
                TrainingSessionEntity.STATUS_STOPPED,
                "Незавершённая тренировка закрыта пользователем",
            )
        }
    }

    private fun startPreparation() {
        timerJob?.cancel()
        val startedAt = clock.nowMillis()
        _state.value = TrainingState.Preparation(
            remainingMillis = settings.preparationDurationMillis,
            totalMillis = settings.preparationDurationMillis,
            attempt = currentAttempt,
            totalAttempts = settings.attemptCount,
        )
        if (settings.preparationMusicEnabled) {
            viewModelScope.launch { runCatching { audio.startPreparationMusic() } }
        }

        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = (settings.preparationDurationMillis - (clock.nowMillis() - startedAt)).coerceAtLeast(0L)
                _state.value = TrainingState.Preparation(
                    remainingMillis = remaining,
                    totalMillis = settings.preparationDurationMillis,
                    attempt = currentAttempt,
                    totalAttempts = settings.attemptCount,
                )
                if (remaining == 0L) break
                delay(TICK_MILLIS)
            }
            beginHolding()
        }
    }

    fun skipPreparation() {
        if (_state.value !is TrainingState.Preparation) return
        timerJob?.cancel()
        timerJob = viewModelScope.launch { beginHolding() }
    }

    private suspend fun beginHolding() {
        audio.stopPreparationMusic()
        runCatching { audio.playTransitionCue() }
        if (settings.holdingMusicEnabled) runCatching { audio.startHoldingMusic() }
        holdStartedAtMillis = clock.nowMillis()
        hiddenElapsedMillis = 0L
        firstDiscomfortElapsedMillis = 0L
        _state.value = TrainingState.Holding(
            attempt = currentAttempt,
            totalAttempts = settings.attemptCount,
            fullScreenGesture = settings.fullScreenHoldGesture,
            gestureEnabled = false,
        )
        holdGuardJob?.cancel()
        holdGuardJob = viewModelScope.launch {
            delay(HOLD_GESTURE_GUARD_MILLIS)
            val holding = _state.value as? TrainingState.Holding ?: return@launch
            _state.value = holding.copy(gestureEnabled = true)
        }

        // Kept private by design: this timer must never become observable UI state.
        hiddenHoldTickerJob?.cancel()
        hiddenHoldTickerJob = viewModelScope.launch {
            while (true) {
                hiddenElapsedMillis = clock.nowMillis() - (holdStartedAtMillis ?: break)
                delay(HIDDEN_TIMER_TICK_MILLIS)
            }
        }
    }

    /** Records the first urge without exposing elapsed time to the holding UI. */
    fun markFirstDiscomfort() {
        val holding = _state.value as? TrainingState.Holding ?: return
        if (!holding.gestureEnabled || holding.firstDiscomfortMarked) return
        val startedAt = holdStartedAtMillis ?: return
        firstDiscomfortElapsedMillis = (clock.nowMillis() - startedAt).coerceAtLeast(1L)
        _state.value = holding.copy(firstDiscomfortMarked = true)
    }

    /** Idempotent: rapid extra taps cannot create duplicate records or recovery timers. */
    fun stopHolding() {
        val holdingState = _state.value as? TrainingState.Holding ?: return
        if (!holdingState.gestureEnabled) return
        val startedAt = holdStartedAtMillis ?: return
        holdStartedAtMillis = null
        hiddenHoldTickerJob?.cancel()
        holdGuardJob?.cancel()
        audio.stopHoldingMusic()

        val duration = (clock.nowMillis() - startedAt).coerceAtLeast(1L)
        val firstDiscomfort = firstDiscomfortElapsedMillis.coerceAtMost(duration)
        val completedAttempt = holdingState.attempt
        sessionResults += duration

        viewModelScope.launch {
            sessionCreateJob?.join()
            runCatching {
                val recordId = records.save(
                    BreathHoldRecord(
                        holdDurationMillis = duration,
                        recoveryDurationMillis = settings.recoveryDurationMillis,
                        timestamp = System.currentTimeMillis(),
                        sessionId = sessionId,
                        attemptNumber = completedAttempt,
                        energyLevel = checkIn.energyLevel,
                        stressLevel = checkIn.stressLevel,
                        firstDiscomfortMillis = firstDiscomfort,
                    ),
                )
                savedRecordIds[completedAttempt] = recordId
                records.updateSessionProgress(sessionId, completedAttempt)
                val rating = pendingComfortRatings[completedAttempt]
                val reason = pendingStopReasons[completedAttempt]
                if (rating != null && reason != null) {
                    records.updateFeedback(recordId, rating.storedValue, reason.storedValue)
                    pendingComfortRatings.remove(completedAttempt)
                    pendingStopReasons.remove(completedAttempt)
                }
                pendingActualRecovery.remove(completedAttempt)?.let { actual ->
                    records.updateActualRecovery(recordId, actual)
                }
            }
        }
        firstDiscomfortElapsedMillis = 0L
        startRecovery(duration, completedAttempt)
    }

    private fun startRecovery(holdDuration: Long, completedAttempt: Int) {
        timerJob?.cancel()
        recoveryTotalMillis = settings.recoveryDurationMillis
        recoveryStartedAtMillis = clock.nowMillis()
        recoveryDeadlineMillis = clock.nowMillis() + recoveryTotalMillis
        _state.value = recoveryState(
            holdDuration,
            recoveryTotalMillis,
            completedAttempt,
            recoveryTotalMillis,
        )
        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = (recoveryDeadlineMillis - clock.nowMillis()).coerceAtLeast(0L)
                _state.value = recoveryState(holdDuration, remaining, completedAttempt, recoveryTotalMillis)
                if (remaining == 0L) break
                delay(TICK_MILLIS)
            }
        }
    }

    private fun recoveryState(holdDuration: Long, remaining: Long, attempt: Int, total: Long) =
        TrainingState.Recovering(
            holdDurationMillis = holdDuration,
            remainingMillis = remaining,
            totalRecoveryMillis = total,
            completedAttempt = attempt,
            totalAttempts = settings.attemptCount,
            comfortRating = (_state.value as? TrainingState.Recovering)?.comfortRating,
            stopReason = (_state.value as? TrainingState.Recovering)?.stopReason,
        )

    fun extendRecovery() {
        val recovery = _state.value as? TrainingState.Recovering ?: return
        recoveryDeadlineMillis += RECOVERY_EXTENSION_MILLIS
        recoveryTotalMillis += RECOVERY_EXTENSION_MILLIS
        _state.value = recovery.copy(
            remainingMillis = recovery.remainingMillis + RECOVERY_EXTENSION_MILLIS,
            totalRecoveryMillis = recoveryTotalMillis,
        )
    }

    fun completeRecoveryEarly() {
        val recovery = _state.value as? TrainingState.Recovering ?: return
        if (recovery.comfortRating == null || recovery.stopReason == null) return
        timerJob?.cancel()
        captureActualRecovery(recovery.completedAttempt)
        timerJob = viewModelScope.launch { advanceAfterRecovery(recovery.completedAttempt) }
    }

    private suspend fun advanceAfterRecovery(completedAttempt: Int) {
        if (completedAttempt >= settings.attemptCount) {
            runCatching { audio.playTransitionCue() }
            finalizeSession(TrainingSessionEntity.STATUS_COMPLETED)
            val hadHardAttempt = sessionRatings.values.any { it == ComfortRating.TooHard }
            _state.value = TrainingState.Finished(
                resultsMillis = sessionResults.toList(),
                coachMessage = if (hadHardAttempt) {
                    "Сегодня нагрузка была выше комфортной. Это полезный сигнал, а не неудача."
                } else {
                    "Сессия завершена спокойно. Регулярность и ровные ощущения важнее единичного максимума."
                },
                nextSessionAdvice = if (currentReadiness == ReadinessLevel.Optimal && !hadHardAttempt) {
                    "Следующую сессию оставьте такой же — нагрузка растёт только после устойчивой серии."
                } else {
                    "Перед следующей практикой восстановитесь и снова оцените сон и самочувствие."
                },
            )
        } else {
            currentAttempt = completedAttempt + 1
            beginHolding()
        }
    }

    fun setComfortRating(rating: ComfortRating) {
        val recovery = _state.value as? TrainingState.Recovering ?: return
        _state.value = recovery.copy(comfortRating = rating)
        pendingComfortRatings[recovery.completedAttempt] = rating
        sessionRatings[recovery.completedAttempt] = rating
        persistFeedbackIfComplete(recovery.completedAttempt)
    }

    fun setStopReason(reason: RecoveryStopReason) {
        val recovery = _state.value as? TrainingState.Recovering ?: return
        _state.value = recovery.copy(stopReason = reason)
        pendingStopReasons[recovery.completedAttempt] = reason
        persistFeedbackIfComplete(recovery.completedAttempt)
    }

    private fun persistFeedbackIfComplete(attempt: Int) {
        val rating = pendingComfortRatings[attempt] ?: return
        val reason = pendingStopReasons[attempt] ?: return
        val recordId = savedRecordIds[attempt] ?: return
        viewModelScope.launch {
            runCatching { records.updateFeedback(recordId, rating.storedValue, reason.storedValue) }
            pendingComfortRatings.remove(attempt)
            pendingStopReasons.remove(attempt)
        }
    }

    private fun captureActualRecovery(attempt: Int) {
        val actual = (clock.nowMillis() - recoveryStartedAtMillis).coerceIn(0L, 600_000L)
        val recordId = savedRecordIds[attempt]
        if (recordId == null) {
            pendingActualRecovery[attempt] = actual
        } else {
            viewModelScope.launch { records.updateActualRecovery(recordId, actual) }
        }
    }

    fun finishNow() {
        val stateBeforeFinish = _state.value
        (stateBeforeFinish as? TrainingState.Recovering)?.let {
            captureActualRecovery(it.completedAttempt)
        }
        timerJob?.cancel()
        hiddenHoldTickerJob?.cancel()
        holdGuardJob?.cancel()
        holdStartedAtMillis = null
        audio.stopPreparationMusic()
        audio.stopHoldingMusic()
        if (stateBeforeFinish !is TrainingState.Idle &&
            stateBeforeFinish !is TrainingState.Finished &&
            stateBeforeFinish !is TrainingState.Interrupted
        ) {
            finalizeSession(TrainingSessionEntity.STATUS_STOPPED, "Остановлено пользователем")
        }
        _state.value = if (sessionResults.isEmpty()) TrainingState.Idle
        else TrainingState.Finished(sessionResults.toList())
    }

    fun interruptForSafety() {
        val activeState = _state.value
        if (activeState is TrainingState.Idle ||
            activeState is TrainingState.Finished ||
            activeState is TrainingState.Interrupted
        ) return
        (activeState as? TrainingState.Recovering)?.let {
            captureActualRecovery(it.completedAttempt)
        }
        timerJob?.cancel()
        hiddenHoldTickerJob?.cancel()
        holdGuardJob?.cancel()
        holdStartedAtMillis = null
        audio.stopPreparationMusic()
        audio.stopHoldingMusic()
        finalizeSession(
            TrainingSessionEntity.STATUS_INTERRUPTED,
            "Приложение свёрнуто или тренировка прервана системой",
        )
        _state.value = TrainingState.Interrupted(
            resultsMillis = sessionResults.toList(),
            message = if (activeState is TrainingState.Holding) {
                "Задержка остановлена, потому что приложение было свёрнуто или прервано. Текущий подход не сохранён."
            } else {
                "Тренировка безопасно остановлена после сворачивания или внешнего прерывания. Уже завершённые подходы сохранены."
            },
        )
    }

    fun returnHome() {
        finishNow()
        sessionResults.clear()
        _state.value = TrainingState.Idle
    }

    override fun onCleared() {
        timerJob?.cancel()
        hiddenHoldTickerJob?.cancel()
        holdGuardJob?.cancel()
        audio.stopPreparationMusic()
        audio.stopHoldingMusic()
        super.onCleared()
    }

    private fun finalizeSession(status: String, reason: String = "") {
        if (sessionFinalized || sessionId.isBlank()) return
        sessionFinalized = true
        viewModelScope.launch {
            sessionCreateJob?.join()
            records.finishSession(sessionId, status, reason)
        }
    }

    companion object {
        const val PREPARATION_MILLIS = 30_000L
        private const val TICK_MILLIS = 100L
        private const val HIDDEN_TIMER_TICK_MILLIS = 50L
        private const val HOLD_GESTURE_GUARD_MILLIS = 1_500L
        private const val RECOVERY_EXTENSION_MILLIS = 30_000L
    }
}
