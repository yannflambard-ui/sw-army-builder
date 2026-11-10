package com.legionforge.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens (étape 0 du redesign).
 * Noms sémantiques des couleurs les plus utilisées dans l'app.
 * Valeurs identiques à l'existant : ce fichier ne change aucun rendu.
 * Les écrans seront migrés vers ces tokens étape par étape.
 */
object LegionColors {
    // Fonds
    val Background = Color(0xFF0A0E15)
    val Surface = Color(0xFF111827)
    val SurfaceRaised = Color(0xFF1A2330)
    val Border = Color(0xFF2A3A4A)

    // Accent (or des cartes)
    val Accent = Color(0xFFFFC857)
    val AccentStrong = Color(0xFFFFB800)
    val AccentMuted = Color(0xFFB08D57)

    // Texte
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9EACBC)
    val TextTertiary = Color(0xFF5A6A7A)

    // Statuts
    val Success = Color(0xFF77D9A7)
    val SuccessSurface = Color(0xFF1E3A2A)
    val Danger = Color(0xFFFF6B6B)
    val DangerSurface = Color(0xFF3B2224)
    val Warning = Color(0xFFF59E0B)
    val Info = Color(0xFF38BDF8)
}

/** Échelle typographique : 6 niveaux, plancher 11sp pour la lisibilité. */
object LegionType {
    val Display = 22.sp
    val Title = 18.sp
    val Subtitle = 15.sp
    val Body = 13.sp
    val Label = 12.sp
    val Caption = 11.sp
}

/** Dimensions : cibles tactiles minimales et espacements. */
object LegionDims {
    /** Taille minimale d'une cible tactile (Material). */
    val TouchMin = 48.dp
    val SpaceXs = 4.dp
    val SpaceS = 8.dp
    val SpaceM = 12.dp
    val SpaceL = 16.dp
    val Radius = 12.dp
}
