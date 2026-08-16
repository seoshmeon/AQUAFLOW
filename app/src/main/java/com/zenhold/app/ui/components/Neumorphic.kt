package com.zenhold.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenhold.app.ui.theme.LocalNeumorphicColors

/** Tinted opposed shadows and an inner edge create depth without a bright outer glow. */
@Composable
fun NeumorphicPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    color: Color = Color.Unspecified,
    elevation: Dp = 12.dp,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val neoColors = LocalNeumorphicColors.current
    val panelColor = if (color == Color.Unspecified) neoColors.panel else color
    Box(modifier = modifier, contentAlignment = contentAlignment) {
        Box(
            Modifier.matchParentSize()
                .offset((-4).dp, (-4).dp)
                .shadow(elevation, shape, ambientColor = neoColors.lightShadow, spotColor = neoColors.lightShadow)
                .background(panelColor, shape),
        )
        Box(
            Modifier.matchParentSize()
                .offset(5.dp, 6.dp)
                .shadow(elevation, shape, ambientColor = neoColors.darkShadow, spotColor = neoColors.darkShadow)
                .background(panelColor, shape),
        )
        Box(
            Modifier.matchParentSize()
                .background(panelColor, shape)
                .background(
                    Brush.verticalGradient(
                        0f to neoColors.lightShadow.copy(alpha = .16f),
                        .42f to Color.Transparent,
                        1f to neoColors.darkShadow.copy(alpha = .08f),
                    ),
                    shape,
                )
                .border(
                    width = .75.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            neoColors.lightShadow.copy(alpha = .5f),
                            neoColors.darkShadow.copy(alpha = .18f),
                        ),
                    ),
                    shape = shape,
                ),
        )
        content()
    }
}

@Composable
fun NeumorphicAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(7.dp),
    color: Color = Color.Unspecified,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.72f),
        label = "neumorphicPress",
    )
    val pressedOffset by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 0.dp,
        animationSpec = spring(stiffness = 650f, dampingRatio = .78f),
        label = "neumorphicDepth",
    )
    val actionElevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 14.dp,
        animationSpec = spring(stiffness = 620f, dampingRatio = .8f),
        label = "neumorphicElevation",
    )
    NeumorphicPanel(
        modifier = modifier
            .offset(y = pressedOffset)
            .graphicsLayer {
                scaleX = pressedScale
                scaleY = pressedScale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        shape = shape,
        color = color,
        elevation = actionElevation,
        contentAlignment = contentAlignment,
        content = content,
    )
}
