package com.zenhold.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        val themeMode = stringPreferencesKey("theme_mode")
    }

    override val settings: Flow<TrainingSettings> = context.trainingDataStore.data.map { values ->
        TrainingSettings(
            attemptCount = (values[Keys.attempts] ?: 4).coerceIn(1, 10),
            recoveryDurationMillis = (values[Keys.recovery] ?: 120_000L)
                .coerceIn(30_000L, 600_000L),
            themeMode = values[Keys.themeMode]
                ?.let { stored -> AppThemeMode.entries.firstOrNull { it.name == stored } }
                ?: AppThemeMode.System,
        )
    }

    override suspend fun update(settings: TrainingSettings) {
        context.trainingDataStore.edit { values ->
            values[Keys.attempts] = settings.attemptCount
            values[Keys.recovery] = settings.recoveryDurationMillis
            values[Keys.themeMode] = settings.themeMode.name
        }
    }
}
