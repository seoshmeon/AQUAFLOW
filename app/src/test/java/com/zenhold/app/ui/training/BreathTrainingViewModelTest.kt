package com.zenhold.app.ui.training

import com.zenhold.app.audio.TrainingAudioController
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.data.local.TrainingSessionEntity
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.TrainingState
import com.zenhold.app.domain.model.CueStyle
import com.zenhold.app.domain.model.VibrationStrength
import com.zenhold.app.domain.model.ComfortRating
import com.zenhold.app.domain.model.RecoveryStopReason
import com.zenhold.app.domain.repository.RecordRepository
import com.zenhold.app.util.ElapsedRealtimeClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BreathTrainingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun completeAttempt_keepsHoldTimePrivate_savesRecord_andFinishesAfterRecovery() = runTest(dispatcher) {
        val repository = FakeRecordRepository()
        val viewModel = BreathTrainingViewModel(
            records = repository,
            audio = FakeAudioController(),
            clock = ElapsedRealtimeClock { testScheduler.currentTime },
        )

        viewModel.startTraining(TrainingSettings(attemptCount = 1, recoveryDurationMillis = 30_000L))
        assertTrue(viewModel.state.value is TrainingState.Preparation)

        advanceTimeBy(BreathTrainingViewModel.PREPARATION_MILLIS)
        runCurrent()
        assertEquals(
            TrainingState.Holding(
                attempt = 1,
                totalAttempts = 1,
                fullScreenGesture = true,
                gestureEnabled = false,
            ),
            viewModel.state.value,
        )

        advanceTimeBy(42_000L)
        viewModel.stopHolding()
        runCurrent()
        val recovery = viewModel.state.value as TrainingState.Recovering
        assertEquals(42_000L, recovery.holdDurationMillis)
        assertEquals(1, repository.saved.size)
        assertEquals(42_000L, repository.saved.single().holdDurationMillis)

        advanceTimeBy(30_000L)
        runCurrent()
        assertTrue(viewModel.state.value is TrainingState.Recovering)
        viewModel.setComfortRating(ComfortRating.Comfortable)
        viewModel.setStopReason(RecoveryStopReason.ComfortableLimit)
        viewModel.completeRecoveryEarly()
        runCurrent()
        assertTrue(viewModel.state.value is TrainingState.Finished)
        assertEquals(TrainingSessionEntity.STATUS_COMPLETED, repository.finishedStatus)
    }

    @Test
    fun finishNow_duringPreparation_returnsIdleWithoutSaving() = runTest(dispatcher) {
        val repository = FakeRecordRepository()
        val viewModel = BreathTrainingViewModel(
            records = repository,
            audio = FakeAudioController(),
            clock = ElapsedRealtimeClock { testScheduler.currentTime },
        )

        viewModel.startTraining(TrainingSettings())
        advanceTimeBy(10_000L)
        viewModel.finishNow()
        runCurrent()

        assertEquals(TrainingState.Idle, viewModel.state.value)
        assertTrue(repository.saved.isEmpty())
        assertEquals(TrainingSessionEntity.STATUS_STOPPED, repository.finishedStatus)
    }

    @Test
    fun backgroundInterruption_duringPreparation_entersSafeInterruptedState() = runTest(dispatcher) {
        val repository = FakeRecordRepository()
        val viewModel = BreathTrainingViewModel(
            records = repository,
            audio = FakeAudioController(),
            clock = ElapsedRealtimeClock { testScheduler.currentTime },
        )

        viewModel.startTraining(TrainingSettings())
        advanceTimeBy(5_000L)
        viewModel.interruptForSafety()
        runCurrent()

        assertTrue(viewModel.state.value is TrainingState.Interrupted)
        assertTrue(repository.saved.isEmpty())
        assertEquals(TrainingSessionEntity.STATUS_INTERRUPTED, repository.finishedStatus)
    }

    @Test
    fun repeatedStopHolding_savesOnlyOneRecord() = runTest(dispatcher) {
        val repository = FakeRecordRepository()
        val viewModel = BreathTrainingViewModel(
            records = repository,
            audio = FakeAudioController(),
            clock = ElapsedRealtimeClock { testScheduler.currentTime },
        )

        viewModel.startTraining(TrainingSettings(attemptCount = 1, recoveryDurationMillis = 30_000L))
        advanceTimeBy(BreathTrainingViewModel.PREPARATION_MILLIS)
        runCurrent()
        advanceTimeBy(15_000L)

        viewModel.stopHolding()
        viewModel.stopHolding()
        runCurrent()

        assertEquals(1, repository.saved.size)
        assertTrue(viewModel.state.value is TrainingState.Recovering)
    }

    @Test
    fun recovery_canBeExtended_andCompletedEarly() = runTest(dispatcher) {
        val repository = FakeRecordRepository()
        val viewModel = BreathTrainingViewModel(repository, FakeAudioController(), ElapsedRealtimeClock { testScheduler.currentTime })
        viewModel.startTraining(TrainingSettings(attemptCount = 1, recoveryDurationMillis = 30_000L))
        advanceTimeBy(BreathTrainingViewModel.PREPARATION_MILLIS + 1_600L)
        runCurrent()
        viewModel.stopHolding()
        runCurrent()

        viewModel.extendRecovery()
        assertEquals(60_000L, (viewModel.state.value as TrainingState.Recovering).totalRecoveryMillis)
        viewModel.setComfortRating(ComfortRating.Easy)
        viewModel.setStopReason(RecoveryStopReason.ComfortableLimit)
        viewModel.completeRecoveryEarly()
        runCurrent()

        assertTrue(viewModel.state.value is TrainingState.Finished)
    }

    @Test
    fun firstDiscomfort_isStoredPrivately_withoutExposingElapsedTime() = runTest(dispatcher) {
        val repository = FakeRecordRepository()
        val viewModel = BreathTrainingViewModel(repository, FakeAudioController(), ElapsedRealtimeClock { testScheduler.currentTime })
        viewModel.startTraining(TrainingSettings(attemptCount = 1, recoveryDurationMillis = 30_000L))
        advanceTimeBy(BreathTrainingViewModel.PREPARATION_MILLIS + 1_600L)
        runCurrent()
        advanceTimeBy(18_000L)
        viewModel.markFirstDiscomfort()
        advanceTimeBy(12_000L)
        viewModel.stopHolding()
        runCurrent()

        assertEquals(19_600L, repository.saved.single().firstDiscomfortMillis)
        assertTrue(viewModel.state.value is TrainingState.Recovering)
    }

    @Test
    fun activeSession_resumesFromNextAttemptWithFreshPreparation() = runTest(dispatcher) {
        val active = TrainingSessionEntity(
            sessionId = "active",
            startedAt = 1L,
            plannedAttempts = 3,
            completedAttempts = 1,
            preparationDurationMillis = 30_000L,
            recoveryDurationMillis = 60_000L,
            energyLevel = 4,
            stressLevel = 2,
        )
        val repository = FakeRecordRepository(listOf(active))
        val viewModel = BreathTrainingViewModel(repository, FakeAudioController(), ElapsedRealtimeClock { testScheduler.currentTime })
        runCurrent()

        viewModel.resumeTraining(TrainingSettings())
        runCurrent()

        val preparation = viewModel.state.value as TrainingState.Preparation
        assertEquals(2, preparation.attempt)
        assertEquals(3, preparation.totalAttempts)
        viewModel.finishNow()
        runCurrent()
    }

    @Test
    fun voiceGuidance_runsOnlyAroundPreparationAndRecovery() = runTest(dispatcher) {
        val audio = FakeAudioController()
        val viewModel = BreathTrainingViewModel(
            FakeRecordRepository(),
            audio,
            ElapsedRealtimeClock { testScheduler.currentTime },
        )
        viewModel.startTraining(
            TrainingSettings(attemptCount = 1, recoveryDurationMillis = 30_000L, voiceGuidanceEnabled = true),
        )
        assertEquals(1, audio.preparationGuidanceCount)

        advanceTimeBy(BreathTrainingViewModel.PREPARATION_MILLIS + 1_600L)
        runCurrent()
        viewModel.stopHolding()
        runCurrent()

        assertTrue(audio.stopVoiceCount >= 1)
        assertEquals(1, audio.recoveryGuidanceCount)
    }
}

