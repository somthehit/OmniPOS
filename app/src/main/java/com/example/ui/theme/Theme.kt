package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PosPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Slate800,
    onPrimaryContainer = Color(0xFF93C5FD),
    secondary = PosEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = PosAmber,
    onTertiary = Color.Black,
    background = PosDarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = PosDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = PosDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Slate600,
    error = PosRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PosPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = PosEmerald,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF065F46),
    tertiary = PosAmber,
    onTertiary = Color.White,
    background = Color.White,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = PosLightSurfaceVariant,
    onSurfaceVariant = Slate700,
    outline = Color(0xFFE2E8F0),
    error = PosRose,
    onError = Color.White
)

@Composable
fun OmniPosTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
