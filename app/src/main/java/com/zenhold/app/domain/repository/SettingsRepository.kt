package com.zenhold.app.domain.repository

import com.zenhold.app.domain.model.TrainingSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<TrainingSettings>
    suspend fun update(settings: TrainingSettings)
}