private class FakeRecordRepository(initialSessions: List<TrainingSessionEntity> = emptyList()) : RecordRepository {
    val saved = mutableListOf<BreathHoldRecord>()
    var startedSession: TrainingSessionEntity? = null
    var finishedStatus: String? = null
    private val records = MutableStateFlow<List<BreathHoldRecord>>(emptyList())
    private val sessions = MutableStateFlow(initialSessions)
    override fun observeRecords(): Flow<List<BreathHoldRecord>> = records
    override fun observeSessions(): Flow<List<TrainingSessionEntity>> = sessions
    override suspend fun getActiveSession(): TrainingSessionEntity? =
        sessions.value.lastOrNull { it.status == TrainingSessionEntity.STATUS_ACTIVE }
    override suspend fun getSessionRecords(sessionId: String): List<BreathHoldRecord> =
        saved.filter { it.sessionId == sessionId }
    override suspend fun getAllRecords(): List<BreathHoldRecord> = saved.toList()
    override suspend fun save(record: BreathHoldRecord): Long {
        saved += record
        records.value = saved.toList()
        return saved.size.toLong()
    }
    override suspend fun updateComfort(recordId: Long, rating: Int) {
        val index = recordId.toInt() - 1
        if (index >= 0) saved[index] = saved[index].copy(comfortRating = rating)
    }
    override suspend fun updateFeedback(recordId: Long, rating: Int, reason: String) {
        val index = recordId.toInt() - 1
        if (index >= 0) saved[index] = saved[index].copy(comfortRating = rating, stopReason = reason)
    }
    override suspend fun updateActualRecovery(recordId: Long, durationMillis: Long) {
        val index = recordId.toInt() - 1
        if (index >= 0) saved[index] = saved[index].copy(actualRecoveryDurationMillis = durationMillis)
    }
    override suspend fun updateSessionNote(sessionId: String, note: String) {
        saved.indices.filter { saved[it].sessionId == sessionId }.forEach { index ->
            saved[index] = saved[index].copy(sessionNote = note)
        }
    }
    override suspend fun startSession(session: TrainingSessionEntity) {
        startedSession = session
        sessions.value = sessions.value + session
    }
    override suspend fun updateSessionProgress(sessionId: String, completedAttempts: Int) = Unit
    override suspend fun finishSession(sessionId: String, status: String, reason: String) {
        finishedStatus = status
    }
}

private class FakeAudioController : TrainingAudioController {
    var preparationGuidanceCount = 0
    var recoveryGuidanceCount = 0
    var stopVoiceCount = 0
    override fun configure(
        musicVolumePercent: Int,
        cueVolumePercent: Int,
        vibrationEnabled: Boolean,
        cueStyle: CueStyle,
        vibrationStrength: VibrationStrength,
    ) = Unit
    override suspend fun startPreparationMusic() = Unit
    override fun stopPreparationMusic() = Unit
    override fun startHoldingMusic() = Unit
    override fun stopHoldingMusic() = Unit
    override suspend fun playTransitionCue() = Unit
    override fun speakPreparationGuidance() { preparationGuidanceCount++ }
    override fun speakRecoveryGuidance() { recoveryGuidanceCount++ }
    override fun stopVoiceGuidance() { stopVoiceCount++ }
    override fun release() = Unit
}
