package com.zenhold.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

@Composable
fun ZenHoldTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalNeumorphicColors provides if (darkTheme) DarkNeumorphicColors else LightNeumorphicColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) ZenDarkColors else ZenColors,
            content = content,
        )
    }
}
