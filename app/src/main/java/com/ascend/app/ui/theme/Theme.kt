package com.ascend.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * ASCEND ships one dark "System Window" theme in v1 — see design spec §6.
 * [isSystemInDarkTheme] is intentionally ignored; the app looks the same
 * regardless of the device's light/dark setting.
 */
private val AscendDarkColorScheme = darkColorScheme(
    primary = AscendColors.AccentBlue,
    onPrimary = AscendColors.TextPrimary,
    secondary = AscendColors.AccentViolet,
    onSecondary = AscendColors.TextPrimary,
    tertiary = AscendColors.Amber,
    onTertiary = AscendColors.Background,
    background = AscendColors.Background,
    onBackground = AscendColors.TextPrimary,
    surface = AscendColors.Surface,
    onSurface = AscendColors.TextPrimary,
    surfaceVariant = AscendColors.SurfaceElevated,
    onSurfaceVariant = AscendColors.TextSecondary,
    outline = AscendColors.Divider,
    error = AscendColors.Danger,
    onError = AscendColors.TextPrimary,
)

@Composable
fun AscendTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? android.app.Activity)?.window
        if (window != null) {
            window.statusBarColor = AscendColors.Background.toArgb()
            window.navigationBarColor = AscendColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = AscendDarkColorScheme,
        typography = AscendTypography,
        content = content,
    )
}
