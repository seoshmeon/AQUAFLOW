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
) {
    init {
        require(energyLevel in 1..5)
        require(stressLevel in 1..5)
    }
}

enum class ComfortRating(val storedValue: Int) {
    Easy(1),
    Comfortable(2),
    Uncomfortable(3),
    TooHard(4),
}

enum class AppThemeMode {
    System,
    Light,
    Dark,
}

enum class CueStyle { Bell, Soft, VibrationOnly, Silent }
