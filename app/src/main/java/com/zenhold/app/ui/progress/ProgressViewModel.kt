package com.zenhold.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.data.local.TrainingSessionEntity
import com.zenhold.app.domain.repository.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ProgressPeriod { Week, Month, Quarter, Year, All }

data class CalendarDay(
    val dayOfMonth: Int,
    val sessionCount: Int,
    val bestMillis: Long,
)

data class Achievement(
    val title: String,
    val description: String,
    val unlocked: Boolean,
)

data class SessionSummary(
    val sessionId: String,
    val timestamp: Long,
    val attempts: List<BreathHoldRecord>,
    val averageMillis: Long,
    val maximumMillis: Long,
    val note: String,
    val plannedAttempts: Int = attempts.size,
    val status: String = TrainingSessionEntity.STATUS_COMPLETED,
    val energyLevel: Int = attempts.firstOrNull()?.energyLevel ?: 3,
    val stressLevel: Int = attempts.firstOrNull()?.stressLevel ?: 2,
    val interruptionReason: String = "",
)

data class SessionPoint(
    val timestamp: Long,
    val averageMillis: Long,
    val maximumMillis: Long,
    val comfortableMillis: Long = 0L,
)

data class MonthPoint(
    val yearMonth: YearMonth,
    val label: String,
    val averageMillis: Long,
    val maximumMillis: Long,
    val attemptCount: Int,
    val sessionCount: Int,
    val comfortableAverageMillis: Long = 0L,
)

data class PersonalRecord(val title: String, val value: String, val description: String)

data class ProgressUiState(
    val sessions: List<SessionPoint> = emptyList(),
    val months: List<MonthPoint> = emptyList(),
    val records: List<BreathHoldRecord> = emptyList(),
    val personalBestMillis: Long = 0L,
    val recentAverageMillis: Long = 0L,
    val comfortableAverageMillis: Long = 0L,
    val ratedAttemptCount: Int = 0,
    val selectedPeriod: ProgressPeriod = ProgressPeriod.Month,
    val comparisonPercent: Float? = null,
    val calendarMonthLabel: String = "",
    val calendarDays: List<CalendarDay> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val sessionSummaries: List<SessionSummary> = emptyList(),
    val personalRecords: List<PersonalRecord> = emptyList(),
    val insights: List<String> = emptyList(),
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: RecordRepository,
) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(ProgressPeriod.Month)

    val state: StateFlow<ProgressUiState> = combine(
        repository.observeRecords(),
        repository.observeSessions(),
        selectedPeriod,
    ) { records, sessions, period -> buildProgressState(records, period, sessionEntities = sessions) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    fun selectPeriod(period: ProgressPeriod) {
        selectedPeriod.value = period
    }

    fun saveSessionNote(sessionId: String, note: String) {
        viewModelScope.launch {
            repository.updateSessionNote(sessionId, note.trim().take(500))
        }
    }
}

