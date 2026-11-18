package com.legionforge.app.domain

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Géométrie du plateau tabletop Legion — calcul PUR, sans Compose, testable en JVM.
 *
 * CONVENTIONS D'ÉCHELLE (choix d'implémentation, PAS des règles du rulebook) :
 * - 1 pouce = 25,4 mm (conversion physique).
 * - 1 mm de table = [LEGION_PX_PER_MM] px à zoom 1.
 * - Taille des socles : petit = 25 mm, moyen = 40 mm, grand = 50 mm de DIAMÈTRE
 *   (valeurs communément admises ; le rulebook ne les chiffre pas, rulebook:298-303).
 * - Arc avant = 90° (±45° autour du facing) : APPROXIMATION. Le rulebook ne chiffre
 *   aucun angle (rulebook:2686-2689, socles crantés aux lignes sculptées).
 */
object LegionTabletopGeometry {
    const val MM_PER_INCH = 25.4f
    const val LEGION_PX_PER_MM = 2.0f

    /** Rayon du socle en mm selon la taille. small=12,5 / medium=20 / large=25. */
    fun baseRadiusMm(size: String): Float = when (size.lowercase()) {
        "small" -> 12.5f
        "medium" -> 20f
        "large" -> 25f
        else -> 12.5f
    }

    /** Diamètre du socle en mm. */
    fun baseDiameterMm(size: String): Float = baseRadiusMm(size) * 2f

    /**
     * Socle PTIT vs CRANTÉ — règle officielle (rulebook:298-303) :
     * « Troopers, Clone Troopers, Droid Troopers, and Wookiee Troopers are on
     * small bases. Everything else is on notched bases of various sizes. »
     * Mandalorian/Emplacement/Creature Troopers et Heavy Droid Troopers sont
     * DONC crantés (piège : le mot "trooper" ne suffit pas).
     */
    private val SMALL_BASE_SUBTYPES = setOf(
        "trooper", "clone trooper", "droid trooper", "wookiee trooper",
    )

    fun isNotchedBase(cardSubtype: String?): Boolean {
        val s = cardSubtype?.trim()?.lowercase() ?: return true
        return s !in SMALL_BASE_SUBTYPES
    }

    /** Applique une nouvelle valeur de blessures : bornage 0..max et defeated dérivé. */
    fun applyWounds(current: Int, max: Int, newWounds: Int): Pair<Int, Boolean> {
        val w = newWounds.coerceIn(0, max.coerceAtLeast(0))
        return w to (w <= 0)
    }

    /** Total de blessures = seuil par figurine × effectif (rulebook:473-476). */
    fun totalWounds(health: Int, miniCount: Int): Int =
        health.coerceAtLeast(1) * miniCount.coerceAtLeast(1)

    /**
     * Distance bord-à-bord entre deux socles, en mm (rulebook:682-692 : la plus courte
     * distance possible, mesurée depuis les socles, miniature ignorée).
     * Retourne 0 si les socles se chevauchent ou se touchent.
     */
    fun edgeDistanceMm(
        ax: Float, ay: Float, aRadiusMm: Float,
        bx: Float, by: Float, bRadiusMm: Float,
    ): Float {
        val center = hypot(bx - ax, by - ay)
        return (center - aRadiusMm - bRadiusMm).coerceAtLeast(0f)
    }

    /** Convertit une distance en mm en pouces (arrondi au 0,5" pour l'affichage). */
    fun mmToInches(mm: Float): Float = mm / MM_PER_INCH

    /** Arrondit une valeur au 0,5 le plus proche (affichage des règles). */
    fun roundHalf(value: Float): Float = kotlin.math.round(value * 2f) / 2f

    /** Format d'affichage d'une mesure en pouces : "1"", "1.5"", "6"". */
    fun formatInches(value: Float): String {
        val whole = value.toInt()
        return if (abs(value - whole) < 1e-4f) "${whole}\"" else "${value}\""
    }

    /**
     * Bande de portée 1..N. Le ruler Legion est fait de segments de 6 POUCES
     * (rulebook range ruler ; legion.takras.net/range) : R1 = 0-6", R2 = 6-12",
     * R3 = 12-18". PAS 1 pouce par bande. 0 si au contact.
     */
    fun rangeBand(mm: Float): Int {
        val inches = mmToInches(mm)
        if (inches <= 0f) return 0
        return kotlin.math.ceil(inches / 6.0).toInt().coerceAtLeast(1)
    }

    /**
     * Vrai si la cible est dans l'arc AVANT du tireur (approximation 90°).
     * convention : headingDeg = 0 → face vers le HAUT (y négatif), horaire positif.
     */
    fun inFrontArc(
        attackerX: Float, attackerY: Float, headingDeg: Float,
        targetX: Float, targetY: Float,
    ): Boolean {
        val dx = targetX - attackerX
        val dy = targetY - attackerY
        if (dx == 0f && dy == 0f) return true
        val rad = Math.toRadians(headingDeg.toDouble())
        // Vecteur avant : cap 0 = (0,-1), rotation horaire.
        val fx = sin(rad)
        val fy = -cos(rad)
        val len = sqrt((dx * dx + dy * dy).toDouble())
        val dot = (dx * fx + dy * fy) / len
        // ±45° → cos(45°) ≈ 0.7071
        return dot >= sqrt(2.0) / 2.0 - 1e-6
    }

    /** Angle (degrés, horaire depuis le haut) d'une cible relativement au tireur. */
    fun bearingDeg(fromX: Float, fromY: Float, toX: Float, toY: Float): Float {
        val dx = toX - fromX
        val dy = toY - fromY
        // atan2(dx, -dy) : 0 = haut, horaire positif.
        val deg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble()))
        return ((deg + 360) % 360).toFloat()
    }

    /** Différence angulaire minimale signée en [-180,180]. */
    fun angleDelta(a: Float, b: Float): Float {
        var d = (a - b) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }

    /**
     * Génère les rayons en px des anneaux de portée 1..maxInches, à un scale donné.
     * Utilisé par le Canvas pour dessiner les bandes de la réglette.
     */
    fun rangeRingPx(inches: Int, scale: Float): Float =
        inches * MM_PER_INCH * LEGION_PX_PER_MM * scale

    /** Rayon visuel d'un socle en px à un scale donné. */
    fun baseRadiusPx(size: String, scale: Float): Float =
        baseRadiusMm(size) * LEGION_PX_PER_MM * scale
}
