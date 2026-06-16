package com.avfusionapps.game_2048.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val DarkColorScheme = darkColorScheme(
    primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80
)

/**
 * Extension function to find the Activity from a Context.
 * This is useful in Compose Previews where the context might be a BridgeContext.
 */
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun _2048OriginalTheme(
    theme: GameTheme = GameTheme.NeonPink,
    darkTheme: Boolean = theme.isDark,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Provide the current GameTheme using a CompositionLocal
    CompositionLocalProvider(LocalGameTheme provides theme) {
        val colorScheme = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicDarkColorScheme(context)
            }
            else -> {
                // Create color scheme based on theme
                if (theme.isDark) {
                    darkColorScheme(
                        primary = theme.primaryColor,
                        secondary = theme.secondaryColor,
                        background = theme.backgroundColor,
                        surface = theme.surfaceColor,
                        onBackground = theme.textColor,
                        onSurface = theme.textColor
                    )
                } else {
                    androidx.compose.material3.lightColorScheme(
                        primary = theme.primaryColor,
                        secondary = theme.secondaryColor,
                        background = theme.backgroundColor,
                        surface = theme.surfaceColor,
                        onBackground = theme.textColor,
                        onSurface = theme.textColor
                    )
                }
            }
        }

        val view = LocalView.current
        val context = LocalContext.current
        val useDarkIcons = !darkTheme

        // Set status bar color and enable edge-to-edge
        SideEffect {
            // Using findActivity() safely to avoid ClassCastException in Compose Previews
            context.findActivity()?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = theme.backgroundColor.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = useDarkIcons
            }
        }

        MaterialTheme(
            colorScheme = colorScheme, typography = Typography, content = content
        )
    }
}

// Add a CompositionLocal to provide GameTheme
val LocalGameTheme = androidx.compose.runtime.staticCompositionLocalOf<GameTheme> { GameTheme.NeonPink }