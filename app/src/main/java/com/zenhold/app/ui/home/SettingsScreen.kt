package com.zenhold.app.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zenhold.app.domain.model.TrainingSettings
import com.zenhold.app.domain.model.AppThemeMode
import com.zenhold.app.domain.model.CueStyle
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.components.NeumorphicAction
import com.zenhold.app.ui.util.formatDuration
import com.zenhold.app.data.backup.BackupPreview
import com.zenhold.app.data.backup.ImportMode
import java.time.LocalDate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: TrainingSettings,
    onAttemptsChanged: (Int) -> Unit,
    onRecoverySecondsChanged: (Int) -> Unit,
    onPreparationSecondsChanged: (Int) -> Unit,
    onPreparationMusicChanged: (Boolean) -> Unit,
    onHoldingMusicChanged: (Boolean) -> Unit,
    onMusicVolumeChanged: (Int) -> Unit,
    onCueVolumeChanged: (Int) -> Unit,
    onCueStyleChanged: (CueStyle) -> Unit,
    onVibrationChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onFullScreenHoldGestureChanged: (Boolean) -> Unit,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    dataMessage: String?,
    importPreview: BackupPreview?,
    onExportJson: (Uri) -> Unit,
    onExportCsv: (Uri) -> Unit,
    onImportJson: (Uri) -> Unit,
    onConfirmImport: (ImportMode) -> Unit,
    onCancelImport: () -> Unit,
    onClearData: () -> Unit,
    onDismissDataMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { it?.let(onExportJson) }
    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { it?.let(onExportCsv) }
    val importJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { it?.let(onImportJson) }
    var confirmClear by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxWidth < 420.dp || LocalDensity.current.fontScale > 1.1f
        val horizontalPadding = if (maxWidth < 360.dp) 16.dp else 24.dp
        Column(
            Modifier.align(androidx.compose.ui.Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 820.dp)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPadding, vertical = 72.dp),
        ) {
            Text("Настройки", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Тема меняется сразу. Параметры тренировки — со следующей сессии.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(40.dp))
            ThemeModeSelector(selected = settings.themeMode, compact = compact, onSelected = onThemeModeChanged)
            Spacer(Modifier.height(28.dp))
            PreparationSelector(
                selectedMillis = settings.preparationDurationMillis,
                compact = compact,
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
            TrainingSettingSlider(
                label = "Громкость музыки",
                valueLabel = "${settings.musicVolumePercent}%",
                value = settings.musicVolumePercent.toFloat(),
                range = 0f..100f,
                steps = 9,
                onValue = { onMusicVolumeChanged((it / 10).toInt() * 10) },
            )
            Spacer(Modifier.height(20.dp))
            CueStyleSelector(settings.cueStyle, compact, onCueStyleChanged)
            Spacer(Modifier.height(20.dp))
            TrainingSettingSlider(
                label = "Громкость сигнала",
                valueLabel = "${settings.cueVolumePercent}%",
                value = settings.cueVolumePercent.toFloat(),
                range = 0f..100f,
                steps = 9,
                onValue = { onCueVolumeChanged((it / 10).toInt() * 10) },
            )
            Spacer(Modifier.height(20.dp))
            PreferenceToggle(
                title = "Вибрация при переходе",
                description = "Тактильный сигнал перед задержкой",
                checked = settings.vibrationEnabled,
                onChecked = onVibrationChanged,
            )
            Spacer(Modifier.height(16.dp))
            PreferenceToggle(
                title = "Уменьшить анимацию",
                description = "Статичные волны и дыхательные круги",
                checked = settings.reduceMotion,
                onChecked = onReduceMotionChanged,
            )
            Spacer(Modifier.height(16.dp))
            PreferenceToggle(
                title = "Двойной тап по всему экрану",
                description = "Если выключить, сработает только выделенный круг",
                checked = settings.fullScreenHoldGesture,
                onChecked = onFullScreenHoldGestureChanged,
            )
            Spacer(Modifier.height(32.dp))
            DataManagementSection(
                onExportJson = {
                    exportJson.launch("AQUAFLOW-backup-${LocalDate.now()}.json")
                },
                onExportCsv = {
                    exportCsv.launch("AQUAFLOW-progress-${LocalDate.now()}.csv")
                },
                onImportJson = { importJson.launch(arrayOf("application/json", "text/plain")) },
                onClearData = { confirmClear = true },
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Удалить все данные?") },
            text = { Text("История тренировок и настройки будут удалены без возможности восстановления. Сначала можно сохранить JSON-копию.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearData()
                }) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Отмена") }
            },
        )
    }
    dataMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissDataMessage,
            title = { Text("Данные AQUAFLOW") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onDismissDataMessage) { Text("Готово") } },
        )
    }
    importPreview?.let { preview ->
        val date = remember(preview.exportedAt) {
            if (preview.exportedAt > 0L) {
                SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru-RU"))
                    .format(Date(preview.exportedAt))
            } else "дата не указана"
        }
        AlertDialog(
            onDismissRequest = onCancelImport,
            title = { Text("Проверка резервной копии") },
            text = {
                Text("Создана: $date\nСессий: ${preview.sessions}\nПодходов: ${preview.records}\n\nОбъединение сохранит текущую историю. Замена сначала удалит её полностью.")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { onConfirmImport(ImportMode.Merge) }) { Text("Объединить") }
                    TextButton(onClick = { onConfirmImport(ImportMode.Replace) }) {
                        Text("Заменить", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = { TextButton(onClick = onCancelImport) { Text("Отмена") } },
        )
    }
}

@Composable
private fun CueStyleSelector(selected: CueStyle, compact: Boolean, onSelected: (CueStyle) -> Unit) {
    NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text("Сигнал перехода", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                CueStyle.entries.forEach { style ->
                    SegmentedButton(
                        selected = selected == style,
                        onClick = { onSelected(style) },
                        shape = RoundedCornerShape(7.dp),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            when (style) {
                                CueStyle.Bell -> "Колокол"
                                CueStyle.Soft -> "Мягкий"
                                CueStyle.VibrationOnly -> "Вибро"
                                CueStyle.Silent -> "Тишина"
                            },
                            fontSize = if (compact) 9.sp else 11.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataManagementSection(
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImportJson: () -> Unit,
    onClearData: () -> Unit,
) {
    Text("Данные и резервные копии", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    NeumorphicPanel(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), elevation = 9.dp) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "История хранится только на устройстве. JSON подходит для восстановления, CSV — для анализа.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DataAction("Сохранить резервную копию JSON", onExportJson)
            DataAction("Экспортировать прогресс в CSV", onExportCsv)
            DataAction("Восстановить из JSON", onImportJson)
            DataAction("Удалить историю и настройки", onClearData, destructive = true)
        }
    }
}

@Composable
private fun DataAction(label: String, onClick: () -> Unit, destructive: Boolean = false) {
    NeumorphicAction(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PreparationSelector(
    selectedMillis: Long,
    compact: Boolean,
    onSelectedSeconds: (Int) -> Unit,
) {
    val options = listOf(15, 30, 45, 60)
    NeumorphicPanel(
        modifier = Modifier.fillMaxWidth().heightIn(min = 126.dp),
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
                    ) { Text("$seconds с", fontSize = if (compact) 11.sp else 14.sp, maxLines = 1) }
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
    compact: Boolean,
    onSelected: (AppThemeMode) -> Unit,
) {
    val modes = AppThemeMode.entries
    NeumorphicPanel(
        modifier = Modifier.fillMaxWidth().heightIn(min = 126.dp),
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
                            fontSize = if (compact) 11.sp else 14.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
