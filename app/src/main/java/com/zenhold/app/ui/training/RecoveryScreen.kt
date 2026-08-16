package com.zenhold.app.ui.training

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenhold.app.domain.model.TrainingState
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.util.formatDuration
import com.zenhold.app.ui.util.keepScreenOn

@Composable
fun RecoveryScreen(state: TrainingState.Recovering, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "recovery breath")
    val breathScale by transition.animateFloat(
        initialValue = .72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_500), RepeatMode.Reverse),
        label = "breathing circle",
    )
    val showResult = remember(state.completedAttempt) { true }
    val progress = if (state.totalRecoveryMillis == 0L) 1f
    else 1f - state.remainingMillis.toFloat() / state.totalRecoveryMillis

    BoxWithConstraints(modifier.fillMaxSize().keepScreenOn()) {
      val pagePadding = if (maxWidth < 360.dp || maxHeight < 650.dp) 18.dp else 28.dp
      val circleSize = minOf(maxWidth * .72f, maxHeight * .42f, 300.dp).coerceAtLeast(176.dp)
      Column(
        Modifier.align(Alignment.Center).fillMaxSize().padding(pagePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("РЕЗУЛЬТАТ", color = accent, letterSpacing = 3.sp, fontSize = 12.sp)
            AnimatedVisibility(visible = showResult, enter = fadeIn(tween(900)) + scaleIn(tween(900))) {
                Text(
                    formatDuration(state.holdDurationMillis),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Light,
                )
            }
            Text("Мягко вернитесь к дыханию", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        NeumorphicPanel(
            modifier = Modifier.size(circleSize),
            shape = CircleShape,
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(circleSize - 32.dp)) {
                drawCircle(accent.copy(alpha = .07f), radius = size.minDimension * .45f * breathScale)
                drawArc(accent.copy(alpha = .16f), -90f, 360f, false, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                drawArc(accent, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ОТДЫХ", letterSpacing = 2.sp, fontSize = 11.sp, color = accent)
                Text(formatDuration(state.remainingMillis), fontSize = 38.sp, fontWeight = FontWeight.Light)
                Text(if (breathScale > .86f) "Вдох" else "Выдох", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Text(
            "Завершён подход ${state.completedAttempt} из ${state.totalAttempts}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
}
