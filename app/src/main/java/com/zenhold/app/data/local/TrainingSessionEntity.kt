package com.zenhold.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Stable session-level metadata. Old attempt rows remain valid even without a matching session. */
@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAt: Long,
    @ColumnInfo(defaultValue = "0") val endedAt: Long = 0L,
    val plannedAttempts: Int,
    @ColumnInfo(defaultValue = "0") val completedAttempts: Int = 0,
    val preparationDurationMillis: Long,
    val recoveryDurationMillis: Long,
    val energyLevel: Int,
    val stressLevel: Int,
    @ColumnInfo(defaultValue = "'ACTIVE'") val status: String = STATUS_ACTIVE,
    @ColumnInfo(defaultValue = "''") val interruptionReason: String = "",
    @ColumnInfo(defaultValue = "''") val note: String = "",
) {
    companion object {
        const val STATUS_ACTIVE = "ACTIVE"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_STOPPED = "STOPPED"
        const val STATUS_INTERRUPTED = "INTERRUPTED"
    }
}
