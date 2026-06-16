package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeonEmerald,
    onPrimary = SlateDark,
    secondary = NeonTeal,
    onSecondary = SoftWhite,
    tertiary = NeonAmber,
    background = SlateDark,
    onBackground = SoftWhite,
    surface = SlateCard,
    onSurface = SoftWhite,
    surfaceVariant = SlateCard,
    onSurfaceVariant = SoftGray,
    outline = SlateBorder
)

// Consistent dark visual style works best for viewfinder and active camera scanning
private val LightColorScheme = lightColorScheme(
    primary = NeonEmerald,
    onPrimary = SlateDark,
    secondary = NeonTeal,
    onSecondary = SoftWhite,
    tertiary = NeonAmber,
    background = SlateDark,
    onBackground = SoftWhite,
    surface = SlateCard,
    onSurface = SoftWhite,
    surfaceVariant = SlateCard,
    onSurfaceVariant = SoftGray,
    outline = SlateBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium dark layout for the scanner viewfinder
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our beautiful custom slate theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
