package com.zenhold.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.ui.components.NeumorphicAction
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.util.formatDuration

@Composable
fun HomeScreen(
    settings: TrainingSettings,
    onStart: () -> Unit,
    onProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(accent, CircleShape))
                Text("  AERNEA", fontSize = 13.sp, letterSpacing = 3.sp, color = accent, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(26.dp))
            Text("Спокойная сила", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Light)
            Text("начинается с дыхания", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        }

        NeumorphicPanel(
            modifier = Modifier.fillMaxWidth().height(238.dp),
            shape = RoundedCornerShape(34.dp),
        ) {
            Column(Modifier.fillMaxSize().padding(26.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("СЕГОДНЯ", fontSize = 11.sp, letterSpacing = 2.sp, color = accent)
                        Text("Комфортная сессия", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    }
                    NeumorphicPanel(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        elevation = 7.dp,
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Air, null, tint = accent) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SessionMetric("ПОДХОДЫ", settings.attemptCount.toString(), Modifier.weight(1f))
                    SessionMetric("ОТДЫХ", formatDuration(settings.recoveryDurationMillis), Modifier.weight(1f))
                }
            }
        }

        Column {
            Text(
                "Только сидя или лёжа. Не тренируйтесь в воде, за рулём или после гипервентиляции.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 14.dp),
            )
            NeumorphicAction(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                color = accent,
                shape = RoundedCornerShape(22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary)
                    Text("  Начать тренировку", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            NeumorphicAction(
                onClick = onProgress,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.ShowChart, null, tint = accent)
                    Text("  Мой прогресс", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun SessionMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 10.sp, letterSpacing = 1.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun TrainingSettingSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValue: (Float) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    NeumorphicPanel(
        modifier = Modifier.fillMaxWidth().height(126.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(valueLabel, fontWeight = FontWeight.SemiBold, color = accent)
            }
            Slider(
                value = value,
                onValueChange = onValue,
                modifier = Modifier.semantics {
                    contentDescription = "$label: $valueLabel"
                },
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    activeTickColor = MaterialTheme.colorScheme.background,
                    inactiveTickColor = accent.copy(alpha = .72f),
                ),
            )
        }
    }
}
