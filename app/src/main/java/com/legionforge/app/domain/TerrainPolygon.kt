package com.legionforge.app.domain

import com.legionforge.app.ui.screens.TerrainPiece

/**
 * Géométrie des emprises POLYGONALES des battle mats (rulebook 846-848).
 *
 * Une [TerrainPiece] porte optionnellement un [outline] = contour en PIXELS IMAGE
 * (source 1408x704). Ce domaine convertit ces pixels en mm plateau (mapping 6'x3')
 * et fournit un test disque/polygone PUR (sans Compose) pour la collision et le
 * rendu — une seule source de vérité pour la forme.
 *
 * Plateau 6'x3' = 1828,8 x 914,4 mm ; image 1408x704 couvre exactement :
 *   1 px x = 1828,8/1408 mm ; 1 px y = 914,4/704 mm.
 */
object TerrainPolygon {

    const val IMG_W = 1408f
    const val IMG_H = 704f
    const val BOARD_W_MM = 1828.8f
    const val BOARD_H_MM = 914.4f

    fun pxToMmX(px: Float): Float = (px / IMG_W) * BOARD_W_MM - BOARD_W_MM / 2f
    fun pxToMmY(px: Float): Float = (px / IMG_H) * BOARD_H_MM - BOARD_H_MM / 2f

    /** Contour d'une pièce en mm plateau (vide si AABB = pas de polygone). */
    fun outlineMm(piece: TerrainPiece): List<Pair<Float, Float>> =
        piece.outline?.map { (px, py) -> pxToMmX(px) to pxToMmY(py) } ?: emptyList()

    /** Bounding box d'un contour, en mm (centre + tailles) — pour l'anneau de couvert. */
    fun outlineBoundsMm(piece: TerrainPiece): QuadBounds? {
        val pts = outlineMm(piece)
        if (pts.isEmpty()) return null
        val xs = pts.map { it.first }; val ys = pts.map { it.second }
        val minX = xs.minOrNull() ?: return null
        val maxX = xs.maxOrNull() ?: return null
        val minY = ys.minOrNull() ?: return null
        val maxY = ys.maxOrNull() ?: return null
        return QuadBounds(cx = (minX + maxX) / 2f, cy = (minY + maxY) / 2f, w = maxX - minX, h = maxY - minY)
    }

    data class QuadBounds(val cx: Float, val cy: Float, val w: Float, val h: Float)

    /**
     * Rayon-casting standard : vrai si le point (px,py) est DANS le polygone.
     * Un point sur une arête est considéré DANS (bord inclus = contact compte).
     */
    fun pointInPolygon(x: Float, y: Float, poly: List<Pair<Float, Float>>): Boolean {
        if (poly.size < 3) return false
        var inside = false
        var i = 0
        var j = poly.size - 1
        while (i < poly.size) {
            val xi = poly[i].first; val yi = poly[i].second
            val xj = poly[j].first; val yj = poly[j].second
            // Bord inclus : distance point-segment < epsilon.
            if (pointOnSegment(x, y, xi, yi, xj, yj)) return true
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside
            }
            j = i++
        }
        return inside
    }

    private fun pointOnSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val eps = 1e-3f
        val cross = (px - x1) * (y2 - y1) - (py - y1) * (x2 - x1)
        if (kotlin.math.abs(cross) > eps) return false
        return px >= minOf(x1, x2) - eps && px <= maxOf(x1, x2) + eps &&
               py >= minOf(y1, y2) - eps && py <= maxOf(y1, y2) + eps
    }

    /**
     * Vrai si le disque (cx,cy,r) intersecte le polygone en mm.
     * Stratégie : (1) un sommet dans le disque, (2) le centre dans le polygone,
     * (3) une arête à distance <= r du centre. Couvre tous les cas de contact.
     */
    fun circleIntersectsPolygon(cx: Float, cy: Float, r: Float, poly: List<Pair<Float, Float>>): Boolean {
        if (poly.size < 3 || r <= 0f) return false
        val r2 = r * r
        for (pt in poly) {
            val dx = pt.first - cx; val dy = pt.second - cy
            if (dx * dx + dy * dy <= r2) return true
        }
        if (pointInPolygon(cx, cy, poly)) return true
        for (i in poly.indices) {
            val a = poly[i]; val b = poly[(i + 1) % poly.size]
            if (distPointSegmentSq(cx, cy, a.first, a.second, b.first, b.second) <= r2) return true
        }
        return false
    }

    private fun distPointSegmentSq(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1; val dy = y2 - y1
        val len2 = dx * dx + dy * dy
        if (len2 == 0f) { val ex = px - x1; val ey = py - y1; return ex * ex + ey * ey }
        var t = ((px - x1) * dx + (py - y1) * dy) / len2
        t = t.coerceIn(0f, 1f)
        val ex = px - (x1 + t * dx); val ey = py - (y1 + t * dy)
        return ex * ex + ey * ey
    }
}
