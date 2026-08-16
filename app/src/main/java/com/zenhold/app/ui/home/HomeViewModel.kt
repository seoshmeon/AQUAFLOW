package com.zenhold.app.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenhold.app.data.backup.DataBackupManager
import com.zenhold.app.data.backup.BackupPreview
import com.zenhold.app.data.backup.ImportMode
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.AppThemeMode
import com.zenhold.app.domain.model.CueStyle
import com.zenhold.app.domain.model.VibrationStrength
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
    private val _importPreview = MutableStateFlow<BackupPreview?>(null)
    val importPreview = _importPreview.asStateFlow()
    private var pendingImportUri: Uri? = null

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

    fun setCueStyle(value: CueStyle) = update(settings.value.copy(cueStyle = value))

    fun setVibrationEnabled(value: Boolean) =
        update(settings.value.copy(vibrationEnabled = value))

    fun setVibrationStrength(value: VibrationStrength) =
        update(settings.value.copy(vibrationStrength = value))

    fun setReduceMotion(value: Boolean) =
        update(settings.value.copy(reduceMotion = value))

    fun setFullScreenHoldGesture(value: Boolean) =
        update(settings.value.copy(fullScreenHoldGesture = value))

    fun setThemeMode(value: AppThemeMode) = update(settings.value.copy(themeMode = value))

    fun completeOnboarding() = update(settings.value.copy(onboardingCompleted = true))
    fun restartOnboarding() = update(settings.value.copy(onboardingCompleted = false))

    fun exportJson(uri: Uri) = runDataAction {
        val result = dataBackupManager.exportJson(uri)
        "Резервная копия сохранена: ${result.records} подходов"
    }

    fun exportCsv(uri: Uri) = runDataAction {
        "CSV сохранён: ${dataBackupManager.exportCsv(uri)} подходов"
    }

    fun previewImport(uri: Uri) {
        viewModelScope.launch {
            runCatching { dataBackupManager.previewJson(uri) }
                .onSuccess { preview ->
                    pendingImportUri = uri
                    _importPreview.value = preview
                }
                .onFailure { error ->
                    _dataMessage.value = error.message ?: "Не удалось прочитать резервную копию"
                }
        }
    }

    fun confirmImport(mode: ImportMode) = runDataAction {
        val uri = pendingImportUri ?: error("Файл импорта больше недоступен")
        val result = dataBackupManager.importJson(uri, mode)
        pendingImportUri = null
        _importPreview.value = null
        "Импортировано: ${result.sessions} сессий, ${result.records} подходов"
    }

    fun cancelImport() {
        pendingImportUri = null
        _importPreview.value = null
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
