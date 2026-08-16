package com.zenhold.app.data.repository

import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.data.local.RecordDao
import com.zenhold.app.domain.repository.RecordRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class RoomRecordRepository @Inject constructor(
    private val dao: RecordDao,
) : RecordRepository {
    override fun observeRecords(): Flow<List<BreathHoldRecord>> = dao.observeAll()
    override suspend fun save(record: BreathHoldRecord): Long = dao.insert(record)
    override suspend fun updateComfort(recordId: Long, rating: Int) = dao.updateComfort(recordId, rating)
    override suspend fun updateSessionNote(sessionId: String, note: String) =
        dao.updateSessionNote(sessionId, note)
}
