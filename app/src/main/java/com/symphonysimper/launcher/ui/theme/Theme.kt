package com.symphonysimper.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

// Mono palette, matching the approved mockup. Neutral greys only.
private val MonoLight = lightColorScheme(
    primary = Color(0xFF1B1B1B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E2E2),
    onPrimaryContainer = Color(0xFF1B1B1B),
    inversePrimary = Color(0xFFC6C6C6),
    secondary = Color(0xFF474747),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE2E2E2),
    onSecondaryContainer = Color(0xFF1B1B1B),
    tertiary = Color(0xFF474747),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE2E2E2),
    onTertiaryContainer = Color(0xFF1B1B1B),
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFF9F9F9),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE2E2E2),
    onSurfaceVariant = Color(0xFF474747),
    surfaceTint = Color(0xFF1B1B1B),
    inverseSurface = Color(0xFF303030),
    inverseOnSurface = Color(0xFFF1F1F1),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF777777),
    outlineVariant = Color(0xFFC6C6C6),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFDADADA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F3),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE2E2E2),
)

private val MonoDark = darkColorScheme(
    primary = Color(0xFFECECEC),
    onPrimary = Color(0xFF1B1B1B),
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFECECEC),
    inversePrimary = Color(0xFF474747),
    secondary = Color(0xFFC6C6C6),
    onSecondary = Color(0xFF1B1B1B),
    secondaryContainer = Color(0xFF303030),
    onSecondaryContainer = Color(0xFFECECEC),
    tertiary = Color(0xFFC6C6C6),
    onTertiary = Color(0xFF1B1B1B),
    tertiaryContainer = Color(0xFF303030),
    onTertiaryContainer = Color(0xFFECECEC),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF0E0E0E),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF303030),
    onSurfaceVariant = Color(0xFFC6C6C6),
    surfaceTint = Color(0xFFECECEC),
    inverseSurface = Color(0xFFE2E2E2),
    inverseOnSurface = Color(0xFF303030),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF919191),
    outlineVariant = Color(0xFF474747),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF1F1F1F),
    surfaceDim = Color(0xFF0E0E0E),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF1B1B1B),
    surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerHighest = Color(0xFF2A2A2A),
)

/** Settings pages: grey page with white rows in light, near-black page with dark grey rows in dark. */
val ColorScheme.isDark: Boolean
    get() = this === MonoDark

val ColorScheme.pageBackground: Color
    get() = if (isDark) surface else surfaceContainer

// Material's title and label styles use weight 500, which the system font on
// some phones (Motorola's, for one) draws close to bold. Keep everything regular.
private val RegularType = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Normal),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Normal),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.Normal),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Normal),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Normal),
        labelSmall = labelSmall.copy(fontWeight = FontWeight.Normal),
    )
}

@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (isSystemInDarkTheme()) MonoDark else MonoLight,
        motionScheme = MotionScheme.expressive(),
        typography = RegularType,
        content = content,
    )
}
