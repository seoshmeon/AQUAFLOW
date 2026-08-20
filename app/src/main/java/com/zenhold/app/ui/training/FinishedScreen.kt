package com.zenhold.app.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenhold.app.domain.model.TrainingState
import com.zenhold.app.ui.components.NeumorphicAction
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.util.formatDuration

@Composable
fun FinishedScreen(state: TrainingState.Finished, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Column(
        Modifier.fillMaxWidth()
            .widthIn(max = 540.dp)
            .verticalScroll(rememberScrollState())
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        NeumorphicPanel(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(38.dp)) }
        Spacer(Modifier.height(18.dp))
        Text("Тренировка завершена", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Лучший результат", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            formatDuration(state.resultsMillis.maxOrNull() ?: 0L),
            fontSize = 54.sp,
            fontWeight = FontWeight.Light,
            color = accent,
        )
        if (state.coachMessage.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            NeumorphicPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Комментарий тренера", color = accent, fontWeight = FontWeight.SemiBold)
                    Text(state.coachMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.nextSessionAdvice.isNotBlank()) {
                        Text(state.nextSessionAdvice, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        NeumorphicAction(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(7.dp),
            color = accent,
        ) { Text("Готово", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold) }
      }
    }
}
