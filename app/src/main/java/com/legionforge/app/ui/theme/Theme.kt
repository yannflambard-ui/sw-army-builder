package com.legionforge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Thème Material3 branché sur les design tokens (fin du « design system fantôme »).
 * LegionColors est la source de vérité unique des couleurs ; LegionType fixe l'échelle
 * typographique avec un plancher de 11sp pour la lisibilité à côté de la table.
 */
private val LegionColorScheme = darkColorScheme(
    primary = LegionColors.Accent,
    secondary = LegionColors.AccentMuted,
    tertiary = LegionColors.Info,
    background = LegionColors.Background,
    surface = LegionColors.Surface,
    surfaceVariant = LegionColors.SurfaceRaised,
    error = LegionColors.Danger,
    onPrimary = LegionColors.TextPrimary,
    onSecondary = LegionColors.TextPrimary,
    onBackground = LegionColors.TextPrimary,
    onSurface = LegionColors.TextPrimary,
    onSurfaceVariant = LegionColors.TextSecondary,
    onError = LegionColors.TextPrimary,
)

/**
 * Échelle typographique dérivée de LegionType. Le plancher 11sp (Caption) est appliqué
 * aux styles les plus petits pour respecter le critère d'accessibilité.
 */
private val LegionTypography = Typography(
    displayLarge = TextStyle(fontSize = LegionType.Display, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = LegionType.Display, fontWeight = FontWeight.Bold),
    displaySmall = TextStyle(fontSize = LegionType.Display, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = LegionType.Title, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = LegionType.Title, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = LegionType.Subtitle, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = LegionType.Title, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = LegionType.Subtitle, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontSize = LegionType.Subtitle, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = LegionType.Body),
    bodyMedium = TextStyle(fontSize = LegionType.Body),
    bodySmall = TextStyle(fontSize = LegionType.Caption),
    labelLarge = TextStyle(fontSize = LegionType.Label, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = LegionType.Label),
    labelSmall = TextStyle(fontSize = LegionType.Caption),
)

@Composable
fun LegionForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LegionColorScheme,
        typography = LegionTypography,
        content = content
    )
}
