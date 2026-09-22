package com.strategy.blackjacktrainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun BlackjackTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = darkColorScheme(
        primary = Emerald,
        onPrimary = OnPrimaryText,
        secondary = Gold,
        background = TableBg,
        surface = SurfaceDark,
        surfaceVariant = SurfaceElevated,
        error = MistakeRed,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        outline = OutlineSoft
    )

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}

