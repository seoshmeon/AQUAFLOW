package com.zenhold.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    onThemeModeChanged: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 72.dp),
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Тема меняется сразу. Параметры тренировки — со следующей сессии.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        ThemeModeSelector(
            selected = settings.themeMode,
            onSelected = onThemeModeChanged,
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
                modes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selected == mode,
                        onClick = { onSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
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
