package com.szainabbas.ummi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

/** Follows the More screen's Light / Dark / System control. */
enum class UmmiThemeMode { LIGHT, DARK, SYSTEM }

val LocalUmmiColors = compositionLocalOf { LightUmmiColors }

@Composable
fun UmmiTheme(
    mode: UmmiThemeMode = UmmiThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        UmmiThemeMode.LIGHT -> false
        UmmiThemeMode.DARK -> true
        UmmiThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val tokens = if (dark) DarkUmmiColors else LightUmmiColors

    val colorScheme = if (dark) {
        darkColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            primaryContainer = tokens.pc,
            onPrimaryContainer = tokens.onpc,
            secondaryContainer = tokens.ac,
            onSecondaryContainer = tokens.accentText,
            background = tokens.bg,
            onBackground = tokens.ink,
            surface = tokens.surface,
            onSurface = tokens.ink,
            surfaceVariant = tokens.surface2,
            onSurfaceVariant = tokens.ink2,
            outline = tokens.line,
            outlineVariant = tokens.line,
            error = tokens.danger,
        )
    } else {
        lightColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            primaryContainer = tokens.pc,
            onPrimaryContainer = tokens.onpc,
            secondaryContainer = tokens.ac,
            onSecondaryContainer = tokens.accentText,
            background = tokens.bg,
            onBackground = tokens.ink,
            surface = tokens.surface,
            onSurface = tokens.ink,
            surfaceVariant = tokens.surface2,
            onSurfaceVariant = tokens.ink2,
            outline = tokens.line,
            outlineVariant = tokens.line,
            error = tokens.danger,
        )
    }

    CompositionLocalProvider(LocalUmmiColors provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = UmmiTypography,
            content = content,
        )
    }
}

/** Shorthand for `LocalUmmiColors.current`, for the tokens without a ColorScheme slot. */
object Ummi {
    val colors: UmmiColors
        @Composable get() = LocalUmmiColors.current
}

