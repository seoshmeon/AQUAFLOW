package com.zenhold.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.zenhold.app.data.local.AppDatabase
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.data.local.RecordDao
import com.zenhold.app.data.local.TrainingSessionDao
import com.zenhold.app.data.local.TrainingSessionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ImportSummary(val sessions: Int, val records: Int)

/** User-owned, offline backup. No training data is sent over the network. */
@Singleton
class DataBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val records: RecordDao,
    private val sessions: TrainingSessionDao,
) {
    suspend fun exportJson(uri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        val allRecords = records.getAll()
        val allSessions = sessions.getAll()
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", FORMAT_VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("sessions", JSONArray().apply { allSessions.forEach { put(it.toJson()) } })
            .put("records", JSONArray().apply { allRecords.forEach { put(it.toJson()) } })
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
            it.write(root.toString(2))
        } ?: throw IOException("Не удалось открыть файл для записи")
        ImportSummary(allSessions.size, allRecords.size)
    }

    suspend fun exportCsv(uri: Uri): Int = withContext(Dispatchers.IO) {
        val allRecords = records.getAll()
        context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { writer ->
            writer.appendLine(
                "id,timestamp,session_id,attempt,hold_ms,recovery_ms,comfort,energy,stress,note",
            )
            allRecords.forEach { record ->
                writer.appendLine(
                    listOf(
                        record.id,
                        record.timestamp,
                        record.sessionId,
                        record.attemptNumber,
                        record.holdDurationMillis,
                        record.recoveryDurationMillis,
                        record.comfortRating,
                        record.energyLevel,
                        record.stressLevel,
                        record.sessionNote,
                    ).joinToString(",") { csvCell(it.toString()) },
                )
            }
        } ?: throw IOException("Не удалось открыть файл для записи")
        allRecords.size
    }

    suspend fun importJson(uri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(8_192)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                result.append(buffer, 0, count)
                if (result.length > MAX_BACKUP_CHARS) {
                    throw IOException("Файл резервной копии слишком большой")
                }
            }
            result.toString()
        } ?: throw IOException("Не удалось открыть файл")
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "Это не резервная копия AQUAFLOW" }
        require(root.optInt("version") == FORMAT_VERSION) { "Версия резервной копии не поддерживается" }
        val importedSessions = root.getJSONArray("sessions").toSessions()
        val importedRecords = root.getJSONArray("records").toRecords()
        require(importedSessions.size <= MAX_ITEMS && importedRecords.size <= MAX_ITEMS) {
            "В резервной копии слишком много записей"
        }
        database.withTransaction {
            sessions.upsertAll(importedSessions)
            records.insertAll(importedRecords)
        }
        ImportSummary(importedSessions.size, importedRecords.size)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        database.withTransaction {
            records.deleteAll()
            sessions.deleteAll()
        }
    }

    private fun TrainingSessionEntity.toJson() = JSONObject()
        .put("sessionId", sessionId).put("startedAt", startedAt).put("endedAt", endedAt)
        .put("plannedAttempts", plannedAttempts).put("completedAttempts", completedAttempts)
        .put("preparationDurationMillis", preparationDurationMillis)
        .put("recoveryDurationMillis", recoveryDurationMillis)
        .put("energyLevel", energyLevel).put("stressLevel", stressLevel)
        .put("status", status).put("interruptionReason", interruptionReason).put("note", note)

    private fun BreathHoldRecord.toJson() = JSONObject()
        .put("id", id).put("holdDurationMillis", holdDurationMillis)
        .put("recoveryDurationMillis", recoveryDurationMillis).put("timestamp", timestamp)
        .put("sessionId", sessionId).put("attemptNumber", attemptNumber)
        .put("comfortRating", comfortRating).put("energyLevel", energyLevel)
        .put("stressLevel", stressLevel).put("sessionNote", sessionNote)

    private fun JSONArray.toSessions() = buildList {
        repeat(length()) { index ->
            val value = getJSONObject(index)
            add(
                TrainingSessionEntity(
                    sessionId = value.getString("sessionId").requireShort("sessionId", 100),
                    startedAt = value.getLong("startedAt").requirePositive("startedAt"),
                    endedAt = value.optLong("endedAt", 0L).coerceAtLeast(0L),
                    plannedAttempts = value.getInt("plannedAttempts").coerceIn(1, 10),
                    completedAttempts = value.optInt("completedAttempts", 0).coerceIn(0, 10),
                    preparationDurationMillis = value.getLong("preparationDurationMillis").coerceIn(0L, 600_000L),
                    recoveryDurationMillis = value.getLong("recoveryDurationMillis").coerceIn(0L, 600_000L),
                    energyLevel = value.optInt("energyLevel", 3).coerceIn(1, 5),
                    stressLevel = value.optInt("stressLevel", 2).coerceIn(1, 5),
                    status = value.optString("status", TrainingSessionEntity.STATUS_COMPLETED).requireSessionStatus(),
                    interruptionReason = value.optString("interruptionReason").take(500),
                    note = value.optString("note").take(500),
                ),
            )
        }
    }

    private fun JSONArray.toRecords() = buildList {
        repeat(length()) { index ->
            val value = getJSONObject(index)
            add(
                BreathHoldRecord(
                    id = value.getLong("id").coerceAtLeast(0L),
                    holdDurationMillis = value.getLong("holdDurationMillis").coerceIn(1L, MAX_DURATION_MILLIS),
                    recoveryDurationMillis = value.getLong("recoveryDurationMillis").coerceIn(0L, 600_000L),
                    timestamp = value.getLong("timestamp").requirePositive("timestamp"),
                    sessionId = value.getString("sessionId").requireShort("sessionId", 100),
                    attemptNumber = value.getInt("attemptNumber").coerceIn(1, 10),
                    comfortRating = value.optInt("comfortRating", 0).coerceIn(0, 4),
                    energyLevel = value.optInt("energyLevel", 3).coerceIn(1, 5),
                    stressLevel = value.optInt("stressLevel", 2).coerceIn(1, 5),
                    sessionNote = value.optString("sessionNote").take(500),
                ),
            )
        }
    }

    private fun String.requireShort(name: String, maxLength: Int) = apply {
        require(isNotBlank() && length <= maxLength) { "Некорректное поле $name" }
    }

    private fun Long.requirePositive(name: String) = apply {
        require(this > 0L) { "Некорректное поле $name" }
    }

    private fun String.requireSessionStatus() = apply {
        require(this in SESSION_STATUSES) { "Некорректный статус сессии" }
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private companion object {
        const val FORMAT = "aquaflow-backup"
        const val FORMAT_VERSION = 1
        const val MAX_BACKUP_CHARS = 10_000_000
        const val MAX_ITEMS = 100_000
        const val MAX_DURATION_MILLIS = 86_400_000L
        val SESSION_STATUSES = setOf(
            TrainingSessionEntity.STATUS_ACTIVE,
            TrainingSessionEntity.STATUS_COMPLETED,
            TrainingSessionEntity.STATUS_STOPPED,
            TrainingSessionEntity.STATUS_INTERRUPTED,
        )
    }
}
