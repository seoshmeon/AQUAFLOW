package com.zenhold.app

import android.os.Bundle
import android.app.Activity
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.zenhold.app.domain.model.TrainingState
import com.zenhold.app.domain.model.AppThemeMode
import com.zenhold.app.ui.components.AppNavigationMenu
import com.zenhold.app.ui.components.AppBottomNavigation
import com.zenhold.app.ui.components.MainNavigationItem
import com.zenhold.app.ui.components.TrainingMenu
import com.zenhold.app.ui.components.WaveBackdrop
import com.zenhold.app.ui.home.HomeScreen
import com.zenhold.app.ui.home.HomeViewModel
import com.zenhold.app.ui.home.SettingsScreen
import com.zenhold.app.ui.home.SafetyScreen
import com.zenhold.app.ui.home.OnboardingScreen
import com.zenhold.app.ui.progress.ProgressScreen
import com.zenhold.app.ui.progress.ProgressViewModel
import com.zenhold.app.ui.progress.RecordsScreen
import com.zenhold.app.ui.theme.ZenHoldTheme
import com.zenhold.app.ui.training.BreathTrainingViewModel
import com.zenhold.app.ui.training.FinishedScreen
import com.zenhold.app.ui.training.HoldScreen
import com.zenhold.app.ui.training.PreparationScreen
import com.zenhold.app.ui.training.RecoveryScreen
import com.zenhold.app.ui.training.InterruptedScreen
import com.zenhold.app.ui.training.PreTrainingScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var trainingKeyHandler: ((Int) -> Boolean)? = null

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.repeatCount == 0 && trainingKeyHandler?.invoke(keyCode) == true) return true
        return super.onKeyDown(keyCode, event)
    }

    fun setTrainingKeyHandler(handler: ((Int) -> Boolean)?) {
        trainingKeyHandler = handler
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ZenHold)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ZenHoldApp() }
    }
}

private enum class Destination { Onboarding, Home, SessionPlan, Training, Settings, Progress, Records, Safety }

