package com.zenhold.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import kotlin.math.PI
import kotlin.math.sin

/** Low-contrast water movement shared by non-hold screens. It never displays timing data. */
@Composable
fun WaveBackdrop(modifier: Modifier = Modifier, reduceMotion: Boolean = false) {
    val accent = MaterialTheme.colorScheme.primary
    val phase = if (reduceMotion) 0f else {
        val transition = rememberInfiniteTransition(label = "ambient waves")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
            label = "wave phase",
        )
        animatedPhase
    }

    Canvas(modifier) {
        val layers = listOf(
            Triple(.62f, 22.dp.toPx(), .11f),
            Triple(.73f, 30.dp.toPx(), .075f),
            Triple(.86f, 18.dp.toPx(), .055f),
        )
        layers.forEachIndexed { layerIndex, (baseline, amplitude, alpha) ->
            val path = Path()
            val segments = 56
            repeat(segments + 1) { index ->
                val x = size.width * index / segments
                val angle = phase * (if (layerIndex % 2 == 0) 1f else -0.72f) +
                    index / segments.toFloat() * (2f * PI).toFloat()
                val y = size.height * baseline + sin(angle) * amplitude
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0f),
                        accent.copy(alpha = alpha),
                        accent.copy(alpha = alpha * .58f),
                        accent.copy(alpha = 0f),
                    ),
                ),
                style = Stroke(width = (1.25f + layerIndex * .55f).dp.toPx()),
            )
        }
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = .055f), accent.copy(alpha = 0f)),
                center = Offset(size.width * .84f, size.height * .2f),
                radius = size.minDimension * .42f,
            ),
            radius = size.minDimension * .42f,
            center = Offset(size.width * .84f, size.height * .2f),
        )
    }
}
