package com.zenhold.app.domain.model

data class TrainingSettings(
    val attemptCount: Int = 4,
    val recoveryDurationMillis: Long = 120_000L,
    val preparationDurationMillis: Long = 30_000L,
    val preparationMusicEnabled: Boolean = true,
    val holdingMusicEnabled: Boolean = true,
    val musicVolumePercent: Int = 20,
    val cueVolumePercent: Int = 70,
    val cueStyle: CueStyle = CueStyle.Bell,
    val vibrationEnabled: Boolean = true,
    val vibrationStrength: VibrationStrength = VibrationStrength.Medium,
    val reduceMotion: Boolean = false,
    val fullScreenHoldGesture: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val onboardingCompleted: Boolean = true,
) {
    init {
        require(attemptCount in 1..10)
        require(recoveryDurationMillis in 30_000L..600_000L)
        require(preparationDurationMillis in PREPARATION_OPTIONS_MILLIS)
        require(musicVolumePercent in 0..100)
        require(cueVolumePercent in 0..100)
    }

    companion object {
        val PREPARATION_OPTIONS_MILLIS = setOf(15_000L, 30_000L, 45_000L, 60_000L)
    }
}

data class SessionCheckIn(
    val energyLevel: Int = 3,
    val stressLevel: Int = 2,
    val sleepQuality: Int = 3,
    val feelsUnwell: Boolean = false,
    val warningSymptoms: Boolean = false,
    val program: TrainingProgram = TrainingProgram.Adaptive,
) {
    init {
        require(energyLevel in 1..5)
        require(stressLevel in 1..5)
        require(sleepQuality in 1..5)
    }
}

enum class TrainingProgram {
    Adaptive,
    Intro,
    Comfort,
    Stability,
    Recovery,
    Free,
}

enum class ReadinessLevel { Optimal, Reduced, Recovery, Stop }

data class AdaptiveTrainingPlan(
    val settings: TrainingSettings,
    val readiness: ReadinessLevel,
    val title: String,
    val message: String,
    val canStart: Boolean,
)

/**
 * Conservative workload selection. It never prescribes a target hold duration: the user still
 * stops at their personal comfort boundary, while AQUAFLOW only adjusts session volume and rest.
 */
fun buildAdaptiveTrainingPlan(
    base: TrainingSettings,
    checkIn: SessionCheckIn,
): AdaptiveTrainingPlan {
    if (checkIn.warningSymptoms || checkIn.feelsUnwell) {
        return AdaptiveTrainingPlan(
            settings = base,
            readiness = ReadinessLevel.Stop,
            title = "Сегодня без задержек",
            message = "При недомогании, головокружении, боли или необычных симптомах тренировку лучше пропустить.",
            canStart = false,
        )
    }

    val readiness = when {
        checkIn.program == TrainingProgram.Recovery -> ReadinessLevel.Recovery
        checkIn.energyLevel == 1 || checkIn.sleepQuality == 1 || checkIn.stressLevel == 5 ->
            ReadinessLevel.Recovery
        checkIn.energyLevel <= 2 || checkIn.sleepQuality <= 2 || checkIn.stressLevel >= 4 ->
            ReadinessLevel.Reduced
        else -> ReadinessLevel.Optimal
    }
    var effective = when (readiness) {
        ReadinessLevel.Optimal -> base
        ReadinessLevel.Reduced -> base.copy(
            attemptCount = (base.attemptCount - 1).coerceAtLeast(1),
            recoveryDurationMillis = (base.recoveryDurationMillis + 30_000L).coerceAtMost(600_000L),
        )
        ReadinessLevel.Recovery -> base.copy(
            attemptCount = base.attemptCount.coerceAtMost(2),
            recoveryDurationMillis = (base.recoveryDurationMillis + 60_000L).coerceAtMost(600_000L),
        )
        ReadinessLevel.Stop -> base
    }
    effective = when (checkIn.program) {
        TrainingProgram.Adaptive -> effective
        TrainingProgram.Intro -> effective.copy(
            attemptCount = effective.attemptCount.coerceAtMost(3),
            recoveryDurationMillis = effective.recoveryDurationMillis.coerceAtLeast(120_000L),
        )
        TrainingProgram.Comfort -> effective.copy(
            attemptCount = effective.attemptCount.coerceAtMost(4),
            recoveryDurationMillis = effective.recoveryDurationMillis.coerceAtLeast(120_000L),
        )
        TrainingProgram.Stability -> effective.copy(attemptCount = effective.attemptCount.coerceAtMost(5))
        TrainingProgram.Recovery -> effective.copy(
            attemptCount = effective.attemptCount.coerceAtMost(2),
            recoveryDurationMillis = effective.recoveryDurationMillis.coerceAtLeast(180_000L),
        )
        TrainingProgram.Free -> if (readiness == ReadinessLevel.Optimal) base else effective
    }
    val (title, message) = when (readiness) {
        ReadinessLevel.Optimal -> "Готовность хорошая" to
            "Сохраняйте спокойный темп и завершайте каждый подход до выраженного дискомфорта."
        ReadinessLevel.Reduced -> "Облегчённая сессия" to
            "AQUAFLOW уменьшил объём и добавил отдых с учётом вашего состояния."
        ReadinessLevel.Recovery -> "Восстановительная сессия" to
            "Сегодня только короткая спокойная практика без попыток приблизиться к рекорду."
        ReadinessLevel.Stop -> error("Handled above")
    }
    return AdaptiveTrainingPlan(effective, readiness, title, message, canStart = true)
}

enum class ComfortRating(val storedValue: Int) {
    Easy(1),
    Comfortable(2),
    Uncomfortable(3),
    TooHard(4),
}

enum class RecoveryStopReason(val storedValue: String) {
    ComfortableLimit("COMFORTABLE_LIMIT"),
    FirstContractions("FIRST_CONTRACTIONS"),
    StrongUrge("STRONG_URGE"),
    Other("OTHER"),
}

enum class AppThemeMode {
    System,
    Light,
    Dark,
}

enum class CueStyle { Bell, Soft, VibrationOnly, Silent }
enum class VibrationStrength { Gentle, Medium, Strong }
