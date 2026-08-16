package com.zenhold.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zenhold.app.ui.theme.LocalNeumorphicColors

/** Two opposed shadows reproduce a soft raised surface without introducing extra colors. */
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
        Box(Modifier.matchParentSize().background(panelColor, shape))
        content()
    }
}

@Composable
fun NeumorphicAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    color: Color = Color.Unspecified,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.72f),
        label = "neumorphicPress",
    )
    NeumorphicPanel(
        modifier = modifier
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
        elevation = 9.dp,
        contentAlignment = contentAlignment,
        content = content,
    )
}
