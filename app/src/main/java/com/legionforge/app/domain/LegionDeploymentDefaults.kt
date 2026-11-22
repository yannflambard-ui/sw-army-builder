package com.legionforge.app.domain

/**
 * Placement initial des unités Legion DANS leur zone de déploiement (Map Card).
 *
 * Règle (rulebook AMG DOC56) : les Player Territories sont définies par la Map
 * Card (391-395). Les unités commencent dans leur propre territoire. Ce module
 * lit les rectangles transcrits dans [LegionMapLayout] et y répartit les unités
 * en grille, sans inventer de règle ni déborder hors zone.
 *
 * Les zones `allied`/`enemy` sont en CASES [c0,r0,c1,r1] inclus ; on convertit
 * en mm avec caseCenter(). Si la carte n'est pas transcrite (pas de zone), on
 * retombe sur un placement groupé au centre du camp, comme avant.
 */
object LegionDeploymentDefaults {

    data class Spec(val id: String, val isOpponent: Boolean, val miniCount: Int = 1)

    data class Placement(val xMm: Float, val yMm: Float)

    /** Centre mm d'un rectangle de territoire en cases (indices inclusifs). */
    private fun rectCenter(layout: LegionMapLayout, rect: IntArray): Pair<Float, Float> {
        val cc = (rect[0] + rect[2]) / 2f
        val rr = (rect[1] + rect[3]) / 2f
        return layout.caseCenter(cc, rr)
    }

    /**
     * Répartit [specs] dans la zone du camp. [layout] doit déjà être ORIENTÉ
     * (via LegionMapLayout.oriented(blueAtBottom)) : allied = camp joueur,
     * enemy = camp adverse.
     */
    fun placements(specs: List<Spec>, layout: LegionMapLayout?): Map<String, Placement> {
        val out = mutableMapOf<String, Placement>()
        if (specs.isEmpty()) return out
        listOf(false to (layout?.allied ?: emptyList()), true to (layout?.enemy ?: emptyList()))
            .forEach { (isOpp, rects) ->
                val mine = specs.filter { it.isOpponent == isOpp }
                if (mine.isEmpty()) return@forEach
                val zone = rects.firstOrNull()
                val center = if (zone != null && layout != null) rectCenter(layout, zone)
                            else 0f to (if (isOpp) -120f else 120f)
                // Grille 4 de large, espacement 40 mm, centrée sur la zone.
                val cols = 4
                mine.forEachIndexed { i, s ->
                    val col = i % cols
                    val row = i / cols
                    val x = center.first + (col - (cols - 1) / 2f) * 40f
                    val y = center.second + (row - (mine.size / cols) / 2f) * 40f
                    out[s.id] = Placement(x, y)
                }
            }
        return out
    }
}
