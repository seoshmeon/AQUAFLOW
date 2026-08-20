package com.zenhold.app.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zenhold.app.data.backup.DataBackupManager
import com.zenhold.app.data.backup.ImportMode
import com.zenhold.app.data.local.AppDatabase
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.data.local.TrainingSessionEntity
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataBackupManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private val manager = DataBackupManager(context, database, database.recordDao(), database.trainingSessionDao())
    private val backupFile = File(context.cacheDir, "aquaflow-import-test.json")

    @After fun close() {
        database.close()
        backupFile.delete()
    }

    @Test
    fun previewAndReplaceImport_validBackup_restoresSessionAndAttempt() = runBlocking {
        backupFile.writeText(BACKUP)
        val uri = Uri.fromFile(backupFile)

        val preview = manager.previewJson(uri)
        assertEquals(1, preview.sessions)
        assertEquals(1, preview.records)

        manager.importJson(uri, ImportMode.Replace)
        assertEquals(1, database.trainingSessionDao().getAll().size)
        assertEquals(47_000L, database.recordDao().getAll().single().holdDurationMillis)
    }

    @Test
    fun exportPdf_withTrainingData_createsReadablePdfDocument() = runBlocking {
        database.trainingSessionDao().upsert(
            TrainingSessionEntity(
                sessionId = "pdf-session",
                startedAt = 1_700_000_000_000L,
                plannedAttempts = 1,
                completedAttempts = 1,
                preparationDurationMillis = 30_000L,
                recoveryDurationMillis = 120_000L,
                energyLevel = 4,
                stressLevel = 2,
                status = TrainingSessionEntity.STATUS_COMPLETED,
            ),
        )
        database.recordDao().insert(
            BreathHoldRecord(
                holdDurationMillis = 65_000L,
                recoveryDurationMillis = 120_000L,
                timestamp = 1_700_000_010_000L,
                sessionId = "pdf-session",
                attemptNumber = 1,
                comfortRating = 2,
                firstDiscomfortMillis = 43_000L,
                actualRecoveryDurationMillis = 88_000L,
            ),
        )
        val pdfFile = File(context.cacheDir, "aquaflow-report-test.pdf")

        val summary = manager.exportPdf(Uri.fromFile(pdfFile))
        val bytes = pdfFile.readBytes()

        assertEquals(1, summary.sessions)
        assertEquals(1, summary.records)
        assertTrue(bytes.size > 1_000)
        assertEquals("%PDF", bytes.take(4).map { it.toInt().toChar() }.joinToString(""))
        pdfFile.delete()
        Unit
    }

    private companion object {
        val BACKUP = """
            {"format":"aquaflow-backup","version":1,"exportedAt":1000,
             "sessions":[{"sessionId":"imported","startedAt":1000,"endedAt":2000,"plannedAttempts":1,
             "completedAttempts":1,"preparationDurationMillis":30000,"recoveryDurationMillis":60000,
             "energyLevel":3,"stressLevel":2,"status":"COMPLETED","interruptionReason":"","note":""}],
             "records":[{"id":1,"holdDurationMillis":47000,"recoveryDurationMillis":60000,"timestamp":1500,
             "sessionId":"imported","attemptNumber":1,"comfortRating":2,"energyLevel":3,"stressLevel":2,"sessionNote":""}]}
        """.trimIndent()
    }
}
