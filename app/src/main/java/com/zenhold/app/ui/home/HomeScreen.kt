package com.zenhold.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        val compact = maxHeight < 720.dp || maxWidth < 360.dp
        val pagePadding = if (maxWidth < 360.dp) 16.dp else 24.dp
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 980.dp)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = pagePadding, vertical = if (compact) 18.dp else 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(accent, CircleShape))
                Text("  APIRA", fontSize = 13.sp, letterSpacing = 3.4.sp, color = accent, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(if (compact) 20.dp else 34.dp))

            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(34.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeroCopy(Modifier.weight(.9f))
                    SessionOverview(settings, Modifier.weight(1.1f).height(250.dp))
                }
            } else {
                HeroCopy()
                Spacer(Modifier.height(if (compact) 24.dp else 42.dp))
                SessionOverview(
                    settings = settings,
                    modifier = Modifier.fillMaxWidth().height(if (compact) 210.dp else 238.dp),
                )
            }

            Spacer(Modifier.height(if (compact) 20.dp else 32.dp))
            Text(
                "Только сидя или лёжа. Не тренируйтесь в воде, за рулём или после гипервентиляции.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            )

            if (wide) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StartAction(onStart, Modifier.weight(1.25f))
                    ProgressAction(onProgress, Modifier.weight(.75f))
                }
            } else {
                StartAction(onStart, Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                ProgressAction(onProgress, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun HeroCopy(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("Спокойная сила", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Light)
        Text("начинается с дыхания", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Text(
            "Мягкая практика задержки без цифр, гонки и лишнего напряжения.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionOverview(settings: TrainingSettings, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    NeumorphicPanel(modifier = modifier, shape = RoundedCornerShape(30.dp)) {
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
}

@Composable
private fun StartAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    NeumorphicAction(
        onClick = onClick,
        modifier = modifier.heightIn(min = 58.dp),
        color = accent,
        shape = RoundedCornerShape(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary)
            Text("  Начать тренировку", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProgressAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    NeumorphicAction(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(7.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.ShowChart, null, tint = accent)
            Text("  Мой прогресс", color = MaterialTheme.colorScheme.onSurface)
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
