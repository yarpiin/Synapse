package com.af.synapse.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun SynapseTheme(
    themeOverride: Int = com.af.synapse.data.SettingsStore.getThemeMode(),
    accentColorOverride: Color = Color(com.af.synapse.data.SettingsStore.getAccentColor()),
    darkTheme: Boolean = when (themeOverride) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val currentDarkColorScheme = darkColorScheme(
        primary = accentColorOverride,
        secondary = accentColorOverride.copy(alpha = 0.8f),
        tertiary = accentColorOverride.copy(alpha = 0.6f),
        background = Color(0xFF000000),
        surface = Color(0xFF121212),
        surfaceVariant = Color(0xFF000000),
        onSurface = TextWhite,
        onSurfaceVariant = TextLightGray
    )

    val currentLightColorScheme = lightColorScheme(
        primary = accentColorOverride,
        secondary = accentColorOverride.copy(alpha = 0.8f),
        tertiary = accentColorOverride.copy(alpha = 0.6f),
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF8F9FA),
        surfaceVariant = Color(0xFFE1E3E1),
        onSurface = TextBlack,
        onSurfaceVariant = TextDarkGray
    )

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme) {
                base.copy(
                    primary = accentColorOverride,
                    background = Color.Black,
                    surface = Color(0xFF121212),
                    surfaceVariant = Color.Black,
                    onSurface = TextWhite,
                    onSurfaceVariant = TextLightGray
                )
            } else {
                base.copy(
                    primary = accentColorOverride,
                    surfaceVariant = Color(0xFFF1F3F4)
                )
            }
        }
        darkTheme -> currentDarkColorScheme
        else -> currentLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val context = view.context
            if (context is Activity) {
                val window = context.window
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
