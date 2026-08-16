package com.zenhold.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.AppThemeMode
import com.zenhold.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.trainingDataStore by preferencesDataStore(name = "training_settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    private object Keys {
        val attempts = intPreferencesKey("attempt_count")
        val recovery = longPreferencesKey("recovery_duration_ms")
        val preparation = longPreferencesKey("preparation_duration_ms")
        val preparationMusic = booleanPreferencesKey("preparation_music_enabled")
        val holdingMusic = booleanPreferencesKey("holding_music_enabled")
        val musicVolume = intPreferencesKey("music_volume_percent")
        val cueVolume = intPreferencesKey("cue_volume_percent")
        val vibration = booleanPreferencesKey("vibration_enabled")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val fullScreenHoldGesture = booleanPreferencesKey("full_screen_hold_gesture")
        val themeMode = stringPreferencesKey("theme_mode")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
    }

    override val settings: Flow<TrainingSettings> = context.trainingDataStore.data.map { values ->
        TrainingSettings(
            attemptCount = (values[Keys.attempts] ?: 4).coerceIn(1, 10),
            recoveryDurationMillis = (values[Keys.recovery] ?: 120_000L)
                .coerceIn(30_000L, 600_000L),
            preparationDurationMillis = values[Keys.preparation]
                ?.takeIf { it in TrainingSettings.PREPARATION_OPTIONS_MILLIS }
                ?: 30_000L,
            preparationMusicEnabled = values[Keys.preparationMusic] ?: true,
            holdingMusicEnabled = values[Keys.holdingMusic] ?: true,
            musicVolumePercent = (values[Keys.musicVolume] ?: 20).coerceIn(0, 100),
            cueVolumePercent = (values[Keys.cueVolume] ?: 70).coerceIn(0, 100),
            vibrationEnabled = values[Keys.vibration] ?: true,
            reduceMotion = values[Keys.reduceMotion] ?: false,
            fullScreenHoldGesture = values[Keys.fullScreenHoldGesture] ?: true,
            themeMode = values[Keys.themeMode]
                ?.let { stored -> AppThemeMode.entries.firstOrNull { it.name == stored } }
                ?: AppThemeMode.System,
            onboardingCompleted = values[Keys.onboardingCompleted] ?: false,
        )
    }

    override suspend fun update(settings: TrainingSettings) {
        context.trainingDataStore.edit { values ->
            values[Keys.attempts] = settings.attemptCount
            values[Keys.recovery] = settings.recoveryDurationMillis
            values[Keys.preparation] = settings.preparationDurationMillis
            values[Keys.preparationMusic] = settings.preparationMusicEnabled
            values[Keys.holdingMusic] = settings.holdingMusicEnabled
            values[Keys.musicVolume] = settings.musicVolumePercent
            values[Keys.cueVolume] = settings.cueVolumePercent
            values[Keys.vibration] = settings.vibrationEnabled
            values[Keys.reduceMotion] = settings.reduceMotion
            values[Keys.fullScreenHoldGesture] = settings.fullScreenHoldGesture
            values[Keys.themeMode] = settings.themeMode.name
            values[Keys.onboardingCompleted] = settings.onboardingCompleted
        }
    }

    override suspend fun reset() {
        context.trainingDataStore.edit { it.clear() }
    }
}
