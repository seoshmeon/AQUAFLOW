package com.zenhold.app

import android.os.Bundle
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zenhold.app.domain.model.TrainingState
import com.zenhold.app.domain.model.AppThemeMode
import com.zenhold.app.ui.components.AppNavigationMenu
import com.zenhold.app.ui.components.TrainingMenu
import com.zenhold.app.ui.components.WaveBackdrop
import com.zenhold.app.ui.home.HomeScreen
import com.zenhold.app.ui.home.HomeViewModel
import com.zenhold.app.ui.home.SettingsScreen
import com.zenhold.app.ui.progress.ProgressScreen
import com.zenhold.app.ui.progress.ProgressViewModel
import com.zenhold.app.ui.progress.RecordsScreen
import com.zenhold.app.ui.theme.ZenHoldTheme
import com.zenhold.app.ui.training.BreathTrainingViewModel
import com.zenhold.app.ui.training.FinishedScreen
import com.zenhold.app.ui.training.HoldScreen
import com.zenhold.app.ui.training.PreparationScreen
import com.zenhold.app.ui.training.RecoveryScreen
import com.zenhold.app.ui.training.PreTrainingScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ZenHoldApp() }
    }
}

private enum class Destination { Home, SessionPlan, Training, Settings, Progress, Records }

@Composable
private fun ZenHoldApp(
    homeViewModel: HomeViewModel = hiltViewModel(),
    trainingViewModel: BreathTrainingViewModel = hiltViewModel(),
    progressViewModel: ProgressViewModel = hiltViewModel(),
) {
    val settings by homeViewModel.settings.collectAsStateWithLifecycle()
    val trainingState by trainingViewModel.state.collectAsStateWithLifecycle()
    val progressState by progressViewModel.state.collectAsStateWithLifecycle()
    val followsSystemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        AppThemeMode.System -> followsSystemDark
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }
    val view = LocalView.current
    val activity = view.context as? Activity
    // Survives Activity recreation so an active training never continues behind HomeScreen.
    var destination by rememberSaveable { mutableStateOf(Destination.Home) }
    var confirmSystemBack by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = destination == Destination.Training) {
        if (trainingState is TrainingState.Idle) destination = Destination.Home
        else confirmSystemBack = true
    }
    BackHandler(enabled = destination != Destination.Home && destination != Destination.Training) {
        destination = Destination.Home
    }

    SideEffect {
        activity?.window?.let { activityWindow ->
            val controller = WindowCompat.getInsetsController(activityWindow, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    ZenHoldTheme(darkTheme = darkTheme) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                if (trainingState !is TrainingState.Holding) {
                    WaveBackdrop(Modifier.fillMaxSize())
                }
                when (destination) {
                    Destination.Home -> HomeScreen(
                        settings = settings,
                        onStart = { destination = Destination.SessionPlan },
                        onProgress = { destination = Destination.Progress },
                    )
                    Destination.Settings -> SettingsScreen(
                        settings = settings,
                        onAttemptsChanged = homeViewModel::setAttemptCount,
                        onRecoverySecondsChanged = homeViewModel::setRecoverySeconds,
                        onPreparationSecondsChanged = homeViewModel::setPreparationSeconds,
                        onPreparationMusicChanged = homeViewModel::setPreparationMusicEnabled,
                        onHoldingMusicChanged = homeViewModel::setHoldingMusicEnabled,
                        onFullScreenHoldGestureChanged = homeViewModel::setFullScreenHoldGesture,
                        onThemeModeChanged = homeViewModel::setThemeMode,
                    )
                    Destination.SessionPlan -> PreTrainingScreen(
                        settings = settings,
                        onStart = { checkIn ->
                            trainingViewModel.startTraining(settings, checkIn)
                            destination = Destination.Training
                        },
                        onBack = { destination = Destination.Home },
                    )
                    Destination.Progress -> ProgressScreen(
                        state = progressState,
                        onBack = { destination = Destination.Home },
                    )
                    Destination.Records -> RecordsScreen(progressState.records)
                    Destination.Training -> when (val state = trainingState) {
                        TrainingState.Idle -> destination = Destination.Home
                        is TrainingState.Preparation -> PreparationScreen(state, trainingViewModel::skipPreparation)
                        is TrainingState.Holding -> HoldScreen(
                            fullScreenGesture = state.fullScreenGesture,
                            gestureEnabled = state.gestureEnabled,
                            onStopHolding = trainingViewModel::stopHolding,
                        )
                        is TrainingState.Recovering -> RecoveryScreen(state, trainingViewModel::setComfortRating)
                        is TrainingState.Finished -> FinishedScreen(
                            state = state,
                            onDone = {
                                trainingViewModel.returnHome()
                                destination = Destination.Home
                            },
                        )
                    }
                }

                if (destination == Destination.Training) {
                    TrainingMenu(
                        canStop = trainingState !is TrainingState.Finished && trainingState !is TrainingState.Idle,
                        darkBackground = trainingState is TrainingState.Holding,
                        onStopTraining = trainingViewModel::finishNow,
                        onReturnHome = {
                            trainingViewModel.returnHome()
                            destination = Destination.Home
                        },
                        onOpenSettings = {
                            trainingViewModel.returnHome()
                            destination = Destination.Settings
                        },
                        onOpenProgress = {
                            trainingViewModel.returnHome()
                            destination = Destination.Progress
                        },
                        onOpenRecords = {
                            trainingViewModel.returnHome()
                            destination = Destination.Records
                        },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                } else {
                    AppNavigationMenu(
                        onHome = { destination = Destination.Home },
                        onSettings = { destination = Destination.Settings },
                        onProgress = { destination = Destination.Progress },
                        onRecords = { destination = Destination.Records },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }

                if (confirmSystemBack) {
                    AlertDialog(
                        onDismissRequest = { confirmSystemBack = false },
                        shape = RoundedCornerShape(28.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        title = { Text("Прервать тренировку?") },
                        text = { Text("Текущая незавершённая задержка не будет сохранена. Уже завершённые подходы останутся в истории.") },
                        confirmButton = {
                            TextButton(onClick = {
                                confirmSystemBack = false
                                trainingViewModel.returnHome()
                                destination = Destination.Home
                            }) { Text("Прервать") }
                        },
                        dismissButton = {
                            TextButton(onClick = { confirmSystemBack = false }) { Text("Продолжить") }
                        },
                    )
                }
            }
        }
    }
}
