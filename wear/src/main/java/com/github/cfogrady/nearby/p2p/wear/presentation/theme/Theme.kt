package com.github.cfogrady.nearby.p2p.wear.presentation.theme

import android.content.Context
import android.util.Log
import androidx.wear.compose.material3.dynamicColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

internal val defaultColorScheme = ColorScheme(
    primary = Color(0xFF1F3701),
    primaryDim = Color(0xFF1F3701),
    primaryContainer = Color(0xFFCDEDA3),
    onPrimary = Color(0xFF7AC55C),
    onPrimaryContainer = Color(0xFFBFCBAD),
    secondary = Color(0xFF2A331E),
    secondaryDim = Color(0xFF2A331E),
    secondaryContainer = Color(0xFFDCE7C8),
    onSecondary = Color(0xFF404A33),
    onSecondaryContainer = Color(0xFFA0D0CB),
    tertiary = Color(0xFF003735),
    tertiaryDim = Color(0xFF003735),
    tertiaryContainer = Color(0xFFBCECE7),
    onTertiary = Color(0xFF1F4E4B),
    onTertiaryContainer = Color(0xFFFFB4AB),
    onSurface = Color(0xFF44483D),
    onSurfaceVariant = Color(0xFF8F9285),
    surfaceContainerLow = Color(0xFF1E201A),
    surfaceContainer = Color(0xFF282B24),
    surfaceContainerHigh = Color(0xFF33362E),
    outline = Color(0xFF44483D),
    outlineVariant = Color(0xFF000000),
    background = Color(0xFF12140E),
    onBackground = Color(0xFFE2E3D8),
    error = Color(0xFF690005),
    onError = Color(0xFF93000A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFFE2E3D8),
)

@Composable
fun NearbyTheme(context: Context,
                content: @Composable () -> Unit
) {
    var colorScheme = dynamicColorScheme(context)
    if(colorScheme == null) {
        Log.i("Theme", "dynamicColorScheme is unavailable. Using Default.")
        colorScheme = defaultColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}