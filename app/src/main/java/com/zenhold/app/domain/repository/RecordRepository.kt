package com.zenhold.app.domain.repository

import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.data.local.TrainingSessionEntity
import kotlinx.coroutines.flow.Flow

interface RecordRepository {
    fun observeRecords(): Flow<List<BreathHoldRecord>>
    suspend fun save(record: BreathHoldRecord): Long
    suspend fun updateComfort(recordId: Long, rating: Int)
    suspend fun updateSessionNote(sessionId: String, note: String)
    suspend fun startSession(session: TrainingSessionEntity)
    suspend fun updateSessionProgress(sessionId: String, completedAttempts: Int)
    suspend fun finishSession(sessionId: String, status: String, reason: String = "")
}
