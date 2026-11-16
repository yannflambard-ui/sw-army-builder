package com.legionforge.app.domain

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Socles pleins en millimètres ; y croît vers le bas, cap 0 vertical, horaire positif. */
sealed class ArmadaBase {
    data class Ship(
        val xMm: Double, val yMm: Double,
        val widthMm: Double, val lengthMm: Double, val headingDeg: Double,
    ) : ArmadaBase() {
        init {
            require(xMm.isFinite() && yMm.isFinite() && headingDeg.isFinite())
            require(widthMm.isFinite() && widthMm > 0 && lengthMm.isFinite() && lengthMm > 0)
        }
    }

    data class Squadron(val xMm: Double, val yMm: Double, val radiusMm: Double) : ArmadaBase() {
        init {
            require(xMm.isFinite() && yMm.isFinite())
            require(radiusMm.isFinite() && radiusMm > 0)
        }
    }
}

/** Bornes approximatives de la face RANGE, et non distances officielles calibrées. */
const val ARMADA_BLACK_RANGE_MM = 123.19
const val ARMADA_BLUE_RANGE_MM = 186.69
const val ARMADA_RED_RANGE_MM = 304.8

data class ArmadaRangeDice(val black: Int = 0, val blue: Int = 0, val red: Int = 0) {
    init { require(black >= 0 && blue >= 0 && red >= 0) }
}

/**
 * Distance minimale entre les socles entiers, nulle en cas de contact ou chevauchement.
 * C'est une borne inférieure de la distance entre les zones de coque déclarées :
 * elle peut exclure un tir impossible, mais ne valide ni portée des secteurs, ni arc, ni visibilité.
 */
fun minimumBaseDistanceMm(a: ArmadaBase, b: ArmadaBase): Double = when {
    a is ArmadaBase.Ship && b is ArmadaBase.Ship -> shipDistance(a, b)
    a is ArmadaBase.Squadron && b is ArmadaBase.Squadron ->
        (hypot(a.xMm - b.xMm, a.yMm - b.yMm) - a.radiusMm - b.radiusMm).coerceAtLeast(0.0)
    a is ArmadaBase.Ship && b is ArmadaBase.Squadron -> shipSquadronDistance(a, b)
    a is ArmadaBase.Squadron && b is ArmadaBase.Ship -> shipSquadronDistance(b, a)
    else -> error("Unsupported base")
}

/**
 * Filtre conservateur des dés d'une attaque de vaisseau avec la face RANGE : seules
 * les couleurs certainement hors portée sont retirées. Un dé conservé n'est pas validé.
 * Ne pas employer ce filtre pour les attaques d'escadrons (face DISTANCE / Snipe).
 */
fun filterPossibleShipAttackDice(dice: ArmadaRangeDice, minimumBaseDistanceMm: Double): ArmadaRangeDice {
    require(minimumBaseDistanceMm.isFinite() && minimumBaseDistanceMm >= 0)
    return ArmadaRangeDice(
        black = if (minimumBaseDistanceMm <= ARMADA_BLACK_RANGE_MM) dice.black else 0,
        blue = if (minimumBaseDistanceMm <= ARMADA_BLUE_RANGE_MM) dice.blue else 0,
        red = if (minimumBaseDistanceMm <= ARMADA_RED_RANGE_MM) dice.red else 0,
    )
}

private data class Point(val x: Double, val y: Double) {
    operator fun plus(other: Point) = Point(x + other.x, y + other.y)
    operator fun minus(other: Point) = Point(x - other.x, y - other.y)
    operator fun times(scale: Double) = Point(x * scale, y * scale)
    fun dot(other: Point) = x * other.x + y * other.y
}

private fun axes(ship: ArmadaBase.Ship): Pair<Point, Point> {
    val radians = Math.toRadians(ship.headingDeg % 360.0)
    val c = cos(radians)
    val s = sin(radians)
    return Point(c, s) to Point(-s, c)
}

private fun corners(ship: ArmadaBase.Ship): List<Point> {
    val (x, y) = axes(ship)
    val center = Point(ship.xMm, ship.yMm)
    val halfWidth = x * (ship.widthMm / 2)
    val halfLength = y * (ship.lengthMm / 2)
    return listOf(center - halfWidth - halfLength, center + halfWidth - halfLength,
        center + halfWidth + halfLength, center - halfWidth + halfLength)
}

private fun shipSquadronDistance(ship: ArmadaBase.Ship, squadron: ArmadaBase.Squadron): Double {
    val (x, y) = axes(ship)
    val delta = Point(squadron.xMm - ship.xMm, squadron.yMm - ship.yMm)
    val dx = (abs(delta.dot(x)) - ship.widthMm / 2).coerceAtLeast(0.0)
    val dy = (abs(delta.dot(y)) - ship.lengthMm / 2).coerceAtLeast(0.0)
    return (hypot(dx, dy) - squadron.radiusMm).coerceAtLeast(0.0)
}

private fun shipDistance(a: ArmadaBase.Ship, b: ArmadaBase.Ship): Double {
    val aAxes = axes(a)
    val bAxes = axes(b)
    val delta = Point(b.xMm - a.xMm, b.yMm - a.yMm)
    val axes = listOf(aAxes.first, aAxes.second, bAxes.first, bAxes.second)
    val separated = axes.any { axis ->
        val radiusA = a.widthMm / 2 * abs(axis.dot(aAxes.first)) +
            a.lengthMm / 2 * abs(axis.dot(aAxes.second))
        val radiusB = b.widthMm / 2 * abs(axis.dot(bAxes.first)) +
            b.lengthMm / 2 * abs(axis.dot(bAxes.second))
        abs(delta.dot(axis)) > radiusA + radiusB
    }
    if (!separated) return 0.0
    val aCorners = corners(a)
    val bCorners = corners(b)
    return minOf(cornerToEdges(aCorners, bCorners), cornerToEdges(bCorners, aCorners))
}

private fun cornerToEdges(points: List<Point>, polygon: List<Point>): Double {
    var minimum = Double.POSITIVE_INFINITY
    for (point in points) for (i in polygon.indices) {
        val start = polygon[i]
        val edge = polygon[(i + 1) % polygon.size] - start
        val t = ((point - start).dot(edge) / edge.dot(edge)).coerceIn(0.0, 1.0)
        val difference = point - (start + edge * t)
        minimum = minOf(minimum, hypot(difference.x, difference.y))
    }
    return minimum
}
