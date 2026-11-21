package com.legionforge.app.domain

import com.legionforge.app.ui.screens.TerrainPiece
import com.legionforge.app.ui.screens.TerrainSurface
import kotlin.math.max
import kotlin.math.min

/**
 * Collision socle / terrain — SW Legion V2 (rulebook:846-848, 840-844).
 *
 * IMPASSABLE « completely prevents movement — cannot Move through or end a movement
 * on impassable terrain » : le dépôt d'un pion dont le socle recouvre une emprise
 * IMPASSABLE est INTERDIT. On refuse le drop et on revient à la dernière position
 * légale — jamais de snap auto au bord (ça inventrait une position non choisie et
 * modifierait portée / LOS / couvert).
 *
 * DIFFICULT (840-844) est traversable (vitesse -1, min 1) : JAMAIS bloqué ici.
 * OPEN est praticable : jamais bloqué.
 *
 * PRIORITÉ D'IMBRICATION : les emprises peuvent se superposer (ex. mur OPEN posé
 * sur une crête IMPASSABLE). La règle est `any` : dès qu'UNE pièce IMPASSABLE
 * recouvre le socle, le drop est refusé. C'est le choix sûr — un couvert OPEN ne
 * peut jamais « réhabiliter » un terrain interdit (rulebook 846-848). Les pièces
 * analysées sont dédupées par l'emprise au sol, donc pas de faux positifs.
 *
 * Le test est pur (aucun Canvas / Compose) pour être vérifié en JVM.
 */
object LegionTerrainCollision {

    /**
     * Vrai si le disque du socle (centre xMm/yMm, rayon baseRadiusMm) intersecte
     * au moins une emprise IMPASSABLE de la carte de terrain.
     * Le contact compte comme collision (on ne chevauche même pas un mur).
     */
    fun isDropBlocked(
        xMm: Float,
        yMm: Float,
        baseRadiusMm: Float,
        pieces: List<TerrainPiece>,
    ): Boolean {
        if (pieces.isEmpty() || baseRadiusMm <= 0f) return false
        return pieces.any { p ->
            if (p.surface != TerrainSurface.IMPASSABLE) return@any false
            circleIntersectsRect(
                cx = xMm, cy = yMm, r = baseRadiusMm,
                left = p.xMm - p.wMm / 2f, right = p.xMm + p.wMm / 2f,
                top = p.yMm - p.hMm / 2f, bottom = p.yMm + p.hMm / 2f,
            )
        }
    }

    /** Intersection disque / rectangle AxB aligné, bordures incluses. */
    fun circleIntersectsRect(
        cx: Float, cy: Float, r: Float,
        left: Float, right: Float, top: Float, bottom: Float,
    ): Boolean {
        if (left > right || top > bottom) return false
        val nx = max(left, min(cx, right))
        val ny = max(top, min(cy, bottom))
        val dx = cx - nx
        val dy = cy - ny
        return dx * dx + dy * dy <= r * r
    }
}
