package com.zenhold.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.AppThemeMode
import com.zenhold.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<TrainingSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrainingSettings(),
    )

    fun setAttemptCount(value: Int) = update(settings.value.copy(attemptCount = value.coerceIn(1, 10)))

    fun setRecoverySeconds(value: Int) = update(
        settings.value.copy(recoveryDurationMillis = value.coerceIn(30, 600) * 1_000L),
    )

    fun setPreparationSeconds(value: Int) = update(
        settings.value.copy(
            preparationDurationMillis = value.toLong().times(1_000L)
                .takeIf { it in TrainingSettings.PREPARATION_OPTIONS_MILLIS }
                ?: 30_000L,
        ),
    )

    fun setPreparationMusicEnabled(value: Boolean) =
        update(settings.value.copy(preparationMusicEnabled = value))

    fun setHoldingMusicEnabled(value: Boolean) =
        update(settings.value.copy(holdingMusicEnabled = value))

    fun setMusicVolumePercent(value: Int) =
        update(settings.value.copy(musicVolumePercent = value.coerceIn(0, 100)))

    fun setCueVolumePercent(value: Int) =
        update(settings.value.copy(cueVolumePercent = value.coerceIn(0, 100)))

    fun setVibrationEnabled(value: Boolean) =
        update(settings.value.copy(vibrationEnabled = value))

    fun setReduceMotion(value: Boolean) =
        update(settings.value.copy(reduceMotion = value))

    fun setFullScreenHoldGesture(value: Boolean) =
        update(settings.value.copy(fullScreenHoldGesture = value))

    fun setThemeMode(value: AppThemeMode) = update(settings.value.copy(themeMode = value))

    private fun update(value: TrainingSettings) {
        viewModelScope.launch { settingsRepository.update(value) }
    }
}
