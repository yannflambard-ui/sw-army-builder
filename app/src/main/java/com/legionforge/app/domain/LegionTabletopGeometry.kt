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
 * - Taille des socles (DIAMÈTRE officiel, FFG CSR 2018 / AMG 06-2026) :
 *   small=27 mm, medium=50 mm, large=70 mm, huge=100 mm. Le rulebook:298-303 ne
 *   chiffre rien (small vs notched seulement) : ces valeurs viennent du CSR.
 * - Arc avant = 90° (±45° autour du facing) : APPROXIMATION. Le rulebook ne chiffre
 *   aucun angle (rulebook:2686-2689, socles crantés aux lignes sculptées).
 */
object LegionTabletopGeometry {
    const val MM_PER_INCH = 25.4f
    const val LEGION_PX_PER_MM = 2.0f

    /**
     * Diamètre officiel du socle en mm (FFG CSR 2018 / AMG 06-2026).
     * small=27 (infanterie), medium=50 (bikes), large=70 (AT-RT/emplacements),
     * huge=100 (AT-ST, T-47). Valeurs propriétaires : LAAT/le 120, AAT/TX-130 150,
     * A-A5 100x175. Le rulebook:298-303 ne chiffre pas (small vs notched).
     */
    fun baseDiameterMm(size: String): Float = when (size.lowercase()) {
        "small" -> 27f
        "medium" -> 50f
        "large" -> 70f
        "huge" -> 100f
        else -> 27f
    }

    /** Rayon du socle en mm. */
    fun baseRadiusMm(size: String): Float = baseDiameterMm(size) / 2f

    /**
     * Emprise d'une ESCQADE en mm (diamètre de l'ensemble des socles serrés).
     *
     * Une unité Legion n'est PAS un socle unique : [miniCount] figurines sont
     * positionnées côte à côte, touchantes (au contact, rulebook:766). Pour
     * 1 figurine l'emprise = 1 diamètre ; pour N, on aligne les socles en 2
     * rangées compactes. Le RAYON d'emprise sert à la collision et au rendu.
     *
     * Géométrie : N socles de diamètre D serrés en grille quasi-carrée
     * (colonnes = ceil(sqrt(N))) -> largeur = cols*D, hauteur = rows*D.
     * Le rayon circulaire englobant = hypot(largeur, hauteur)/2.
     */
    fun squadFootprintRadiusMm(size: String, miniCount: Int): Float {
        val d = baseDiameterMm(size)
        val n = miniCount.coerceAtLeast(1)
        if (n == 1) return d / 2f // 1 figurine = rayon d'un socle unique, pas sa diagonale
        val cols = kotlin.math.ceil(kotlin.math.sqrt(n.toDouble())).toInt().coerceAtLeast(1)
        val rows = kotlin.math.ceil(n.toDouble() / cols).toInt().coerceAtLeast(1)
        val w = cols * d
        val h = rows * d
        return hypot(w / 2f, h / 2f)
    }

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

    /**
     * Taille de socle ("small"/"medium"/"large"/"huge") depuis la CARTE.
     * Le rulebook:298-303 ne chiffre pas, mais le CSR publie : small 27 (les 4
     * sous-types infanterie), medium 50 (bikes/cavalerie/droïdes lourds),
     * large 70 (AT-RT, emplacements, véhicules légers), huge 100+ (AT-ST,
     * T-47, Occupier, AAT). Le catalogue n'a PAS de champ de taille : on
     * déduit du sous-type puis du nom pour les véhicules lourds.
     */
    fun baseSizeFor(cardSubtype: String?, name: String?, miniCount: Int): String {
        val sub = cardSubtype?.trim()?.lowercase() ?: ""
        if (sub in SMALL_BASE_SUBTYPES) return "small"
        val n = name?.lowercase() ?: ""
        // Gros véhicules / créatures montures : socle huge (100 mm et +).
        if (n.contains("at-st") || n.contains("t-47") || n.contains("aat") ||
            n.contains("occupier") || n.contains("tx-225") || n.contains("laat/le") ||
            n.contains("a-at") || n.contains("saber-class") || n.contains("dwarf spider") ||
            n.contains("crab droid") || n.contains("grievous")) return "huge"
        // Bikes, cavalerie, droïdes lourds, speeders légers : medium 50 mm.
        if (sub == "repulsor vehicle" || sub == "heavy droid trooper" ||
            n.contains("bike") || n.contains("speeder") && !n.contains("a-a5") ||
            n.contains("tauntaun") || n.contains("dewback") || n.contains("droideka")) return "medium"
        // AT-RT, emplacements, autres véhicules : large 70 mm.
        return "large"
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
