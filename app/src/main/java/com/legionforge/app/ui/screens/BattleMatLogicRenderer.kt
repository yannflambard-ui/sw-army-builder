package com.legionforge.app.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

/**
 * Couche de LOGIQUE DE JEU sur une vraie image de battle mat.
 *
 * Dessine UNIQUEMENT ce que le moteur comprend — zones de déplacement
 * (OPEN/DIFFICULT/IMPASSABLE), couvert 0/1/2, blocksLos — avec des couleurs
 * FROIDES qui ne sont jamais dans les images (interdit : ocre, rouge brique,
 * vert palmier). Aucune zone en aplat opaque : la photo reste visible.
 *
 * Palette (expert dessin, 9/10) :
 * - OPEN        = pointillé vert menthe, contour seul
 * - DIFFICULT   = tirets jaunes + remplissage très léger
 * - IMPASSABLE  = trait magenta + hachures 45°, remplissage léger
 * - couvert 1   = anneau bleu #2E9BFF + chiffre 1
 * - couvert 2   = anneau orange #FF7A00 + chiffre 2
 * - blocksLos   = ✕ en COIN (jamais au centre = unité détruite)
 *
 * Zéro asset externe, tout en primitives Canvas Compose.
 */
/** Construit le Path écran d'un contour mm, ou null si AABB (pas de polygone). */
private fun outlinePath(piece: TerrainPiece, mm: (Float, Float) -> Offset): Path? {
    val poly = com.legionforge.app.domain.TerrainPolygon.outlineMm(piece)
    if (poly.size < 3) return null
    val path = Path()
    val first = mm(poly[0].first, poly[0].second)
    path.moveTo(first.x, first.y)
    for (i in 1 until poly.size) {
        val p = mm(poly[i].first, poly[i].second)
        path.lineTo(p.x, p.y)
    }
    path.close()
    return path
}

