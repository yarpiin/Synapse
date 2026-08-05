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

private val DarkColorScheme = darkColorScheme(
    primary = PixelBlue,
    secondary = Color(0xFF8AB4F8),
    tertiary = Color(0xFFADCCF7),
    background = Color(0xFF000000), // Pure black for OLED
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF000000), // Black as requested for sliders/progress tracks
    onSurface = TextWhite,
    onSurfaceVariant = TextLightGray
)

private val LightColorScheme = lightColorScheme(
    primary = PixelBlue,
    secondary = Color(0xFF1967D2),
    tertiary = Color(0xFF185ABC),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF8F9FA),
    surfaceVariant = Color(0xFFE1E3E1),
    onSurface = TextBlack,
    onSurfaceVariant = TextDarkGray
)

@Composable
fun SynapseTheme(
    themeOverride: Int = com.af.synapse.data.SettingsStore.getThemeMode(),
    darkTheme: Boolean = when (themeOverride) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme) {
                base.copy(
                    primary = PixelBlue,
                    background = Color.Black,
                    surface = Color(0xFF121212),
                    surfaceVariant = Color.Black,
                    onSurface = TextWhite,
                    onSurfaceVariant = TextLightGray
                )
            } else {
                base.copy(
                    primary = PixelBlue,
                    surfaceVariant = Color(0xFFF1F3F4)
                )
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
