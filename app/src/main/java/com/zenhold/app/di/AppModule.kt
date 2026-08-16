package com.zenhold.app.di

import android.content.Context
import androidx.room.Room
import com.zenhold.app.audio.Media3TrainingAudioController
import com.zenhold.app.audio.TrainingAudioController
import com.zenhold.app.data.local.AppDatabase
import com.zenhold.app.data.local.RecordDao
import com.zenhold.app.data.local.TrainingSessionDao
import com.zenhold.app.data.repository.DataStoreSettingsRepository
import com.zenhold.app.data.repository.RoomRecordRepository
import com.zenhold.app.domain.repository.RecordRepository
import com.zenhold.app.domain.repository.SettingsRepository
import com.zenhold.app.util.AndroidElapsedRealtimeClock
import com.zenhold.app.util.ElapsedRealtimeClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds abstract fun bindRecordRepository(impl: RoomRecordRepository): RecordRepository
    @Binds abstract fun bindSettingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun bindAudioController(impl: Media3TrainingAudioController): TrainingAudioController
    @Binds abstract fun bindClock(impl: AndroidElapsedRealtimeClock): ElapsedRealtimeClock
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "zenhold.db")
            .build()

    @Provides fun provideRecordDao(database: AppDatabase): RecordDao = database.recordDao()
    @Provides fun provideTrainingSessionDao(database: AppDatabase): TrainingSessionDao =
        database.trainingSessionDao()
}
