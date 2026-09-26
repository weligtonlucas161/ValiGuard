package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Material 3 Expressive Dark Theme with Blue and Red Details
val ExpressiveDarkColorScheme =
    darkColorScheme(
        primary = BlueExpressive, // Azul Expressivo
        onPrimary = BlueOnExpressive,
        primaryContainer = BlueExpressiveContainer,
        onPrimaryContainer = BlueOnExpressiveContainer,
        secondary = RedExpressive, // Vermelho Expressivo
        onSecondary = RedOnExpressive,
        secondaryContainer = RedExpressiveContainer,
        onSecondaryContainer = RedOnExpressiveContainer,
        tertiary = OrangeMarkdown, // Laranja Rebaixa (≤15d)
        onTertiary = OrangeOnMarkdown,
        tertiaryContainer = OrangeMarkdownContainer,
        onTertiaryContainer = OrangeOnMarkdown,
        background = DarkBackground,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkBorder,
        outlineVariant = DarkBorderSubtle,
        error = RedExpressive,
        onError = RedOnExpressive,
        errorContainer = RedExpressiveContainer,
        onErrorContainer = RedOnExpressiveContainer
    )

private val ExpressiveLightColorScheme =
    lightColorScheme(
        primary = BlueExpressiveDark,
        onPrimary = BlueOnExpressive,
        primaryContainer = BlueExpressiveContainer,
        onPrimaryContainer = BlueOnExpressiveContainer,
        secondary = RedExpressive,
        onSecondary = RedOnExpressive,
        secondaryContainer = RedExpressiveContainer,
        onSecondaryContainer = RedOnExpressiveContainer,
        tertiary = OrangeMarkdown,
        onTertiary = OrangeOnMarkdown,
        tertiaryContainer = OrangeMarkdownContainer,
        onTertiaryContainer = OrangeOnMarkdown,
        background = DarkBackground,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkBorder,
        outlineVariant = DarkBorderSubtle,
        error = RedExpressive,
        onError = RedOnExpressive,
        errorContainer = RedExpressiveContainer,
        onErrorContainer = RedOnExpressiveContainer
    )

@Composable
fun MyApplicationTheme(
    // Always default to Dark Theme as explicitly requested by user
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ExpressiveDarkColorScheme else ExpressiveLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
