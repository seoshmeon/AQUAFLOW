package com.zenhold.app.ui.training

import com.zenhold.app.audio.TrainingAudioController
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.TrainingState
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
        assertTrue(viewModel.state.value is TrainingState.Finished)
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
}

private class FakeRecordRepository : RecordRepository {
    val saved = mutableListOf<BreathHoldRecord>()
    private val records = MutableStateFlow<List<BreathHoldRecord>>(emptyList())
    override fun observeRecords(): Flow<List<BreathHoldRecord>> = records
    override suspend fun save(record: BreathHoldRecord): Long {
        saved += record
        records.value = saved.toList()
        return saved.size.toLong()
    }
    override suspend fun updateComfort(recordId: Long, rating: Int) {
        val index = recordId.toInt() - 1
        if (index >= 0) saved[index] = saved[index].copy(comfortRating = rating)
    }
    override suspend fun updateSessionNote(sessionId: String, note: String) {
        saved.indices.filter { saved[it].sessionId == sessionId }.forEach { index ->
            saved[index] = saved[index].copy(sessionNote = note)
        }
    }
}

private class FakeAudioController : TrainingAudioController {
    override fun configure(musicVolumePercent: Int, cueVolumePercent: Int, vibrationEnabled: Boolean) = Unit
    override suspend fun startPreparationMusic() = Unit
    override fun stopPreparationMusic() = Unit
    override fun startHoldingMusic() = Unit
    override fun stopHoldingMusic() = Unit
    override suspend fun playTransitionCue() = Unit
    override fun release() = Unit
}