fun DrawScope.drawBattleMatLogic(
    mat: BattleMat,
    mm: (Float, Float) -> Offset,
    pxPerMm: Float,
    textMeasurer: TextMeasurer,
) {
    if (mat == BattleMats.NONE || mat.pieces.isEmpty()) return
    // LOD : sous ~25 % de zoom (pxPerMm < 0,5), les 159 contours fins écrasent la
    // hiérarchie visuelle. On ne garde que les emprises IMPASSABLE (contrainte de
    // collision réelle) en trait simple — couvert/chiffres/hachures réapparaissent
    // dès qu'on zoome assez pour les lire (rulebook lisibilité).
    val coarse = pxPerMm < 0.5f
    val pin = BattleMatPalette()
    val halo = Color(0x99000000)
    val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
    val dot = PathEffect.dashPathEffect(floatArrayOf(3f, 7f), 0f)
    val stroke = maxOf(2f, 2.dp.toPx())

    for (piece in mat.pieces) {
        val c = mm(piece.xMm, piece.yMm)
        val w = piece.wMm * pxPerMm
        val h = piece.hMm * pxPerMm
        val left = c.x - w / 2f
        val top = c.y - h / 2f
        val rect = androidx.compose.ui.geometry.Rect(left, top, left + w, top + h)
        val opath = outlinePath(piece, mm) // non-null = vrai polygone, sinon AABB

        if (coarse) {
            if (piece.surface == TerrainSurface.IMPASSABLE) {
                if (opath != null) {
                    drawPath(opath, Color(0x18FF3D6E))
                    drawPath(opath, pin.impassable.copy(alpha = 0.7f), style = Stroke(width = maxOf(1.5f, stroke)))
                } else {
                    drawRect(Color(0x18FF3D6E), topLeft = rect.topLeft, size = rect.size)
                    drawRect(pin.impassable.copy(alpha = 0.7f), topLeft = rect.topLeft, size = rect.size,
                        style = Stroke(width = maxOf(1.5f, stroke)))
                }
            }
            continue
        }

        when (piece.surface) {
            TerrainSurface.OPEN -> {
                if (opath != null) drawPath(opath, pin.open.copy(alpha = 0.6f), style = Stroke(width = stroke, pathEffect = dot))
                else drawRect(pin.open.copy(alpha = 0.6f), topLeft = rect.topLeft, size = rect.size,
                    style = Stroke(width = stroke, pathEffect = dot))
            }
            TerrainSurface.DIFFICULT -> {
                if (opath != null) {
                    drawPath(opath, Color(0x14FFD24A))
                    drawPath(opath, pin.difficult, style = Stroke(width = stroke, pathEffect = dash))
                } else {
                    drawRect(Color(0x14FFD24A), topLeft = rect.topLeft, size = rect.size)
                    drawRect(pin.difficult, topLeft = rect.topLeft, size = rect.size,
                        style = Stroke(width = stroke, pathEffect = dash))
                }
            }
            TerrainSurface.IMPASSABLE -> {
                if (opath != null) {
                    drawPath(opath, Color(0x20FF3D6E))
                    drawPath(opath, pin.impassable, style = Stroke(width = stroke))
                } else {
                    drawRect(Color(0x20FF3D6E), topLeft = rect.topLeft, size = rect.size)
                    drawRect(pin.impassable, topLeft = rect.topLeft, size = rect.size,
                        style = Stroke(width = stroke))
                }
                // Hachures 45° discrètes dans l'emprise.
                val step = maxOf(18f, 24.dp.toPx())
                var hx = left - h
                while (hx < left + w) {
                    val x1 = maxOf(left, hx)
                    val y1 = top + (x1 - hx)
                    val x2 = kotlin.math.min(left + w, hx + h)
                    val y2 = top + (x2 - hx)
                    if (x2 > x1 && y1 <= top + h && y2 <= top + h) {
                        drawLine(pin.impassable.copy(alpha = 0.26f), Offset(x1, y1), Offset(x2, y2), strokeWidth = 1f)
                    }
                    hx += step
                }
            }
        }

        // Halo sombre sous le trait principal (surface claire comme eau turquoise).
        if (opath != null) {
            drawPath(opath, halo.copy(alpha = 0.25f), style = Stroke(width = 1f))
        } else {
            drawRect(halo.copy(alpha = 0.25f), topLeft = Offset(left + 2f, top + 2f), size = rect.size,
                style = Stroke(width = 1f))
        }
        // Anneau de couvert : centré sur le vrai contour si polygone (sinon AABB).
        val coverCenter = com.legionforge.app.domain.TerrainPolygon.outlineBoundsMm(piece)?.let { b -> mm(b.cx, b.cy) } ?: c
        val coverW = com.legionforge.app.domain.TerrainPolygon.outlineBoundsMm(piece)?.w ?: w
        val coverH = com.legionforge.app.domain.TerrainPolygon.outlineBoundsMm(piece)?.h ?: h

        // Couvert : anneau + CHIFFRE (daltonisme + fond chargé), épaisseur = niveau.
        if (piece.cover > 0) {
            val r = maxOf(coverW, coverH) / 2f + 3.dp.toPx()
            val col = if (piece.cover >= 2) Color(0xAAFF7A00) else Color(0xAA2E9BFF)
            // Epaisseur plafonnée à 2px : l'anneau SIGNALE, ne domine pas la photo.
            val cw = maxOf(1.2f, (piece.cover).dp.toPx()).coerceAtMost(2f)
            drawCircle(halo.copy(alpha = 0.55f), radius = r, center = coverCenter, style = Stroke(width = cw + 1f))
            drawCircle(col, radius = r, center = coverCenter, style = Stroke(width = cw))
            // Chiffre >= 11sp avec halo sombre (plancher lisibilité, daltonisme).
            val glyph = piece.cover.toString()
            val style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFFFFF))
            val haloStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xEE000000))
            val tw = 14.dp.toPx(); val th = 18.dp.toPx()
            val tl = Offset(coverCenter.x - tw / 2f, coverCenter.y - th / 2f)
            // Halo 4 directions pour décoller le chiffre du sable et des hachures.
            for (d in listOf(Offset(-1f, 0f), Offset(1f, 0f), Offset(0f, -1f), Offset(0f, 1f))) {
                drawText(textMeasurer, glyph, topLeft = tl + Offset(d.x, d.y), size = Size(tw, th), style = haloStyle)
            }
            drawText(textMeasurer, glyph, topLeft = tl, size = Size(tw, th), style = style)
        }

        // blocksLos : ✕ en COIN supérieur gauche, jamais au centre.
        if (piece.blocksLos) {
            val m = 8.dp.toPx()
            drawLine(Color(0xCCE8EAF0), Offset(left + 2f, top + 2f), Offset(left + m, top + m), strokeWidth = 1.5f)
            drawLine(Color(0xCCE8EAF0), Offset(left + m, top + 2f), Offset(left + 2f, top + m), strokeWidth = 1.5f)
        }
    }
}

