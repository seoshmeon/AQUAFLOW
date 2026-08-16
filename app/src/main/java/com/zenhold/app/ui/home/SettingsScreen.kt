package com.zenhold.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.AppThemeMode
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.util.formatDuration

@Composable
fun SettingsScreen(
    settings: TrainingSettings,
    onAttemptsChanged: (Int) -> Unit,
    onRecoverySecondsChanged: (Int) -> Unit,
    onPreparationSecondsChanged: (Int) -> Unit,
    onPreparationMusicChanged: (Boolean) -> Unit,
    onHoldingMusicChanged: (Boolean) -> Unit,
    onFullScreenHoldGestureChanged: (Boolean) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val horizontalPadding = if (maxWidth < 360.dp) 16.dp else 24.dp
        Column(
            Modifier.align(androidx.compose.ui.Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 72.dp),
        ) {
            Text("Настройки", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Тема меняется сразу. Параметры тренировки — со следующей сессии.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
            ThemeModeSelector(selected = settings.themeMode, onSelected = onThemeModeChanged)
            Spacer(Modifier.height(28.dp))
            PreparationSelector(
                selectedMillis = settings.preparationDurationMillis,
                onSelectedSeconds = onPreparationSecondsChanged,
            )
            Spacer(Modifier.height(28.dp))
            TrainingSettingSlider(
                label = "Количество подходов",
                valueLabel = settings.attemptCount.toString(),
                value = settings.attemptCount.toFloat(),
                range = 1f..10f,
                steps = 8,
                onValue = { onAttemptsChanged(it.toInt()) },
            )
            Spacer(Modifier.height(28.dp))
            TrainingSettingSlider(
                label = "Время восстановления",
                valueLabel = formatDuration(settings.recoveryDurationMillis),
                value = (settings.recoveryDurationMillis / 1_000).toFloat(),
                range = 30f..600f,
                steps = 37,
                onValue = { seconds ->
                    onRecoverySecondsChanged(((seconds / 15).toInt() * 15).coerceIn(30, 600))
                },
            )
            Spacer(Modifier.height(28.dp))
            PreferenceToggle(
                title = "Музыка во время подготовки",
                description = "Мягкий фон перед началом задержки",
                checked = settings.preparationMusicEnabled,
                onChecked = onPreparationMusicChanged,
            )
            Spacer(Modifier.height(16.dp))
            PreferenceToggle(
                title = "Музыка во время задержки",
                description = "Handpan можно отключить и оставить тишину",
                checked = settings.holdingMusicEnabled,
                onChecked = onHoldingMusicChanged,
            )
            Spacer(Modifier.height(16.dp))
            PreferenceToggle(
                title = "Двойной тап по всему экрану",
                description = "Если выключить, сработает только выделенный круг",
                checked = settings.fullScreenHoldGesture,
                onChecked = onFullScreenHoldGestureChanged,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PreparationSelector(selectedMillis: Long, onSelectedSeconds: (Int) -> Unit) {
    val options = listOf(15, 30, 45, 60)
    NeumorphicPanel(
        modifier = Modifier.fillMaxWidth().height(126.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text("Подготовка перед задержкой", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEach { seconds ->
                    SegmentedButton(
                        selected = selectedMillis == seconds * 1_000L,
                        onClick = { onSelectedSeconds(seconds) },
                        shape = RoundedCornerShape(7.dp),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) { Text("$seconds с") }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggle(
    title: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), elevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun ThemeModeSelector(
    selected: AppThemeMode,
    onSelected: (AppThemeMode) -> Unit,
) {
    val modes = AppThemeMode.entries
    NeumorphicPanel(
        modifier = Modifier.fillMaxWidth().height(126.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text("Тема приложения", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEach { mode ->
                    SegmentedButton(
                        selected = selected == mode,
                        onClick = { onSelected(mode) },
                        shape = RoundedCornerShape(7.dp),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text(
                            when (mode) {
                                AppThemeMode.System -> "Система"
                                AppThemeMode.Light -> "Светлая"
                                AppThemeMode.Dark -> "Тёмная"
                            },
                        )
                    }
                }
            }
        }
    }
}
