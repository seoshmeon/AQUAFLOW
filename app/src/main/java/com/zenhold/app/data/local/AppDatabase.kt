package com.zenhold.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.AutoMigration

@Database(
    entities = [BreathHoldRecord::class, TrainingSessionEntity::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
    abstract fun trainingSessionDao(): TrainingSessionDao
}
