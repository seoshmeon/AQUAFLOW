package com.zenhold.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ZenGreen = Color(0xFF557D78)
val NeoBackground = Color(0xFFDADCDD)
val NeoSurface = Color(0xFFE0E2E3)
val NeoDarkShadow = Color(0x667F8588)
val NeoLightShadow = Color(0xE6FFFFFF)
val Charcoal = Color(0xFF303436)

data class NeumorphicColors(
    val panel: Color,
    val darkShadow: Color,
    val lightShadow: Color,
)

private val LightNeumorphicColors = NeumorphicColors(
    panel = NeoBackground,
    darkShadow = NeoDarkShadow,
    lightShadow = NeoLightShadow,
)

private val DarkNeumorphicColors = NeumorphicColors(
    panel = Color(0xFF1C2122),
    darkShadow = Color(0xB3000000),
    lightShadow = Color(0x26667573),
)

val LocalNeumorphicColors = staticCompositionLocalOf { LightNeumorphicColors }

private val ZenColors = androidx.compose.material3.lightColorScheme(
    primary = ZenGreen,
    onPrimary = Color.White,
    secondary = Color(0xFF65716F),
    background = NeoBackground,
    onBackground = Charcoal,
    surface = NeoSurface,
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFC8CBCC),
    onSurfaceVariant = Color(0xFF666D70),
)

private val ZenDarkColors = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF79A9A2),
    onPrimary = Color(0xFF10211F),
    secondary = Color(0xFF9AA9A6),
    background = Color(0xFF171B1C),
    onBackground = Color(0xFFE7EBEA),
    surface = Color(0xFF1C2122),
    onSurface = Color(0xFFE7EBEA),
    surfaceVariant = Color(0xFF2B3233),
    onSurfaceVariant = Color(0xFFAAB3B1),
)

private val ZenShapes = Shapes(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(7.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

private val ZenTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1.1).sp,
        fontWeight = FontWeight.Normal,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 33.sp,
        letterSpacing = (-.6).sp,
        fontWeight = FontWeight.Medium,
    ),
    headlineSmall = TextStyle(
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = (-.3).sp,
        fontWeight = FontWeight.Medium,
    ),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
)

@Composable
fun ZenHoldTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNeumorphicColors provides if (darkTheme) DarkNeumorphicColors else LightNeumorphicColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) ZenDarkColors else ZenColors,
            shapes = ZenShapes,
            typography = ZenTypography,
            content = content,
        )
    }
}