internal fun buildProgressState(
    records: List<BreathHoldRecord>,
    selectedPeriod: ProgressPeriod = ProgressPeriod.All,
    nowMillis: Long = System.currentTimeMillis(),
    sessionEntities: List<TrainingSessionEntity> = emptyList(),
): ProgressUiState {
        val zone = ZoneId.systemDefault()
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.forLanguageTag("ru-RU"))
        if (records.isEmpty()) return ProgressUiState(
            selectedPeriod = selectedPeriod,
            calendarMonthLabel = YearMonth.from(now).format(monthFormatter)
                .replaceFirstChar { it.titlecase() },
            achievements = defaultAchievements(),
            sessionSummaries = sessionEntities.map { session ->
                SessionSummary(
                    sessionId = session.sessionId,
                    timestamp = session.startedAt,
                    attempts = emptyList(),
                    averageMillis = 0L,
                    maximumMillis = 0L,
                    note = session.note,
                    plannedAttempts = session.plannedAttempts,
                    status = session.status,
                    energyLevel = session.energyLevel,
                    stressLevel = session.stressLevel,
                    interruptionReason = session.interruptionReason,
                )
            }.sortedByDescending { it.timestamp },
        )
        val (periodStart, previousStart) = periodBoundaries(selectedPeriod, now)
        val periodRecords = periodStart?.let { start ->
            records.filter { it.timestamp >= start.toInstant().toEpochMilli() }
        } ?: records
        val previousRecords = if (periodStart != null && previousStart != null) {
            records.filter { record ->
                record.timestamp >= previousStart.toInstant().toEpochMilli() &&
                    record.timestamp < periodStart.toInstant().toEpochMilli()
            }
        } else emptyList()
        val sessions = periodRecords.groupBy { it.sessionId }.values
            .map { attempts ->
                SessionPoint(
                    timestamp = attempts.minOf { it.timestamp },
                    averageMillis = attempts.map { it.holdDurationMillis }.average().toLong(),
                    maximumMillis = attempts.maxOf { it.holdDurationMillis },
                    comfortableMillis = attempts.filter { it.comfortRating in 1..2 }
                        .takeIf { it.isNotEmpty() }
                        ?.map { it.holdDurationMillis }?.average()?.toLong() ?: 0L,
                )
            }
            .sortedBy { it.timestamp }
        val months = records.groupBy { record ->
            YearMonth.from(Instant.ofEpochMilli(record.timestamp).atZone(zone))
        }.map { (yearMonth, attempts) ->
            MonthPoint(
                yearMonth = yearMonth,
                label = yearMonth.format(monthFormatter).replaceFirstChar { it.titlecase() },
                averageMillis = attempts.map { it.holdDurationMillis }.average().toLong(),
                maximumMillis = attempts.maxOf { it.holdDurationMillis },
                attemptCount = attempts.size,
                sessionCount = attempts.map { it.sessionId }.distinct().size,
                comfortableAverageMillis = attempts.filter { it.comfortRating in 1..2 }
                    .takeIf { it.isNotEmpty() }
                    ?.map { it.holdDurationMillis }?.average()?.toLong() ?: 0L,
            )
        }.sortedBy { it.yearMonth }
        val comfortableRecords = records.filter { it.comfortRating == 1 || it.comfortRating == 2 }
        val currentMonth = YearMonth.from(now)
        val calendarDays = records
            .filter { YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) == currentMonth }
            .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).dayOfMonth }
            .map { (day, attempts) ->
                CalendarDay(
                    dayOfMonth = day,
                    sessionCount = attempts.map { it.sessionId }.distinct().size,
                    bestMillis = attempts.maxOf { it.holdDurationMillis },
                )
            }
            .sortedBy { it.dayOfMonth }
        val currentAverage = periodRecords.takeIf { it.isNotEmpty() }
            ?.map { it.holdDurationMillis }?.average()
        val previousAverage = previousRecords.takeIf { it.isNotEmpty() }
            ?.map { it.holdDurationMillis }?.average()
        val comparison = if (currentAverage != null && previousAverage != null && previousAverage > 0.0) {
            ((currentAverage - previousAverage) / previousAverage * 100.0).toFloat()
        } else null
        val attemptsBySession = records.groupBy { it.sessionId }
        val metadataBySession = sessionEntities.associateBy { it.sessionId }
        val allSessionIds = (attemptsBySession.keys + metadataBySession.keys)
        val allSessions = allSessionIds.map { id ->
            val attempts = attemptsBySession[id].orEmpty()
            val metadata = metadataBySession[id]
            SessionSummary(
                sessionId = id,
                timestamp = metadata?.startedAt ?: attempts.minOf { it.timestamp },
                attempts = attempts.sortedBy { it.attemptNumber },
                averageMillis = attempts.takeIf { it.isNotEmpty() }
                    ?.map { it.holdDurationMillis }?.average()?.toLong() ?: 0L,
                maximumMillis = attempts.maxOfOrNull { it.holdDurationMillis } ?: 0L,
                note = metadata?.note?.takeIf { it.isNotBlank() } ?: attempts.firstNotNullOfOrNull { attempt ->
                    attempt.sessionNote.takeIf { it.isNotBlank() }
                }.orEmpty(),
                plannedAttempts = metadata?.plannedAttempts ?: attempts.size,
                status = metadata?.status ?: TrainingSessionEntity.STATUS_COMPLETED,
                energyLevel = metadata?.energyLevel ?: attempts.firstOrNull()?.energyLevel ?: 3,
                stressLevel = metadata?.stressLevel ?: attempts.firstOrNull()?.stressLevel ?: 2,
                interruptionReason = metadata?.interruptionReason.orEmpty(),
            )
        }.sortedByDescending { it.timestamp }
        val comfortableCount = records.count { it.comfortRating == 1 || it.comfortRating == 2 }
        val stableSession = allSessions.any { summary ->
            val min = summary.attempts.minOfOrNull { it.holdDurationMillis } ?: 0L
            val max = summary.maximumMillis
            summary.attempts.size >= 3 && max > 0L && (max - min).toFloat() / max <= .15f
        }
        val achievements = listOf(
            Achievement("Первый спокойный шаг", "Завершить первую тренировку", allSessions.isNotEmpty()),
            Achievement("Ритм практики", "Провести 5 тренировок", allSessions.size >= 5),
            Achievement("Комфорт прежде всего", "Отметить 5 комфортных подходов", comfortableCount >= 5),
            Achievement("Ровное дыхание", "Три стабильных подхода в одной сессии", stableSession),
        )
        val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru-RU"))
        val bestRecord = records.maxBy { it.holdDurationMillis }
        val currentMonthRecords = records.filter {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone)) == currentMonth
        }
        val mostStable = allSessions.filter { it.attempts.size >= 3 }.minByOrNull { summary ->
            val min = summary.attempts.minOf { it.holdDurationMillis }
            (summary.maximumMillis - min).toDouble() / summary.maximumMillis.coerceAtLeast(1L)
        }
        val mostComfortable = allSessions.filter { summary ->
            summary.attempts.any { it.comfortRating in 1..2 }
        }.maxByOrNull { summary -> summary.attempts.count { it.comfortRating in 1..2 } }
        val personalRecords = buildList {
            add(PersonalRecord("Абсолютный рекорд", formatMillis(bestRecord.holdDurationMillis),
                Instant.ofEpochMilli(bestRecord.timestamp).atZone(zone).format(dateFormatter)))
            currentMonthRecords.maxByOrNull { it.holdDurationMillis }?.let {
                add(PersonalRecord("Лучшее месяца", formatMillis(it.holdDurationMillis),
                    Instant.ofEpochMilli(it.timestamp).atZone(zone).format(dateFormatter)))
            }
            mostStable?.let { add(PersonalRecord("Самая ровная серия", "${it.attempts.size} подхода", "Разброс результатов минимален")) }
            mostComfortable?.let {
                add(PersonalRecord("Комфортная сессия", "${it.attempts.count { a -> a.comfortRating in 1..2 }} подхода", "Оценены как легко или комфортно"))
            }
        }
        val insights = buildList {
            comparison?.let { value ->
                add(if (value >= 0f) "Среднее выбранного периода выросло на ${value.toInt()}%."
                else "Среднее выбранного периода изменилось на ${value.toInt()}% — ориентируйтесь прежде всего на комфорт.")
            }
            val calm = records.filter { it.stressLevel <= 2 }
            val tense = records.filter { it.stressLevel >= 4 }
            if (calm.size >= 3 && tense.size >= 3) {
                val calmAverage = calm.map { it.holdDurationMillis }.average()
                val tenseAverage = tense.map { it.holdDurationMillis }.average()
                add(if (calmAverage >= tenseAverage) "При низком напряжении результаты в среднем стабильнее и выше."
                else "Уровень напряжения пока не показывает устойчивой связи с результатом.")
            }
            if (allSessions.size >= 4) {
                val recent = allSessions.take(3).flatMap { it.attempts }.map { it.holdDurationMillis }
                val earlier = allSessions.drop(3).take(3).flatMap { it.attempts }.map { it.holdDurationMillis }
                if (recent.isNotEmpty() && earlier.isNotEmpty()) {
                    add("Последние сессии: среднее ${formatMillis(recent.average().toLong())}; ранее — ${formatMillis(earlier.average().toLong())}.")
                }
            }
            if (isEmpty()) add("Добавьте ещё несколько комфортных сессий — здесь появятся нейтральные наблюдения по вашей практике.")
        }
        return ProgressUiState(
            sessions = sessions,
            months = months,
            records = records.sortedByDescending { it.timestamp },
            personalBestMillis = records.maxOf { it.holdDurationMillis },
            recentAverageMillis = records.sortedByDescending { it.timestamp }
                .take(10)
                .map { it.holdDurationMillis }
                .average()
                .toLong(),
            comfortableAverageMillis = comfortableRecords
                .takeIf { it.isNotEmpty() }
                ?.map { it.holdDurationMillis }
                ?.average()
                ?.toLong()
                ?: 0L,
            ratedAttemptCount = records.count { it.comfortRating != 0 },
            selectedPeriod = selectedPeriod,
            comparisonPercent = comparison,
            calendarMonthLabel = currentMonth.format(monthFormatter).replaceFirstChar { it.titlecase() },
            calendarDays = calendarDays,
            achievements = achievements,
            sessionSummaries = allSessions,
            personalRecords = personalRecords,
            insights = insights,
        )
}

private fun formatMillis(value: Long): String {
    val totalSeconds = value / 1_000L
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

private fun defaultAchievements() = listOf(
    Achievement("Первый спокойный шаг", "Завершить первую тренировку", false),
    Achievement("Ритм практики", "Провести 5 тренировок", false),
    Achievement("Комфорт прежде всего", "Отметить 5 комфортных подходов", false),
    Achievement("Ровное дыхание", "Три стабильных подхода в одной сессии", false),
)

private fun periodBoundaries(
    period: ProgressPeriod,
    now: ZonedDateTime,
): Pair<ZonedDateTime?, ZonedDateTime?> = when (period) {
    ProgressPeriod.Week -> now.minusDays(7) to now.minusDays(14)
    ProgressPeriod.Month -> now.withDayOfMonth(1).toLocalDate().atStartOfDay(now.zone).let { start ->
        start to start.minusMonths(1)
    }
    ProgressPeriod.Quarter -> now.minusMonths(3) to now.minusMonths(6)
    ProgressPeriod.Year -> now.withDayOfYear(1).toLocalDate().atStartOfDay(now.zone).let { start ->
        start to start.minusYears(1)
    }
    ProgressPeriod.All -> null to null
}
