package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF005327),
    onPrimaryContainer = Color(0xFF78FFA8),
    secondary = NeonCyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004E59),
    onSecondaryContainer = Color(0xFF97F0FF),
    tertiary = NeonPurple,
    onTertiary = Color(0xFF270068),
    tertiaryContainer = Color(0xFF451996),
    onTertiaryContainer = Color(0xFFEADBFF),
    error = NeonRed,
    onError = Color(0xFF680008),
    errorContainer = Color(0xFF920011),
    onErrorContainer = Color(0xFFFFDAD7),
    background = DarkSurface,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    outlineVariant = Color(0xFF1E2942)
)

private val LightColorScheme = DarkColorScheme // Always enforce sleek terminal aesthetic

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
