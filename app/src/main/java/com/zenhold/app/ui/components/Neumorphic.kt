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
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(7.dp),
    color: Color = Color.Unspecified,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressedScale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.72f),
        label = "neumorphicPress",
    )
    val pressedOffset by animateDpAsState(
        targetValue = if (enabled && isPressed) 2.dp else 0.dp,
        animationSpec = spring(stiffness = 650f, dampingRatio = .78f),
        label = "neumorphicDepth",
    )
    val actionElevation by animateDpAsState(
        targetValue = if (enabled && isPressed) 4.dp else 14.dp,
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
                enabled = enabled,
                onClick = onClick,
            ),
        shape = shape,
        color = color,
        elevation = actionElevation,
        contentAlignment = contentAlignment,
        content = content,
    )
}

/**
 * Primary action based on the selected Neo-Tactile reference: a saturated cobalt face,
 * refractive top edge and a deep directional shadow that collapses under the finger.
 */
@Composable
fun NeoTactilePrimaryAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val faceOffset by animateDpAsState(
        targetValue = if (enabled && isPressed) 7.dp else 0.dp,
        animationSpec = spring(stiffness = 720f, dampingRatio = .76f),
        label = "neoTactileFaceOffset",
    )
    val faceScale by animateFloatAsState(
        targetValue = if (enabled && isPressed) .992f else 1f,
        animationSpec = spring(stiffness = 760f, dampingRatio = .8f),
        label = "neoTactileFaceScale",
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (enabled && isPressed) .34f else .82f,
        animationSpec = spring(stiffness = 620f, dampingRatio = .82f),
        label = "neoTactileShadowAlpha",
    )

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.matchParentSize()
                .offset(x = 8.dp, y = 11.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = shape,
                    ambientColor = Color(0xFF071A46).copy(alpha = shadowAlpha),
                    spotColor = Color(0xFF071A46).copy(alpha = shadowAlpha),
                )
                .background(Color(0xFF142A65).copy(alpha = shadowAlpha), shape),
        )
        Box(
            Modifier.matchParentSize()
                .offset(y = faceOffset)
                .graphicsLayer {
                    scaleX = faceScale
                    scaleY = faceScale
                }
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF4D88FF), Color(0xFF315BF4)),
                    ),
                    shape,
                )
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = .22f),
                        .28f to Color.Transparent,
                        1f to Color(0xFF173BC8).copy(alpha = .18f),
                    ),
                    shape,
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = .5f), Color(0xFF173BC8).copy(alpha = .55f)),
                    ),
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
