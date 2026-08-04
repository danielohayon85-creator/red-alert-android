package com.shomerapp.alerts.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dark-first by design (§8) — the app stays dark regardless of the system theme setting.
// A white 3am wake screen slows reactions, so we never branch on isSystemInDarkTheme().
private val AzakonColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = OnAmberPrimary,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceMutedDark,
    error = StatusInactiveRed,
)

@Composable
fun AzakonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AzakonColorScheme,
        typography = AzakonTypography,
        content = content,
    )
}
