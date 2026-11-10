package com.legionforge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LegionDarkBg = Color(0xFF0D0D1A)
private val LegionSurface = Color(0xFF16162B)
// Tokens partagés : voir Tokens.kt (LegionColors)
private val LegionAccent = Color(0xFFFFB800)
private val LegionAccentSecondary = Color(0xFFB08D57)

private val LegionColorScheme = darkColorScheme(
    primary = LegionAccent,
    secondary = LegionAccentSecondary,
    background = LegionDarkBg,
    surface = LegionSurface,
    onPrimary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun LegionForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LegionColorScheme,
        content = content
    )
}
