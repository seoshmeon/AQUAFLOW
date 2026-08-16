package com.zenhold.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrainingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: TrainingSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(sessions: List<TrainingSessionEntity>)

    @Query("SELECT * FROM training_sessions ORDER BY startedAt ASC")
    suspend fun getAll(): List<TrainingSessionEntity>

    @Query("UPDATE training_sessions SET completedAttempts = :completedAttempts WHERE sessionId = :sessionId")
    suspend fun updateProgress(sessionId: String, completedAttempts: Int)

    @Query(
        "UPDATE training_sessions SET endedAt = :endedAt, status = :status, " +
            "interruptionReason = :reason WHERE sessionId = :sessionId",
    )
    suspend fun finish(sessionId: String, endedAt: Long, status: String, reason: String)

    @Query("UPDATE training_sessions SET note = :note WHERE sessionId = :sessionId")
    suspend fun updateNote(sessionId: String, note: String)

    @Query("DELETE FROM training_sessions")
    suspend fun deleteAll()
}
