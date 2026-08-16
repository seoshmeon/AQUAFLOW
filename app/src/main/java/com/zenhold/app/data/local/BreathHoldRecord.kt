package com.zenhold.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

/** One completed attempt. Durations are stored as milliseconds to avoid rounding loss. */
@Entity(tableName = "breath_hold_records")
data class BreathHoldRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdDurationMillis: Long,
    val recoveryDurationMillis: Long,
    val timestamp: Long,
    val sessionId: String,
    val attemptNumber: Int,
    @ColumnInfo(defaultValue = "0") val comfortRating: Int = 0,
    @ColumnInfo(defaultValue = "3") val energyLevel: Int = 3,
    @ColumnInfo(defaultValue = "2") val stressLevel: Int = 2,
)
