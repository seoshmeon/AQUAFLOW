package com.zenhold.app.ui.training

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenhold.app.domain.model.TrainingState
import com.zenhold.app.ui.components.NeumorphicPanel
import com.zenhold.app.ui.util.formatDuration
import com.zenhold.app.ui.util.keepScreenOn

@Composable
fun PreparationScreen(state: TrainingState.Preparation, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "preparation")
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(5_500), RepeatMode.Reverse),
        label = "slow breath",
    )
    BoxWithConstraints(modifier.fillMaxSize().keepScreenOn(), contentAlignment = Alignment.Center) {
        val circleSize = minOf(maxWidth * .76f, maxHeight * .48f, 330.dp).coerceAtLeast(190.dp)
        NeumorphicPanel(
            modifier = Modifier.size(circleSize),
            shape = CircleShape,
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(circleSize - 32.dp)) {
                drawCircle(accent.copy(alpha = 0.07f), radius = size.minDimension * .48f * pulse)
                drawCircle(accent.copy(alpha = 0.22f), radius = size.minDimension * .34f * pulse, style = Stroke(2.dp.toPx()))
                drawCircle(accent.copy(alpha = 0.18f), radius = size.minDimension * .19f * pulse)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ПОДГОТОВКА", letterSpacing = 3.sp, color = accent, fontSize = 12.sp)
            Text(formatDuration(state.remainingMillis), fontSize = 48.sp, fontWeight = FontWeight.Light)
            Text("Дышите спокойно", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "Подход ${state.attempt} из ${state.totalAttempts}",
            modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
