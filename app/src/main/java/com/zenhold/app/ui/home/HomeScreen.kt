package com.zenhold.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.R
import com.zenhold.app.ui.components.NeumorphicAction
import com.zenhold.app.ui.components.NeoTactilePrimaryAction
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
        val narrow = maxWidth < 420.dp
        val compact = maxHeight < 720.dp || narrow || LocalDensity.current.fontScale > 1.1f
        val pagePadding = if (maxWidth < 340.dp) 16.dp else 24.dp
        Column(
            modifier = Modifier.align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 980.dp)
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = pagePadding, vertical = if (compact) 18.dp else 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.aquaflow_mark),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                )
                Text(
                    "AQUAFLOW",
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 13.sp,
                    letterSpacing = 3.1.sp,
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(if (compact) 20.dp else 34.dp))

            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(34.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HeroCopy(compact = false, modifier = Modifier.weight(.9f))
                    SessionOverview(settings, compact = false, modifier = Modifier.weight(1.1f).heightIn(min = 250.dp))
                }
            } else {
                HeroCopy(compact = compact)
                Spacer(Modifier.height(if (compact) 20.dp else 42.dp))
                SessionOverview(
                    settings = settings,
                    compact = compact,
                    modifier = Modifier.fillMaxWidth().heightIn(min = if (compact) 204.dp else 238.dp),
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
private fun HeroCopy(compact: Boolean, modifier: Modifier = Modifier) {
    val headline = if (compact) {
        MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-.7).sp)
    } else MaterialTheme.typography.headlineLarge
    Column(modifier) {
        Text("Свобода", style = headline, fontWeight = FontWeight.Light, maxLines = 1)
        Text("начинается с дыхания", style = headline, fontWeight = FontWeight.SemiBold, maxLines = 2)
        Spacer(Modifier.height(12.dp))
        Text(
            "Мягкая практика задержки без цифр, гонки и лишнего напряжения.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionOverview(settings: TrainingSettings, compact: Boolean, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    NeumorphicPanel(modifier = modifier, shape = RoundedCornerShape(30.dp)) {
        Column(
            Modifier.fillMaxSize().padding(if (compact) 22.dp else 26.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("СЕГОДНЯ", fontSize = 11.sp, letterSpacing = 2.sp, color = accent)
                    Text(
                        "Комфортная сессия",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (compact) 18.sp else 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                NeumorphicPanel(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    elevation = 7.dp,
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Air, null, tint = accent) }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 14.dp),
            ) {
                SessionMetric("ПОДХОДЫ", settings.attemptCount.toString(), compact, Modifier.weight(1f))
                SessionMetric(
                    if (compact) "ПОДГОТ." else "ПОДГОТОВКА",
                    formatDuration(settings.preparationDurationMillis),
                    compact,
                    Modifier.weight(1f),
                )
                SessionMetric("ОТДЫХ", formatDuration(settings.recoveryDurationMillis), compact, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StartAction(onClick: () -> Unit, modifier: Modifier = Modifier) {
    NeoTactilePrimaryAction(
        onClick = onClick,
        modifier = modifier.heightIn(min = 58.dp),
    ) {
        Text(
            "Начать тренировку",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            Text("  Мой прогресс", color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
private fun SessionMetric(label: String, value: String, compact: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            fontSize = if (compact) 8.sp else 10.sp,
            letterSpacing = if (compact) .5.sp else 1.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
        Text(
            value,
            fontSize = if (compact) 23.sp else 26.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 126.dp),
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
