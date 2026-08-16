package com.zenhold.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.AutoMigration

@Database(
    entities = [BreathHoldRecord::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
}
