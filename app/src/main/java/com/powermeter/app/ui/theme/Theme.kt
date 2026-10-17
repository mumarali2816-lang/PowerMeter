package com.powermeter.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = ElectricBlue,
    tertiary = AccentOrange,
    background = DeepObsidian,
    surface = CardSurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = TextHighEmphasis,
    onSurface = TextHighEmphasis,
    error = DangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    secondary = NeonCyan,
    tertiary = AccentOrange,
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun PowerMeterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
