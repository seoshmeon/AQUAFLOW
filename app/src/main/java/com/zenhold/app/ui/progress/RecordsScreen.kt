package com.zenhold.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenhold.app.data.local.BreathHoldRecord
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.util.formatDuration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordsScreen(records: List<BreathHoldRecord>, modifier: Modifier = Modifier) {
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
            Text("Рекорды", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Все завершённые задержки",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(24.dp))
        if (records.isEmpty()) {
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
                items(records, key = { it.id }) { record -> RecordRow(record) }
            }
        }
      }
    }
}

@Composable
private fun RecordRow(record: BreathHoldRecord) {
    val formattedDate = remember(record.timestamp) {
        SimpleDateFormat("d MMMM · HH:mm", Locale.forLanguageTag("ru-RU"))
            .format(Date(record.timestamp))
    }
    NeumorphicPanel(
        modifier = Modifier.fillMaxWidth().height(if (record.comfortRating == 0) 92.dp else 108.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(formattedDate, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Подход ${record.attemptNumber}", fontWeight = FontWeight.Medium)
                if (record.comfortRating != 0) {
                    Text(
                        comfortLabel(record.comfortRating),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(formatDuration(record.holdDurationMillis), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Light)
        }
    }
}

private fun comfortLabel(value: Int): String = when (value) {
    1 -> "Легко"
    2 -> "Комфортно"
    3 -> "Был дискомфорт"
    4 -> "Слишком тяжело"
    else -> "Без оценки"
}
