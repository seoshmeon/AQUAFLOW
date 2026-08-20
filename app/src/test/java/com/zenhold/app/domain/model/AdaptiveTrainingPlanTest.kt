package com.zenhold.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveTrainingPlanTest {
    private val base = TrainingSettings(attemptCount = 4, recoveryDurationMillis = 120_000L)

    @Test
    fun warningSymptoms_blockBreathHoldSession() {
        val plan = buildAdaptiveTrainingPlan(base, SessionCheckIn(warningSymptoms = true))

        assertFalse(plan.canStart)
        assertEquals(ReadinessLevel.Stop, plan.readiness)
    }

    @Test
    fun lowSleep_createsRecoverySession() {
        val plan = buildAdaptiveTrainingPlan(base, SessionCheckIn(sleepQuality = 1))

        assertTrue(plan.canStart)
        assertEquals(ReadinessLevel.Recovery, plan.readiness)
        assertEquals(2, plan.settings.attemptCount)
        assertEquals(180_000L, plan.settings.recoveryDurationMillis)
    }

    @Test
    fun reducedReadiness_lowersVolumeAndAddsRest() {
        val plan = buildAdaptiveTrainingPlan(base, SessionCheckIn(energyLevel = 2))

        assertEquals(ReadinessLevel.Reduced, plan.readiness)
        assertEquals(3, plan.settings.attemptCount)
        assertEquals(150_000L, plan.settings.recoveryDurationMillis)
    }

    @Test
    fun introProgram_capsAttemptCount() {
        val plan = buildAdaptiveTrainingPlan(
            base.copy(attemptCount = 7),
            SessionCheckIn(program = TrainingProgram.Intro),
        )

        assertEquals(3, plan.settings.attemptCount)
        assertTrue(plan.settings.recoveryDurationMillis >= 120_000L)
    }
}
