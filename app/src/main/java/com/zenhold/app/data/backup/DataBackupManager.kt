package com.zenhold.app.data.backup

import android.content.Context
import android.net.Uri
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
import com.zenhold.app.data.local.AppDatabase
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.data.local.RecordDao
import com.zenhold.app.data.local.TrainingSessionDao
import com.zenhold.app.data.local.TrainingSessionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ImportSummary(val sessions: Int, val records: Int)
data class BackupPreview(val exportedAt: Long, val sessions: Int, val records: Int)
enum class ImportMode { Merge, Replace }

private data class ParsedBackup(
    val exportedAt: Long,
    val sessions: List<TrainingSessionEntity>,
    val records: List<BreathHoldRecord>,
)

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
                "id,timestamp,session_id,attempt,hold_ms,planned_recovery_ms,actual_recovery_ms," +
                    "first_discomfort_ms,comfort,stop_reason,energy,stress,note",
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
                        record.actualRecoveryDurationMillis,
                        record.firstDiscomfortMillis,
                        record.comfortRating,
                        record.stopReason,
                        record.energyLevel,
                        record.stressLevel,
                        record.sessionNote,
                    ).joinToString(",") { csvCell(it.toString()) },
                )
            }
        } ?: throw IOException("Не удалось открыть файл для записи")
        allRecords.size
    }

    /** Creates a readable offline report that can be shared with a coach by the user. */
    suspend fun exportPdf(uri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        val allRecords = records.getAll()
        val allSessions = sessions.getAll()
        val document = PdfDocument()
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 48, 52)
            typeface = Typeface.create("sans", Typeface.NORMAL)
        }
        val bold = Paint(regular).apply { typeface = Typeface.create("sans", Typeface.BOLD) }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        fun openPage() {
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            y = 54f
            bold.textSize = 11f
            bold.color = Color.rgb(74, 143, 136)
            page!!.canvas.drawText("AQUAFLOW · отчёт прогресса", 42f, y, bold)
            y += 26f
        }

        fun closePage() {
            page?.let(document::finishPage)
            page = null
        }

        fun drawLine(text: String, size: Float = 11f, isBold: Boolean = false, gap: Float = 18f) {
            if (page == null) openPage()
            if (y > 800f) {
                closePage()
                openPage()
            }
            val paint = if (isBold) bold else regular
            paint.textSize = size
            paint.color = Color.rgb(35, 48, 52)
            page!!.canvas.drawText(text.take(92), 42f, y, paint)
            y += gap
        }

        fun drawWrapped(text: String, size: Float = 10f) {
            text.trim().split(Regex("\\s+")).fold(mutableListOf<String>()) { lines, word ->
                val candidate = (lines.lastOrNull().orEmpty() + " " + word).trim()
                if (candidate.length <= 78) {
                    if (lines.isEmpty()) lines += candidate else lines[lines.lastIndex] = candidate
                } else lines += word
                lines
            }.forEach { drawLine(it, size, gap = 15f) }
        }

        openPage()
        val date = SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru-RU"))
            .format(Date())
        drawLine("Сформирован: $date", 9f)
        y += 8f
        drawLine("Сводка", 18f, isBold = true, gap = 26f)
        val best = allRecords.maxOfOrNull { it.holdDurationMillis } ?: 0L
        val average = allRecords.takeIf { it.isNotEmpty() }
            ?.map { it.holdDurationMillis }?.average()?.toLong() ?: 0L
        val comfortable = allRecords.filter { it.comfortRating in 1..2 }
        val firstUrges = allRecords.filter { it.firstDiscomfortMillis > 0L }
        val recoveries = allRecords.filter { it.actualRecoveryDurationMillis > 0L }
        drawLine("Тренировок: ${allSessions.size} · подходов: ${allRecords.size}")
        drawLine("Лучший результат: ${pdfDuration(best)} · среднее: ${pdfDuration(average)}")
        drawLine(
            "Комфортное среднее: ${comfortable.averageDuration { it.holdDurationMillis }} · " +
                "первый позыв: ${firstUrges.averageDuration { it.firstDiscomfortMillis }}",
        )
        drawLine("Фактическое восстановление: ${recoveries.averageDuration { it.actualRecoveryDurationMillis }}")
        y += 12f
        drawLine("Последние сессии", 18f, isBold = true, gap = 28f)
        val recordsBySession = allRecords.groupBy { it.sessionId }
        allSessions.sortedByDescending { it.startedAt }.take(100).forEach { session ->
            val attempts = recordsBySession[session.sessionId].orEmpty()
            val sessionDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                .format(Date(session.startedAt))
            val sessionAverage = attempts.takeIf { it.isNotEmpty() }
                ?.map { it.holdDurationMillis }?.average()?.toLong() ?: 0L
            drawLine(
                "$sessionDate · ${attempts.size}/${session.plannedAttempts} подходов · " +
                    "среднее ${pdfDuration(sessionAverage)}",
                isBold = true,
            )
            drawLine(
                "Программа ${session.program.lowercase()} · энергия ${session.energyLevel}/5 · " +
                    "стресс ${session.stressLevel}/5 · сон ${session.sleepQuality}/5",
                size = 9f,
                gap = 15f,
            )
            attempts.forEach { attempt ->
                drawLine(
                    "  Подход ${attempt.attemptNumber}: ${pdfDuration(attempt.holdDurationMillis)} · " +
                        "первый позыв ${pdfDurationOrDash(attempt.firstDiscomfortMillis)} · " +
                        "восстановление ${pdfDurationOrDash(attempt.actualRecoveryDurationMillis)}",
                    size = 9f,
                    gap = 14f,
                )
            }
            if (session.note.isNotBlank()) drawWrapped("Заметка: ${session.note}", 9f)
            y += 8f
        }
        closePage()
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use(document::writeTo)
                ?: throw IOException("Не удалось открыть PDF для записи")
        } finally {
            document.close()
        }
        ImportSummary(allSessions.size, allRecords.size)
    }

    suspend fun previewJson(uri: Uri): BackupPreview = withContext(Dispatchers.IO) {
        val backup = readBackup(uri)
        BackupPreview(backup.exportedAt, backup.sessions.size, backup.records.size)
    }

    suspend fun importJson(uri: Uri, mode: ImportMode): ImportSummary = withContext(Dispatchers.IO) {
        val backup = readBackup(uri)
        database.withTransaction {
            if (mode == ImportMode.Replace) {
                records.deleteAll()
                sessions.deleteAll()
            }
            sessions.upsertAll(backup.sessions)
            records.insertAll(backup.records)
        }
        ImportSummary(backup.sessions.size, backup.records.size)
    }

    private fun readBackup(uri: Uri): ParsedBackup {
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
        return ParsedBackup(
            exportedAt = root.optLong("exportedAt", 0L),
            sessions = importedSessions,
            records = importedRecords,
        )
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
        .put("sleepQuality", sleepQuality).put("program", program)
        .put("readinessLevel", readinessLevel)
        .put("status", status).put("interruptionReason", interruptionReason).put("note", note)

    private fun BreathHoldRecord.toJson() = JSONObject()
        .put("id", id).put("holdDurationMillis", holdDurationMillis)
        .put("recoveryDurationMillis", recoveryDurationMillis).put("timestamp", timestamp)
        .put("sessionId", sessionId).put("attemptNumber", attemptNumber)
        .put("comfortRating", comfortRating).put("energyLevel", energyLevel)
        .put("stressLevel", stressLevel).put("sessionNote", sessionNote)
        .put("firstDiscomfortMillis", firstDiscomfortMillis)
        .put("actualRecoveryDurationMillis", actualRecoveryDurationMillis)
        .put("stopReason", stopReason)

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
                    sleepQuality = value.optInt("sleepQuality", 3).coerceIn(1, 5),
                    program = value.optString("program", "ADAPTIVE").take(30),
                    readinessLevel = value.optString("readinessLevel", "OPTIMAL").take(30),
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
                    firstDiscomfortMillis = value.optLong("firstDiscomfortMillis", 0L)
                        .coerceIn(0L, MAX_DURATION_MILLIS),
                    actualRecoveryDurationMillis = value.optLong("actualRecoveryDurationMillis", 0L)
                        .coerceIn(0L, 600_000L),
                    stopReason = value.optString("stopReason").take(50),
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

    private fun pdfDuration(value: Long): String {
        val seconds = value.coerceAtLeast(0L) / 1_000L
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }

    private fun pdfDurationOrDash(value: Long): String = if (value > 0L) pdfDuration(value) else "—"

    private fun List<BreathHoldRecord>.averageDuration(selector: (BreathHoldRecord) -> Long): String =
        if (isEmpty()) "—" else pdfDuration(map(selector).average().toLong())

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
