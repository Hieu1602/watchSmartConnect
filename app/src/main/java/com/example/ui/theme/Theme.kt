package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyanNeon,
    onPrimary = HealthDarkBackground,
    primaryContainer = CyanNeonMuted,
    onPrimaryContainer = CyanNeon,
    secondary = EmeraldNeon,
    onSecondary = HealthDarkBackground,
    secondaryContainer = EmeraldNeonMuted,
    onSecondaryContainer = EmeraldNeon,
    tertiary = HeartRateRed,
    onTertiary = HealthDarkBackground,
    background = HealthDarkBackground,
    onBackground = TextPrimary,
    surface = HealthDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = HealthDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = HealthDarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = BlueAccent,
    onPrimary = HealthLightBackground,
    primaryContainer = CyanNeonMuted,
    onPrimaryContainer = BlueAccent,
    secondary = EmeraldNeon,
    onSecondary = HealthLightBackground,
    secondaryContainer = EmeraldNeonMuted,
    onSecondaryContainer = EmeraldNeon,
    tertiary = HeartRateRed,
    onTertiary = HealthLightBackground,
    background = HealthLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = HealthLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = HealthLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun HealthSyncTheme(
    darkTheme: Boolean = true, // Default to sleek tech dark theme for health wearable metrics
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
