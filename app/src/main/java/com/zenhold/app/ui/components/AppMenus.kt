package com.zenhold.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Composable
fun AppNavigationMenu(
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onProgress: () -> Unit,
    onRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier.statusBarsPadding().padding(8.dp)) {
        NeumorphicAction(
            onClick = { expanded = true },
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(7.dp),
        ) {
            Icon(Icons.Rounded.Menu, contentDescription = "Открыть меню")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
        ) {
            NavigationItem("Главная", Icons.Rounded.Home) { expanded = false; onHome() }
            NavigationItem("Настройки", Icons.Rounded.Settings) { expanded = false; onSettings() }
            NavigationItem("График прогресса", Icons.AutoMirrored.Rounded.ShowChart) { expanded = false; onProgress() }
            NavigationItem("Рекорды", Icons.Rounded.History) { expanded = false; onRecords() }
        }
    }
}

@Composable
fun TrainingMenu(
    canStop: Boolean,
    darkBackground: Boolean,
    onStopTraining: () -> Unit,
    onReturnHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmation by remember { mutableStateOf<TrainingExitAction?>(null) }
    Column(modifier.statusBarsPadding().padding(8.dp)) {
        NeumorphicAction(
            onClick = { expanded = true },
            modifier = Modifier.size(46.dp),
            shape = RoundedCornerShape(7.dp),
            color = if (darkBackground) Color(0xFF111516) else Color.Unspecified,
        ) {
            Icon(
                Icons.Rounded.Menu,
                contentDescription = "Меню тренировки",
                tint = if (darkBackground) Color.White.copy(alpha = .72f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = .78f),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
        ) {
            if (canStop) {
                DropdownMenuItem(
                    text = { Text("Остановить тренировку") },
                    leadingIcon = { Icon(Icons.Rounded.StopCircle, null) },
                    onClick = { expanded = false; confirmation = TrainingExitAction.Stop },
                )
            }
            DropdownMenuItem(
                text = { Text("Вернуться в главное меню") },
                leadingIcon = { Icon(Icons.Rounded.Home, null) },
                onClick = {
                    expanded = false
                    if (canStop) confirmation = TrainingExitAction.Home else onReturnHome()
                },
            )
            NavigationItem("Настройки", Icons.Rounded.Settings) {
                expanded = false
                if (canStop) confirmation = TrainingExitAction.Settings else onOpenSettings()
            }
            NavigationItem("График прогресса", Icons.AutoMirrored.Rounded.ShowChart) {
                expanded = false
                if (canStop) confirmation = TrainingExitAction.Progress else onOpenProgress()
            }
            NavigationItem("Рекорды", Icons.Rounded.History) {
                expanded = false
                if (canStop) confirmation = TrainingExitAction.Records else onOpenRecords()
            }
        }
    }

    confirmation?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmation = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(if (action == TrainingExitAction.Stop) "Завершить тренировку?" else "Прервать тренировку?") },
            text = {
                Text(
                    if (action == TrainingExitAction.Stop) "Сохранённые подходы останутся в истории."
                    else "Текущая незавершённая задержка не будет сохранена.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmation = null
                    when (action) {
                        TrainingExitAction.Stop -> onStopTraining()
                        TrainingExitAction.Home -> onReturnHome()
                        TrainingExitAction.Settings -> onOpenSettings()
                        TrainingExitAction.Progress -> onOpenProgress()
                        TrainingExitAction.Records -> onOpenRecords()
                    }
                }) { Text(if (action == TrainingExitAction.Stop) "Завершить" else "Прервать") }
            },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Продолжить") } },
        )
    }
}

@Composable
private fun NavigationItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        onClick = onClick,
    )
}

private enum class TrainingExitAction { Stop, Home, Settings, Progress, Records }