@Composable
private fun ZenHoldApp(
    homeViewModel: HomeViewModel = hiltViewModel(),
    trainingViewModel: BreathTrainingViewModel = hiltViewModel(),
    progressViewModel: ProgressViewModel = hiltViewModel(),
) {
    val settings by homeViewModel.settings.collectAsStateWithLifecycle()
    val dataMessage by homeViewModel.dataMessage.collectAsStateWithLifecycle()
    val importPreview by homeViewModel.importPreview.collectAsStateWithLifecycle()
    val trainingState by trainingViewModel.state.collectAsStateWithLifecycle()
    val resumableSession by trainingViewModel.resumableSession.collectAsStateWithLifecycle()
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestTrainingState by rememberUpdatedState(trainingState)

    DisposableEffect(activity, trainingState) {
        val mainActivity = activity as? MainActivity
        val holding = trainingState as? TrainingState.Holding
        mainActivity?.setTrainingKeyHandler(
            if (holding?.gestureEnabled == true) { keyCode ->
                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        trainingViewModel.markFirstDiscomfort()
                        true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        trainingViewModel.stopHolding()
                        true
                    }
                    else -> false
                }
            } else null,
        )
        onDispose { mainActivity?.setTrainingKeyHandler(null) }
    }

    LaunchedEffect(settings.onboardingCompleted) {
        if (!settings.onboardingCompleted && destination != Destination.Training) {
            destination = Destination.Onboarding
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && latestTrainingState !is TrainingState.Idle &&
                latestTrainingState !is TrainingState.Finished &&
                latestTrainingState !is TrainingState.Interrupted
            ) {
                trainingViewModel.interruptForSafety()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = destination == Destination.Training) {
        if (trainingState is TrainingState.Idle) destination = Destination.Home
        else confirmSystemBack = true
    }
    BackHandler(
        enabled = destination != Destination.Home && destination != Destination.Training &&
            destination != Destination.Onboarding,
    ) {
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
                    WaveBackdrop(Modifier.fillMaxSize(), reduceMotion = settings.reduceMotion)
                }
                Crossfade(targetState = destination, label = "mainNavigation") { activeDestination ->
                  when (activeDestination) {
                    Destination.Onboarding -> OnboardingScreen(
                        onComplete = {
                            homeViewModel.completeOnboarding()
                            destination = Destination.Home
                        },
                    )
                    Destination.Home -> HomeScreen(
                        settings = settings,
                        onStart = { destination = Destination.SessionPlan },
                        onProgress = { destination = Destination.Progress },
                        resumableSession = resumableSession,
                        onResumeSession = {
                            trainingViewModel.resumeTraining(settings)
                            destination = Destination.Training
                        },
                        onDiscardSession = trainingViewModel::discardResumableSession,
                        weeklyPlan = progressState.weeklyPlan,
                        modifier = Modifier.padding(bottom = 72.dp),
                    )
                    Destination.Settings -> SettingsScreen(
                        settings = settings,
                        onAttemptsChanged = homeViewModel::setAttemptCount,
                        onRecoverySecondsChanged = homeViewModel::setRecoverySeconds,
                        onPreparationSecondsChanged = homeViewModel::setPreparationSeconds,
                        onPreparationMusicChanged = homeViewModel::setPreparationMusicEnabled,
                        onHoldingMusicChanged = homeViewModel::setHoldingMusicEnabled,
                        onVoiceGuidanceChanged = homeViewModel::setVoiceGuidanceEnabled,
                        onMusicVolumeChanged = homeViewModel::setMusicVolumePercent,
                        onCueVolumeChanged = homeViewModel::setCueVolumePercent,
                        onCueStyleChanged = homeViewModel::setCueStyle,
                        onVibrationChanged = homeViewModel::setVibrationEnabled,
                        onVibrationStrengthChanged = homeViewModel::setVibrationStrength,
                        onReduceMotionChanged = homeViewModel::setReduceMotion,
                        onFullScreenHoldGestureChanged = homeViewModel::setFullScreenHoldGesture,
                        onThemeModeChanged = homeViewModel::setThemeMode,
                        dataMessage = dataMessage,
                        importPreview = importPreview,
                        onExportJson = homeViewModel::exportJson,
                        onExportCsv = homeViewModel::exportCsv,
                        onExportPdf = homeViewModel::exportPdf,
                        onImportJson = homeViewModel::previewImport,
                        onConfirmImport = homeViewModel::confirmImport,
                        onCancelImport = homeViewModel::cancelImport,
                        onClearData = homeViewModel::clearAllData,
                        onDismissDataMessage = homeViewModel::dismissDataMessage,
                        onRestartOnboarding = homeViewModel::restartOnboarding,
                    )
                    Destination.SessionPlan -> PreTrainingScreen(
                        settings = settings,
                        initialProgram = progressState.weeklyPlan.recommendedProgram,
                        onStart = { checkIn ->
                            trainingViewModel.startTraining(settings, checkIn)
                            destination = Destination.Training
                        },
                        onBack = { destination = Destination.Home },
                    )
                    Destination.Progress -> ProgressScreen(
                        state = progressState,
                        onPeriodSelected = progressViewModel::selectPeriod,
                        onBack = { destination = Destination.Home },
                        modifier = Modifier.padding(bottom = 72.dp),
                    )
                    Destination.Records -> RecordsScreen(
                        sessions = progressState.sessionSummaries,
                        onSaveNote = progressViewModel::saveSessionNote,
                        modifier = Modifier.padding(bottom = 72.dp),
                    )
                    Destination.Safety -> SafetyScreen()
                    Destination.Training -> when (val state = trainingState) {
                        TrainingState.Idle -> destination = Destination.Home
                        is TrainingState.Preparation -> PreparationScreen(
                            state,
                            trainingViewModel::skipPreparation,
                            reduceMotion = settings.reduceMotion,
                        )
                        is TrainingState.Holding -> HoldScreen(
                            fullScreenGesture = state.fullScreenGesture,
                            gestureEnabled = state.gestureEnabled,
                            firstDiscomfortMarked = state.firstDiscomfortMarked,
                            reduceMotion = settings.reduceMotion,
                            onStopHolding = trainingViewModel::stopHolding,
                        )
                        is TrainingState.Recovering -> RecoveryScreen(
                            state,
                            trainingViewModel::setComfortRating,
                            trainingViewModel::setStopReason,
                            trainingViewModel::completeRecoveryEarly,
                            trainingViewModel::extendRecovery,
                            reduceMotion = settings.reduceMotion,
                        )
                        is TrainingState.Finished -> FinishedScreen(
                            state = state,
                            onDone = {
                                trainingViewModel.returnHome()
                                destination = Destination.Home
                            },
                        )
                        is TrainingState.Interrupted -> InterruptedScreen(
                            state = state,
                            onDone = {
                                trainingViewModel.returnHome()
                                destination = Destination.Home
                            },
                        )
                    }
                  }
                }

                if (destination == Destination.Training) {
                    TrainingMenu(
                        canStop = trainingState !is TrainingState.Finished &&
                            trainingState !is TrainingState.Interrupted &&
                            trainingState !is TrainingState.Idle,
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
                        onOpenSafety = {
                            trainingViewModel.returnHome()
                            destination = Destination.Safety
                        },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                } else if (destination != Destination.Onboarding) {
                    AppNavigationMenu(
                        onHome = { destination = Destination.Home },
                        onSettings = { destination = Destination.Settings },
                        onProgress = { destination = Destination.Progress },
                        onRecords = { destination = Destination.Records },
                        onSafety = { destination = Destination.Safety },
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }

                if (destination == Destination.Home || destination == Destination.Progress || destination == Destination.Records) {
                    AppBottomNavigation(
                        selected = when (destination) {
                            Destination.Progress -> MainNavigationItem.Progress
                            Destination.Records -> MainNavigationItem.Records
                            else -> MainNavigationItem.Home
                        },
                        onHome = { destination = Destination.Home },
                        onProgress = { destination = Destination.Progress },
                        onRecords = { destination = Destination.Records },
                        modifier = Modifier.align(Alignment.BottomCenter),
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
