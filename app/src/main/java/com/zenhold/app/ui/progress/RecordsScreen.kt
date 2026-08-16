package com.zenhold.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.data.local.TrainingSessionEntity
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.util.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordsScreen(
    sessions: List<SessionSummary>,
    onSaveNote: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
      val pagePadding = if (maxWidth < 360.dp) 16.dp else 24.dp
      Column(
        Modifier.align(Alignment.TopCenter)
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 72.dp),
      ) {
        Column(Modifier.padding(horizontal = pagePadding)) {
            Text("История", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Тренировки, подходы, ощущения и заметки",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
        if (sessions.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Записей пока нет", fontWeight = FontWeight.SemiBold)
                Text(
                    "Завершите первый подход — результат появится здесь.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = pagePadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                items(sessions, key = { it.sessionId }) { session ->
                    SessionHistoryCard(session, onSaveNote)
                }
            }
        }
      }
    }
}

@Composable
private fun SessionHistoryCard(session: SessionSummary, onSaveNote: (String, String) -> Unit) {
    var expanded by remember(session.sessionId) { mutableStateOf(false) }
    var note by remember(session.sessionId, session.note) { mutableStateOf(session.note) }
    val formattedDate = remember(session.timestamp) {
        SimpleDateFormat("d MMMM · HH:mm", Locale.forLanguageTag("ru-RU"))
            .format(Date(session.timestamp))
    }
    NeumorphicPanel(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(formattedDate, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${session.attempts.size} из ${session.plannedAttempts} · ${statusLabel(session.status)}",
                        fontWeight = FontWeight.Medium,
                        color = if (session.status == TrainingSessionEntity.STATUS_COMPLETED) {
                            MaterialTheme.colorScheme.onSurface
                        } else MaterialTheme.colorScheme.primary,
                    )
                    if (session.attempts.isNotEmpty()) {
                        Text("Среднее ${formatDuration(session.averageMillis)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (session.note.isNotBlank() && !expanded) {
                        Text(session.note, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                if (session.maximumMillis > 0L) {
                    Text(
                        formatDuration(session.maximumMillis),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light,
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Энергия ${session.energyLevel}/5 · напряжение ${session.stressLevel}/5",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
                if (session.interruptionReason.isNotBlank()) {
                    Text(session.interruptionReason, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    Spacer(Modifier.height(6.dp))
                }
                session.attempts.forEach { RecordRow(it) }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Заметка о тренировке") },
                    supportingText = { Text("Самочувствие, сон или важное наблюдение") },
                    minLines = 2,
                    shape = RoundedCornerShape(16.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        onSaveNote(session.sessionId, note)
                        expanded = false
                    }) { Text("Сохранить") }
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    TrainingSessionEntity.STATUS_ACTIVE -> "можно продолжить"
    TrainingSessionEntity.STATUS_COMPLETED -> "завершена"
    TrainingSessionEntity.STATUS_STOPPED -> "остановлена"
    TrainingSessionEntity.STATUS_INTERRUPTED -> "прервана"
    else -> "сессия"
}

@Composable
private fun RecordRow(record: BreathHoldRecord) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Подход ${record.attemptNumber}", fontWeight = FontWeight.Medium)
            if (record.comfortRating != 0) {
                Text(comfortLabel(record.comfortRating), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(formatDuration(record.holdDurationMillis), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
    }
}

private fun comfortLabel(value: Int): String = when (value) {
    1 -> "Легко"
    2 -> "Комфортно"
    3 -> "Был дискомфорт"
    4 -> "Слишком тяжело"
    else -> "Без оценки"
}
