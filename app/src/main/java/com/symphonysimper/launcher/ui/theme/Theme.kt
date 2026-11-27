package com.symphonysimper.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material 3 monochrome scheme (material-color-utilities SchemeMonochrome).
private val MonoLight = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFE2E2E2),
    primaryContainer = Color(0xFF3B3B3B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFC6C6C6),
    secondary = Color(0xFF5E5E5E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4D4D4),
    onSecondaryContainer = Color(0xFF1B1B1B),
    tertiary = Color(0xFF3B3B3B),
    onTertiary = Color(0xFFE2E2E2),
    tertiaryContainer = Color(0xFF747474),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFF9F9F9),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE2E2E2),
    onSurfaceVariant = Color(0xFF474747),
    surfaceTint = Color(0xFF5E5E5E),
    inverseSurface = Color(0xFF303030),
    inverseOnSurface = Color(0xFFF1F1F1),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFC6C6C6),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF9F9F9),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE2E2E2),
    surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDADADA),
)

private val MonoDark = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF1B1B1B),
    primaryContainer = Color(0xFFD4D4D4),
    onPrimaryContainer = Color(0xFF000000),
    inversePrimary = Color(0xFF5E5E5E),
    secondary = Color(0xFFC6C6C6),
    onSecondary = Color(0xFF1B1B1B),
    secondaryContainer = Color(0xFF474747),
    onSecondaryContainer = Color(0xFFE2E2E2),
    tertiary = Color(0xFFE2E2E2),
    onTertiary = Color(0xFF1B1B1B),
    tertiaryContainer = Color(0xFF919191),
    onTertiaryContainer = Color(0xFF000000),
    background = Color(0xFF131313),
    onBackground = Color(0xFFE2E2E2),
    surface = Color(0xFF131313),
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF474747),
    onSurfaceVariant = Color(0xFFC6C6C6),
    surfaceTint = Color(0xFFC6C6C6),
    inverseSurface = Color(0xFFE2E2E2),
    inverseOnSurface = Color(0xFF303030),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF919191),
    outlineVariant = Color(0xFF474747),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF393939),
    surfaceContainer = Color(0xFF1F1F1F),
    surfaceContainerHigh = Color(0xFF2A2A2A),
    surfaceContainerHighest = Color(0xFF353535),
    surfaceContainerLow = Color(0xFF1B1B1B),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceDim = Color(0xFF131313),
)

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (isSystemInDarkTheme()) MonoDark else MonoLight,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
