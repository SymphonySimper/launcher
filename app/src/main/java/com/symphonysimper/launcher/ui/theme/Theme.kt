package com.symphonysimper.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

val ColorScheme.isDark: Boolean
    get() = surface.luminance() < 0.5f

/** Settings pages: tinted page with brighter rows in light, near-black page with lifted rows in dark. */
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

/** System (Material You) colours, so the launcher follows the wallpaper palette and dark mode. */
@Composable
fun LauncherTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    MaterialExpressiveTheme(
        colorScheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
        motionScheme = MotionScheme.expressive(),
        typography = RegularType,
        content = content,
    )
}
