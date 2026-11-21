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
fun DrawScope.drawBattleMatLogic(
    mat: BattleMat,
    mm: (Float, Float) -> Offset,
    pxPerMm: Float,
    textMeasurer: TextMeasurer,
) {
    if (mat == BattleMats.NONE || mat.pieces.isEmpty()) return
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

        when (piece.surface) {
            TerrainSurface.OPEN -> {
                drawRect(pin.open.copy(alpha = 0.6f), topLeft = rect.topLeft, size = rect.size,
                    style = Stroke(width = stroke, pathEffect = dot))
            }
            TerrainSurface.DIFFICULT -> {
                drawRect(Color(0x14FFD24A), topLeft = rect.topLeft, size = rect.size)
                drawRect(pin.difficult, topLeft = rect.topLeft, size = rect.size,
                    style = Stroke(width = stroke, pathEffect = dash))
            }
            TerrainSurface.IMPASSABLE -> {
                drawRect(Color(0x20FF3D6E), topLeft = rect.topLeft, size = rect.size)
                drawRect(pin.impassable, topLeft = rect.topLeft, size = rect.size,
                    style = Stroke(width = stroke))
                // Hachures 45° discrètes dans l'emprise.
                val step = maxOf(12f, 16.dp.toPx())
                var hx = left - h
                while (hx < left + w) {
                    val x1 = maxOf(left, hx)
                    val y1 = top + (x1 - hx)
                    val x2 = kotlin.math.min(left + w, hx + h)
                    val y2 = top + (x2 - hx)
                    if (x2 > x1 && y1 <= top + h && y2 <= top + h) {
                        drawLine(pin.impassable.copy(alpha = 0.38f), Offset(x1, y1), Offset(x2, y2), strokeWidth = 1f)
                    }
                    hx += step
                }
            }
        }

        // Halo sombre sous le trait principal (surface claire comme eau turquoise).
        drawRect(halo.copy(alpha = 0.25f), topLeft = Offset(left + 2f, top + 2f), size = rect.size,
            style = Stroke(width = 1f))

        // Couvert : anneau + CHIFFRE (daltonisme + fond chargé), épaisseur = niveau.
        if (piece.cover > 0) {
            val r = maxOf(w, h) / 2f + 3.dp.toPx()
            val col = if (piece.cover >= 2) Color(0xAAFF7A00) else Color(0xAA2E9BFF)
            // Epaisseur plafonnée à 2px : l'anneau SIGNALE, ne domine pas la photo.
            val cw = maxOf(1.2f, (piece.cover).dp.toPx()).coerceAtMost(2f)
            drawCircle(halo.copy(alpha = 0.55f), radius = r, center = c, style = Stroke(width = cw + 1f))
            drawCircle(col, radius = r, center = c, style = Stroke(width = cw))
            val style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xCCFFFFFF))
            drawText(textMeasurer, piece.cover.toString(),
                topLeft = Offset(c.x - 4.dp.toPx(), c.y - 6.dp.toPx()),
                size = Size(8.dp.toPx(), 12.dp.toPx()), style = style)
        }

        // blocksLos : ✕ en COIN supérieur gauche, jamais au centre.
        if (piece.blocksLos) {
            val m = 8.dp.toPx()
            drawLine(Color(0xCCE8EAF0), Offset(left + 2f, top + 2f), Offset(left + m, top + m), strokeWidth = 1.5f)
            drawLine(Color(0xCCE8EAF0), Offset(left + m, top + 2f), Offset(left + 2f, top + m), strokeWidth = 1.5f)
        }
    }
}

