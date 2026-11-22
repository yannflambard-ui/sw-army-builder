package com.legionforge.app.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Placement initial des flottes au déploiement (Armada 1.6.0).
 *
 * but : (1) aucune escadron ne se superpose à une autre ; (2) les escadrons
 * forment un V / une pointe / une ligne, avec un léger bruit DÉTERMINISTE
 * (même seed => même disposition, rejouable) ; (3) les vaisseaux sont posés
 * dans la bande RRG (distance 1-3 du bord long, zone 4' de large).
 *
 * MODÈLE INDICATIF : la formation des escadrons est un choix visuel, pas une
 * règle RRG (le joueur place ses escadrons librement à distance 1-2 d'un
 * vaisseau ami). L'espacement garantit seulement que les jetons ne se
 * recouvrent pas à l'écran.
 */
object ArmadaDeploymentDefaults {
    /** Espacement minimum entre deux jetons d'escadron (mm). */
    const val SQUAD_SPACING_MM = 90f
    /** Bruit positif max (mm) appliqué à chaque escadron. */
    const val JITTER_MM = 10f

    enum class Formation { V, POINTE, LIGNE }

    data class Spec(
        val id: String,
        val isSquadron: Boolean,
        val isOpponent: Boolean,
        val sizeClass: String = "small",
    )

    data class Placement(val xMm: Float, val yMm: Float)

    /** Choix de formation déterministe, stable pour un même camp. */
    fun formationFor(camp: String, count: Int): Formation {
        if (count <= 2) return Formation.LIGNE
        val h = (camp + ":$count").hashCode()
        return Formation.entries[abs(h) % Formation.entries.size]
    }

    /** Bruit déterministe borné [-JITTER, +JITTER]. */
    private fun jitter(key: String): Float {
        val h = abs(key.hashCode() % 2001) - 1000 // -1000..1000
        return h / 1000f * JITTER_MM
    }

    /** Points d'une formation, centrés sur (0,0), avant rotation/orientation du camp. */
    fun formationOffsets(count: Int, formation: Formation): List<Placement> = when (formation) {
        Formation.LIGNE -> (0 until count).map { i ->
            val x = (i - (count - 1) / 2f) * SQUAD_SPACING_MM
            Placement(x, 0f)
        }
        Formation.POINTE -> buildList {
            var placed = 0
            var row = 0
            while (placed < count) {
                val n = minOf(row + 1, count - placed)
                for (j in 0 until n) {
                    val x = (j - (n - 1) / 2f) * SQUAD_SPACING_MM
                    add(Placement(x, row * SQUAD_SPACING_MM * 0.86f))
                }
                placed += n
                row++
            }
        }
        Formation.V -> buildList {
            var placed = 0
            var row = 0
            while (placed < count) {
                val perSide = if (row == 0) 1 else 2
                for (j in 0 until minOf(perSide, count - placed)) {
                    // chevron : pointe vers l'avant (y négatif), ailes en arrière
                    val x = if (j == 0 && row > 0) -SQUAD_SPACING_MM * row
                            else if (row == 0) 0f else SQUAD_SPACING_MM * row
                    val y = if (row == 0) -SQUAD_SPACING_MM else row * SQUAD_SPACING_MM * 0.5f
                    add(Placement(x, y))
                }
                placed += minOf(perSide, count - placed)
                row++
            }
        }
    }

    /**
     * Positions initiales de TOUS les pions d'un camp, posées dans la bande.
     * Les vaisseaux occupent la bande ; les escadrons se forment autour du
     * premier vaisseau ami (à distance 1-2), repliées dans la bande si aucun
     * vaisseau n'existe (RRG : une escadron sans vaisseau de rattachement ne
     * peut pas être placée légalement — on la met dans la bande par défaut).
     */
    fun placements(specs: List<Spec>, isOpponent: Boolean): Map<String, Placement> {
        val mine = specs.filter { it.isOpponent == isOpponent }
        val ships = mine.filter { !it.isSquadron }
        val squads = mine.filter { it.isSquadron }
        val side = ArmadaDeployment.sideFor(isOpponent)
        val out = mutableMapOf<String, Placement>()

        // Vaisseaux : x étalé dans la zone 4', y au centre de la bande (distance 2).
        val halfSetup = ArmadaDeployment.SETUP_W_MM / 2f
        val bandMid = (ArmadaDeployment.DEPLOY_MIN_MM + ArmadaDeployment.DEPLOY_MAX_MM) / 2f
        ships.forEachIndexed { i, s ->
            val frac = if (ships.size == 1) 0.5f else i / (ships.size - 1).toFloat()
            val x = (-halfSetup + 120f) + frac * (2f * (halfSetup - 120f)) + jitter(s.id + "x")
            val yFromEdge = bandMid + jitter(s.id + "y")
            val y = when (side) {
                ArmadaDeployment.Side.SOUTH -> ArmadaDeployment.BOARD_H_MM / 2f - yFromEdge
                ArmadaDeployment.Side.NORTH -> -ArmadaDeployment.BOARD_H_MM / 2f + yFromEdge
            }
            out[s.id] = Placement(x, y)
        }

        // Escadrons : formation autour du 1er vaisseau, sinon centre de la bande.
        if (squads.isNotEmpty()) {
            val formation = formationFor(if (isOpponent) "opponent" else "player", squads.size)
            val anchor = ships.firstOrNull()?.let { out[it.id] }
                ?: Placement(0f, when (side) {
                    ArmadaDeployment.Side.SOUTH -> ArmadaDeployment.BOARD_H_MM / 2f - bandMid
                    ArmadaDeployment.Side.NORTH -> -ArmadaDeployment.BOARD_H_MM / 2f + bandMid
                })
            // La formation doit être ENTIÈREMENT devant le vaisseau (jamais derrière
            // lui, jamais à cheval sur sa coque). On décale tous les y pour qu'ils
            // soient >= un espacement, puis on les projette vers l'intérieur :
            // joueur SUD = y décroissant, adversaire NORD = y croissant.
            val offsets = formationOffsets(squads.size, formation)
            val minY = offsets.minOf { it.yMm }
            val dir = if (isOpponent) 1f else -1f
            offsets.forEachIndexed { i, off ->
                val s = squads[i]
                val ahead = off.yMm - minY + SQUAD_SPACING_MM
                out[s.id] = Placement(
                    anchor.xMm + off.xMm + jitter(s.id + "x"),
                    anchor.yMm + ahead * dir + jitter(s.id + "y"),
                )
            }
        }
        return out
    }
}
