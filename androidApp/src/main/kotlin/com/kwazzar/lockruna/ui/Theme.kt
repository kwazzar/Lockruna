@file:Suppress("FunctionNaming", "LongMethod")

package com.kwazzar.lockruna.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = DarkZenPalette.Accent,
            onPrimary = DarkZenPalette.TextPrimary,
            primaryContainer = Color(0x33DB34F2),
            onPrimaryContainer = DarkZenPalette.TextPrimary,
            secondary = DarkZenPalette.SecondaryAccent,
            onSecondary = DarkZenPalette.TextPrimary,
            secondaryContainer = Color(0x26BF5AF2),
            onSecondaryContainer = DarkZenPalette.TextPrimary,
            tertiary = DarkZenPalette.TertiaryAccent,
            onTertiary = DarkZenPalette.TextPrimary,
            tertiaryContainer = Color(0x1A0A84FF),
            onTertiaryContainer = DarkZenPalette.TextPrimary,
            background = DarkZenPalette.Background,
            onBackground = DarkZenPalette.TextPrimary,
            surface = DarkZenPalette.Background,
            onSurface = DarkZenPalette.TextPrimary,
            surfaceVariant = Color(0xFF1B202F),
            onSurfaceVariant = Color(0xFF9FA1A7),
            surfaceContainerLowest = DarkZenPalette.Background,
            surfaceContainerLow = Color(0xFF141A28),
            surfaceContainer = Color(0xFF1B202F),
            surfaceContainerHigh = Color(0xFF222735),
            surfaceContainerHighest = Color(0xFF272C3A),
            outline = Color(0xFF272C3A),
            outlineVariant = DarkZenPalette.Divider,
            error = DarkZenPalette.Danger,
            onError = DarkZenPalette.Background,
            errorContainer = Color(0x33FF453A),
            onErrorContainer = Color(0xFFFFB4AB),
            scrim = Color.Black,
            surfaceTint = Color.Transparent
        ),
        content = content
    )
}