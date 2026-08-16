package com.zenhold.app.ui.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenhold.app.domain.model.SessionCheckIn
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.ui.components.NeumorphicAction
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.util.formatDuration

@Composable
fun PreTrainingScreen(
    settings: TrainingSettings,
    onStart: (SessionCheckIn) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var energy by remember { mutableIntStateOf(3) }
    var stress by remember { mutableIntStateOf(2) }
    var safetyConfirmed by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxHeight < 720.dp
        Column(
            Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 720.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (maxWidth < 360.dp) 16.dp else 24.dp, vertical = 72.dp),
        ) {
            Text("Перед тренировкой", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Проверьте план и отметьте состояние — это поможет отличать комфортный прогресс от случайного рекорда.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(if (compact) 22.dp else 34.dp))

            NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("План сессии", fontWeight = FontWeight.SemiBold)
                    PlanRow("Подготовка", formatDuration(settings.preparationDurationMillis))
                    PlanRow("Подходы", settings.attemptCount.toString())
                    PlanRow("Отдых", formatDuration(settings.recoveryDurationMillis))
                    PlanRow("Задержка", "без таймера на экране")
                }
            }
            Spacer(Modifier.height(20.dp))
            CheckInSlider("Энергия", energy, "1 — сил мало, 5 — чувствую бодрость") { energy = it }
            Spacer(Modifier.height(16.dp))
            CheckInSlider("Напряжение", stress, "1 — спокойно, 5 — сильный стресс") { stress = it }
            Spacer(Modifier.height(20.dp))

            NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), elevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(checked = safetyConfirmed, onCheckedChange = { safetyConfirmed = it })
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary)
                            Text("  Безопасное место", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Я сижу или лежу, не нахожусь в воде, за рулём и не выполнял гипервентиляцию.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            NeumorphicAction(
                onClick = { onStart(SessionCheckIn(energy, stress)) },
                enabled = safetyConfirmed,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(7.dp),
                color = if (safetyConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Text("  Начать подготовку", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            NeumorphicAction(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Вернуться")
            }
        }
    }
}

@Composable
private fun PlanRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CheckInSlider(label: String, value: Int, helper: String, onValue: (Int) -> Unit) {
    NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = 8.dp) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontWeight = FontWeight.Medium)
                Text(value.toString(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValue(it.toInt().coerceIn(1, 5)) },
                valueRange = 1f..5f,
                steps = 3,
            )
            Text(helper, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
