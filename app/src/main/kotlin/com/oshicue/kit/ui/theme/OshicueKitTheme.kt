package com.oshicue.kit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

@Composable
fun OshicueKitTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = if (dark) KitPalette.dark else KitPalette.light
    val colorScheme = if (dark) {
        darkColorScheme(
            primary = palette.accentFill,
            onPrimary = palette.onAccent,
            surface = palette.surface,
            onSurface = palette.ink,
            background = palette.background,
            onBackground = palette.ink,
            outline = palette.line,
            surfaceVariant = palette.surfaceMuted,
            onSurfaceVariant = palette.muted
        )
    } else {
        lightColorScheme(
            primary = palette.accentFill,
            onPrimary = palette.onAccent,
            surface = palette.surface,
            onSurface = palette.ink,
            background = palette.background,
            onBackground = palette.ink,
            outline = palette.line,
            surfaceVariant = palette.surfaceMuted,
            onSurfaceVariant = palette.muted
        )
    }
    CompositionLocalProvider(LocalKitPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}
