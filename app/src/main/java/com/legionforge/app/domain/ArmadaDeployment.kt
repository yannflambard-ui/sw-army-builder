package com.legionforge.app.domain

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Règles de placement du déploiement Armada (RRG 1.6.0, phase de mise en place).
 *
 * Une SEULE zone de déploiement par joueur : bande à distance 1 à 3 du bord
 * long de table, limitée à la zone de mise en place de 4' de large. Il n'existe
 * PAS de « zone 2 pour les escadrons » (confusion fréquente).
 *
 * Les escadrons se placent à distance 1 à 2 d'un vaisseau AMI et PEUVENT donc
 * être hors de la bande de déploiement. Les vaisseaux, eux, doivent être dans
 * la bande.
 *
 * géométrie commune avec TabletopScreen.kt (source de vérité partagée).
 */
object ArmadaDeployment {
    const val BOARD_W_MM = 1828.8f   // 6' — bord long = bord du joueur (horizontal)
    const val BOARD_H_MM = 914.4f    // 3' — bord court (vertical)
    const val SETUP_W_MM = 1219.2f   // 4' — zone de mise en place centrée
    const val DEPLOY_MIN_MM = 76.2f  // distance 1 (3")
    const val DEPLOY_MAX_MM = 228.6f // distance 3 (9")
    const val SQUAD_MIN_MM = 25.4f   // distance 1 (1")
    const val SQUAD_MAX_MM = 50.8f   // distance 2 (2")

    enum class Side { SOUTH, NORTH }

    /** Demi-dimensions du socle RRG 1.6.0 (largeur/2, longueur/2) en mm.
     *  La distance se mesure au BORD du socle, jamais à son centre. */
    fun baseHalfMm(sizeClass: String): Pair<Float, Float> = when (sizeClass.lowercase()) {
        "small" -> 21.5f to 35.5f    // 43 x 71
        "medium" -> 31.5f to 51f     // 63 x 102
        "large" -> 38.75f to 64.5f   // 77,5 x 129
        "huge" -> 38.75f to 129f     // 77,5 x 258
        else -> 0f to 0f             // flottille / escadron : jeton négligeable
    }

    /** Côté du joueur : le joueur local est SUD, l'adversaire Nord (symétrie miroir). */
    fun sideFor(isOpponent: Boolean): Side = if (isOpponent) Side.NORTH else Side.SOUTH

    /** Distance au bord long du côté [side] (positive dans la bande joueur). */
    fun distanceFromEdge(yMm: Float, side: Side, halfLengthMm: Float = 0f): Float =
        (when (side) {
            Side.SOUTH -> BOARD_H_MM / 2f - yMm
            Side.NORTH -> BOARD_H_MM / 2f + yMm
        }) - halfLengthMm

    /** Vrai si le point est dans la bande de déploiement (vaisseaux). */
    fun inDeployBand(
        xMm: Float, yMm: Float, side: Side,
        halfWidthMm: Float = 0f, halfLengthMm: Float = 0f,
    ): Boolean =
        abs(xMm) + halfWidthMm <= SETUP_W_MM / 2f &&
            distanceFromEdge(yMm, side, halfLengthMm) in DEPLOY_MIN_MM..DEPLOY_MAX_MM

    /**
     * Vrai si un escadron est correctement placé : à distance 1..2 d'au moins
     * un vaisseau ami. S'il n'y a aucun vaisseau ami, la bande sert de repli
     * (on ne peut pas placer un escadron sans son vaisseau de rattachement).
     */
    fun squadronValid(
        xMm: Float, yMm: Float,
        side: Side,
        friendlyShips: List<Triple<Float, Float, Float>>,
        halfMm: Float = 0f,
    ): Boolean {
        if (friendlyShips.isEmpty()) return inDeployBand(xMm, yMm, side)
        // Bord-à-bord : on retranche la demi-taille de l'escadron ET du vaisseau ami.
        return friendlyShips.any { (sx, sy, shipHalf) ->
            val gap = hypot(xMm - sx, yMm - sy) - halfMm - shipHalf
            gap in SQUAD_MIN_MM..SQUAD_MAX_MM
        }
    }

    /** Validation générique d'un placement au moment du drop. */
    fun placementValid(
        xMm: Float, yMm: Float,
        side: Side,
        isSquadron: Boolean,
        friendlyShips: List<Triple<Float, Float, Float>>,
    ): Boolean = if (isSquadron) {
        squadronValid(xMm, yMm, side, friendlyShips)
    } else {
        inDeployBand(xMm, yMm, side)
    }

    /**
     * Ramène un point hors bande dans la bande (plus proche point valide).
     * Utilisé au drop pour garantir "dans la zone uniquement" tout en gardant
     * la position voulue par le joueur au plus près.
     */
    fun clampToBand(
        xMm: Float, yMm: Float, side: Side,
        halfWidthMm: Float = 0f, halfLengthMm: Float = 0f,
    ): Pair<Float, Float> {
        val x = xMm.coerceIn(-SETUP_W_MM / 2f + halfWidthMm, SETUP_W_MM / 2f - halfWidthMm)
        val d = (distanceFromEdge(yMm, side, halfLengthMm)
            .coerceIn(DEPLOY_MIN_MM, DEPLOY_MAX_MM)) + halfLengthMm
        val y = when (side) {
            Side.SOUTH -> BOARD_H_MM / 2f - d
            Side.NORTH -> -BOARD_H_MM / 2f + d
        }
        return x to y
    }
}
