package com.cynorra.areweokay.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = OkOrange,
    onPrimary = Color.White,
    primaryContainer = OkOrangeLight,
    onPrimaryContainer = OkOrangeShade,
    secondary = OkTeal,
    onSecondary = Color.White,
    secondaryContainer = OkTealLight,
    onSecondaryContainer = OkTeal,
    background = OkBeige,
    onBackground = OkBlack,
    surface = OkSurface,
    onSurface = OkBlack,
    outline = OkBorder,
    surfaceVariant = OkBeige,
    onSurfaceVariant = OkMuted
)

@Composable
fun AreWeOkayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We enforce the Warm Soft theme for the premium feel
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                
                val insetsController = WindowCompat.getInsetsController(window, view)
                // Enforce dark (light-status-bar) icons because background is light beige
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
