package com.zenhold.app.domain.repository

import com.zenhold.app.data.local.BreathHoldRecord
import kotlinx.coroutines.flow.Flow

interface RecordRepository {
    fun observeRecords(): Flow<List<BreathHoldRecord>>
    suspend fun save(record: BreathHoldRecord): Long
}
