package com.avfusionapps.game_2048.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val DarkColorScheme = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80
)

@Composable
fun _2048OriginalTheme(
    darkTheme: Boolean = true, // Force dark theme
    dynamicColor: Boolean = true, content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicDarkColorScheme(context) // Always use dynamic dark if available
        }

        else -> DarkColorScheme // Always use DarkColorScheme if dynamic is not available
    }

    val view = LocalView.current
    val context = LocalContext.current
    val useDarkIcons = !darkTheme // Use light icons if not in dark theme

    // Set status bar color and enable edge-to-edge
    SideEffect {
        val window = (context as Activity).window
        // Enable edge-to-edge by not fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set the status bar color
        window.statusBarColor = PurpleDarkBackground.toArgb()

        // Control the status bar icon color (light/dark)
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = useDarkIcons
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, content = content
    )
}