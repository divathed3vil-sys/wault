package com.wault.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Wault theme: neutral warm surfaces, exactly one accent in view (the brand
 * emerald, re-tinted per account context where the design calls for it).
 * surfaceTint is neutralised so elevated surfaces stay quietly neutral.
 */
private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF5E7),
    onPrimaryContainer = Color(0xFF04382C),
    secondary = Brand,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceAlt,
    onSecondaryContainer = LightText,
    tertiary = AccentTeal,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceAlt,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = AccentRose,
    onError = Color.White,
    surfaceTint = Color.Transparent,
)

private val DarkColors = darkColorScheme(
    primary = BrandBright,
    onPrimary = DarkBg,
    primaryContainer = Color(0xFF0A5C41),
    onPrimaryContainer = Color(0xFFB8F4DC),
    secondary = BrandBright,
    onSecondary = DarkBg,
    secondaryContainer = DarkSurfaceAlt,
    onSecondaryContainer = DarkText,
    tertiary = AccentTeal,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = AccentRose,
    onError = Color.White,
    surfaceTint = Color.Transparent,
)

@Composable
fun WaultTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WaultTypography,
        content = content,
    )
}
