package com.zenhold.app.ui.training

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenhold.app.ui.theme.ZenGreen
import com.zenhold.app.ui.util.keepScreenOn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun HoldScreen(
    fullScreenGesture: Boolean,
    gestureEnabled: Boolean,
    firstDiscomfortMarked: Boolean = false,
    reduceMotion: Boolean = false,
    onStopHolding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val wasLightStatusBars = controller?.isAppearanceLightStatusBars
        val wasLightNavigationBars = controller?.isAppearanceLightNavigationBars
        // Hide the only remaining digits (the system clock) without entering Android's
        // immersive mode, which could show a first-run tutorial over the critical screen.
        controller?.hide(WindowInsetsCompat.Type.statusBars())
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            controller?.show(WindowInsetsCompat.Type.statusBars())
            wasLightStatusBars?.let { controller?.isAppearanceLightStatusBars = it }
            wasLightNavigationBars?.let { controller?.isAppearanceLightNavigationBars = it }
        }
    }
    val pulse = if (reduceMotion) 1f else {
        val transition = rememberInfiniteTransition(label = "hold target")
        val animatedPulse by transition.animateFloat(
            initialValue = .96f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(tween(2_400), RepeatMode.Reverse),
            label = "target pulse",
        )
        animatedPulse
    }
    val gestureModifier = if (fullScreenGesture && gestureEnabled) {
        Modifier.pointerInput(onStopHolding) {
            detectTapGestures(onDoubleTap = { onStopHolding() })
        }
    } else Modifier
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF010202))
            .keepScreenOn()
            .then(gestureModifier),
        contentAlignment = Alignment.Center,
    ) {
        val targetSize = minOf(maxWidth * .58f, maxHeight * .4f, 250.dp).coerceAtLeast(180.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(targetSize)
                    .scale(pulse)
                    .background(ZenGreen.copy(alpha = .075f), CircleShape)
                    .border(2.dp, ZenGreen.copy(alpha = .58f), CircleShape)
                    .semantics {
                        contentDescription = "Зона завершения задержки. Коснитесь дважды"
                        onClick(label = "Завершить задержку") { onStopHolding(); true }
                    }
                    .then(
                        if (gestureEnabled) Modifier.pointerInput(onStopHolding) {
                            detectTapGestures(onDoubleTap = { onStopHolding() })
                        } else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = ZenGreen.copy(alpha = .92f),
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        "ДВА КАСАНИЯ",
                        modifier = Modifier.padding(top = 14.dp),
                        color = ZenGreen.copy(alpha = .88f),
                        letterSpacing = 2.sp,
                        fontSize = 11.sp,
                    )
                }
            }
            Text(
                when {
                    !gestureEnabled -> "Зона завершения активируется"
                    firstDiscomfortMarked -> "Первый позыв отмечен · громкость − завершить"
                    fullScreenGesture -> "Два касания в любом месте"
                    else -> "Два касания внутри круга"
                },
                modifier = Modifier.padding(top = 28.dp),
                color = Color.White.copy(alpha = 0.34f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            if (gestureEnabled && !firstDiscomfortMarked) {
                Text(
                    "Громкость + отметить первый позыв · − завершить",
                    modifier = Modifier.padding(top = 10.dp),
                    color = Color.White.copy(alpha = 0.26f),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
