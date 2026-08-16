package com.zenhold.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(record: BreathHoldRecord): Long

    @Query("SELECT * FROM breath_hold_records ORDER BY timestamp ASC, id ASC")
    fun observeAll(): Flow<List<BreathHoldRecord>>

    @Query("SELECT * FROM breath_hold_records ORDER BY timestamp DESC, id DESC LIMIT :limit")
    suspend fun latest(limit: Int): List<BreathHoldRecord>

    @Query("UPDATE breath_hold_records SET comfortRating = :rating WHERE id = :recordId")
    suspend fun updateComfort(recordId: Long, rating: Int)

    @Query("UPDATE breath_hold_records SET sessionNote = :note WHERE sessionId = :sessionId")
    suspend fun updateSessionNote(sessionId: String, note: String)

    @Query("DELETE FROM breath_hold_records")
    suspend fun deleteAll()
}
