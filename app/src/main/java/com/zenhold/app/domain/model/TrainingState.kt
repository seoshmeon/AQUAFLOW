package com.zenhold.app.domain.model

/**
 * The Holding state intentionally contains no elapsed time. This makes displaying a hold timer
 * from UI state impossible and protects the core low-anxiety experience.
 */
sealed interface TrainingState {
    data object Idle : TrainingState

    data class Preparation(
        val remainingMillis: Long,
        val totalMillis: Long,
        val attempt: Int,
        val totalAttempts: Int,
    ) : TrainingState

    data class Holding(
        val attempt: Int,
        val totalAttempts: Int,
        val fullScreenGesture: Boolean,
        val gestureEnabled: Boolean,
        val firstDiscomfortMarked: Boolean = false,
    ) : TrainingState

    data class Recovering(
        val holdDurationMillis: Long,
        val remainingMillis: Long,
        val totalRecoveryMillis: Long,
        val completedAttempt: Int,
        val totalAttempts: Int,
        val comfortRating: ComfortRating? = null,
        val stopReason: RecoveryStopReason? = null,
    ) : TrainingState

    data class Finished(
        val resultsMillis: List<Long>,
        val coachMessage: String = "",
        val nextSessionAdvice: String = "",
    ) : TrainingState

    data class Interrupted(
        val resultsMillis: List<Long>,
        val message: String,
    ) : TrainingState
}
