package com.legionforge.app.domain

import kotlin.math.cos
import kotlin.math.sin

/** Angle par cran : valeur PROVISOIRE, non confirmée par la RRG (décision produit). */
const val YAW_STEP_DEG = 22.5f
/** Longueur d'un segment de gabarit : valeur PROVISOIRE, non sourcée. */
const val SEGMENT_MM = 40f

data class ManeuverResult(val xMm: Float, val yMm: Float, val headingDeg: Float)

/**
 * Gabarit à plusieurs coudes. RRG 1519-1524 : un pliage PAR ARTICULATION (pas un cran global).
 * Convention : cap 0 = proue vers le haut, sens horaire positif, axe y écran vers le bas,
 * donc u(cap) = (sin cap, -cos cap).
 * b_i > 0 = tribord, b_i < 0 = bâbord. cap_i = cap0 + somme(b_1..b_i) * angleParCran.
 * Chaque segment i avance de L selon cap_i. RRG 786-787 : articulations au-delà de la vitesse ignorées.
 * Vitesse 0 : sur place, cap0 inchangé (RRG 1398).
 */
fun resolveManeuver2(
    x0: Float, y0: Float, cap0: Float, speed: Int,
    bends: List<Int>, anglePerClick: Float = YAW_STEP_DEG,
    segmentLengthMm: Float = SEGMENT_MM,
): ManeuverResult {
    if (speed <= 0) return ManeuverResult(x0, y0, cap0)
    var cap = cap0
    var x = x0
    var y = y0
    for (i in 0 until minOf(speed, bends.size)) {
        cap += bends[i] * anglePerClick
        val r = Math.toRadians(cap.toDouble())
        x += (segmentLengthMm * sin(r)).toFloat()
        y -= (segmentLengthMm * cos(r)).toFloat()
    }
    return ManeuverResult(x, y, cap)
}

/** Sommets de la polyligne du gabarit (départ + 1 point par segment), pour l'aperçu du plateau. */
fun maneuverPolyline(
    x0: Float, y0: Float, cap0: Float, speed: Int,
    bends: List<Int>, anglePerClick: Float = YAW_STEP_DEG,
    segmentLengthMm: Float = SEGMENT_MM,
): List<Pair<Float, Float>> {
    val pts = mutableListOf(x0 to y0)
    if (speed <= 0) return pts
    var cap = cap0
    var x = x0
    var y = y0
    for (i in 0 until minOf(speed, bends.size)) {
        cap += bends[i] * anglePerClick
        val r = Math.toRadians(cap.toDouble())
        x += (segmentLengthMm * sin(r)).toFloat()
        y -= (segmentLengthMm * cos(r)).toFloat()
        pts += x to y
    }
    return pts
}

/** Crans max d'une articulation, lus dans la Speed Chart (position 1..n -> vitesse -> crans). */
fun maxClicks(speedChart: List<Map<Int, Int>>, position: Int, speed: Int): Int =
    speedChart.getOrNull(position - 1)?.get(speed) ?: 0
