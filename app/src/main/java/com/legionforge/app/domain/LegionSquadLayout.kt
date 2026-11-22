package com.legionforge.app.domain

import kotlin.math.ceil
import kotlin.math.hypot

/**
 * Géométrie d'une ESCQADE Legion affichée comme PLUSIEURS PETITS SOCLES.
 *
 * Calcul PUR, sans Compose, testable en JVM — source de vérité partagée par le
 * rendu du plateau (chaque figurine = un disque) et l'emprise de collision.
 *
 * CHOIX D'AFFICHAGE (pas une règle du rulebook) : les figurines sont rangées en
 * COLONNES DE 2 DE LARGE (demande utilisateur). L'espacement est le diamètre d'un
 * socle : les socles sont serrés (empilement compact d'affichage, pas une règle chiffrée).
 *
 * RETRAIT DES PERTES : [maxWounds] = health × miniCount, donc une figurine a
 * `health = maxWounds / miniCount` PV. Quand les PV tombent, on retire les
 * derniers socles de la grille (du bas vers le haut) — l'unité « recule ».
 * Une unité encore vivante garde AU MOINS 1 socle, même blessée.
 */
object LegionSquadLayout {

    /** Colonnes de la formation : 2 de large (ou 1 si une seule figurine). */
    fun columns(miniCount: Int): Int = if (miniCount.coerceAtLeast(1) == 1) 1 else 2

    /** Lignes de la formation. */
    fun rows(miniCount: Int): Int {
        val n = miniCount.coerceAtLeast(1)
        return ceil(n.toFloat() / columns(n)).toInt().coerceAtLeast(1)
    }

    /**
     * Nombre de socles encore sur la table.
     * 0 si l'unité est vaincue (tous les socles retirés) ; sinon
     * ceil(wounds / health) borné entre 1 et miniCount.
     */
    fun aliveMinis(miniCount: Int, wounds: Int, maxWounds: Int, defeated: Boolean): Int {
        if (defeated || wounds <= 0) return 0
        val n = miniCount.coerceAtLeast(1)
        if (n == 1) return 1 // héros/véhicule : 1 socle tant qu'il reste des PV
        val health = (maxWounds.coerceAtLeast(1) / n).coerceAtLeast(1)
        val alive = ceil(wounds.toFloat() / health.toFloat()).toInt().coerceAtLeast(1)
        return alive.coerceAtMost(n)
    }

    /**
     * Centre d'un socle en mm RELATIF au centre de l'escouade.
     * Index 0 = premier socle (haut-gauche). Les pertes sont retirées par la fin
     * de la liste, donc les premiers restent Always posés.
     */
    fun offsetMm(index: Int, miniCount: Int, baseSize: String): Pair<Float, Float> {
        val d = LegionTabletopGeometry.baseDiameterMm(baseSize)
        val cols = columns(miniCount)
        val rows = rows(miniCount)
        val col = index % cols
        val row = index / cols
        val x = (col - (cols - 1) / 2f) * d
        val y = (row - (rows - 1) / 2f) * d
        return x to y
    }

    /** Rayon circulaire englobant la formation 2-colonnes, en mm. */
    fun footprintRadiusMm(miniCount: Int, baseSize: String): Float {
        val d = LegionTabletopGeometry.baseDiameterMm(baseSize)
        val n = miniCount.coerceAtLeast(1)
        if (n == 1) return d / 2f
        val w = columns(n) * d
        val h = rows(n) * d
        return hypot(w / 2f, h / 2f)
    }
}
