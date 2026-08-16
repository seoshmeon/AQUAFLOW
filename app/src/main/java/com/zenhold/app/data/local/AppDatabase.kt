package com.zenhold.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BreathHoldRecord::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
}
