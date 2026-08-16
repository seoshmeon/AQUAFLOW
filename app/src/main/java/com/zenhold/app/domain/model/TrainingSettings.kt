package com.zenhold.app.domain.model

data class TrainingSettings(
    val attemptCount: Int = 4,
    val recoveryDurationMillis: Long = 120_000L,
    val themeMode: AppThemeMode = AppThemeMode.System,
) {
    init {
        require(attemptCount in 1..10)
        require(recoveryDurationMillis in 30_000L..600_000L)
    }
}

enum class AppThemeMode {
    System,
    Light,
    Dark,
}
