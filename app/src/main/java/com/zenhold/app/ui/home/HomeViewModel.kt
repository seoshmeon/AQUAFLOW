package com.zenhold.app.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenhold.app.data.backup.DataBackupManager
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.AppThemeMode
import com.zenhold.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dataBackupManager: DataBackupManager,
) : ViewModel() {
    private val _dataMessage = MutableStateFlow<String?>(null)
    val dataMessage = _dataMessage.asStateFlow()

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

    fun completeOnboarding() = update(settings.value.copy(onboardingCompleted = true))

    fun exportJson(uri: Uri) = runDataAction {
        val result = dataBackupManager.exportJson(uri)
        "Резервная копия сохранена: ${result.records} подходов"
    }

    fun exportCsv(uri: Uri) = runDataAction {
        "CSV сохранён: ${dataBackupManager.exportCsv(uri)} подходов"
    }

    fun importJson(uri: Uri) = runDataAction {
        val result = dataBackupManager.importJson(uri)
        "Импортировано: ${result.sessions} сессий, ${result.records} подходов"
    }

    fun clearAllData() = runDataAction {
        dataBackupManager.clearAll()
        settingsRepository.reset()
        "История и настройки удалены"
    }

    fun dismissDataMessage() {
        _dataMessage.value = null
    }

    private fun runDataAction(action: suspend () -> String) {
        viewModelScope.launch {
            _dataMessage.value = runCatching { action() }
                .getOrElse { error -> error.message ?: "Не удалось выполнить операцию" }
        }
    }

    private fun update(value: TrainingSettings) {
        viewModelScope.launch { settingsRepository.update(value) }
    }
}
