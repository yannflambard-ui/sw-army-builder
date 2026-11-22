package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Le doigt choisit parmi les atterrissages du gabarit de manoeuvre (vitesse +
 * crans de la Speed Chart). La destination n'est PAS libre.
 */
class ManeuverLandingsTest {

    private val chartStraight = listOf(mapOf(1 to 0, 2 to 0, 3 to 0))

    @Test
    fun vitesse_zero_une_seule_position_sur_place() {
        val l = maneuverLandings(100f, 200f, 0f, 0, chartStraight)
        assertEquals(1, l.size)
        assertEquals(100f, l[0].xMm, 0.001f)
        assertEquals(200f, l[0].yMm, 0.001f)
        assertEquals(0f, l[0].headingDeg, 0.001f)
    }

    @Test
    fun vitesse_2_sans_crans_une_seule_trajectoire_droite() {
        val l = maneuverLandings(0f, 0f, 0f, 2, chartStraight)
        assertEquals(1, l.size)
        // cap 0 = proue vers le haut = y négatif, 2 segments de 40 mm.
        assertEquals(0f, l[0].xMm, 0.001f)
        assertEquals(-80f, l[0].yMm, 0.001f)
    }

    @Test
    fun un_cran_par_articulation_donne_3_atterrissages_par_segment() {
        // Speed chart : 1 cran max à chaque articulation, vitesse 1 -> 3 gabarits.
        val chart = listOf(mapOf(1 to 1))
        val l = maneuverLandings(0f, 0f, 0f, 1, chart)
        assertEquals(3, l.size)
        // Gauche / droit / centre : cap -22,5 / 0 / +22,5.
        assertTrue(l.any { abs(it.headingDeg) < 0.001f })
        assertTrue(l.any { it.headingDeg > 20f })
        assertTrue(l.any { it.headingDeg < -20f })
    }

    @Test
    fun meme_position_cap_different_avec_un_cran() {
        // RRG p.11 : le segment reste droit ; le cran ne change que le cap final.
        val l = maneuverLandings(0f, 0f, 0f, 1, listOf(mapOf(1 to 1)))
        assertEquals(3, l.size)
        assertTrue(l.all { abs(it.xMm) < 0.001f && abs(it.yMm + 40f) < 0.001f })
        val caps = l.map { it.headingDeg }.sorted()
        assertEquals(3, caps.size)
        assertTrue(abs(caps[0] + 22.5f) < 0.001f)
        assertTrue(abs(caps[1]) < 0.001f)
        assertTrue(abs(caps[2] - 22.5f) < 0.001f)
    }

    @Test
    fun vitesse_2_avec_un_cran_par_articulation_donne_9_atterrissages() {
        val chart = listOf(mapOf(2 to 1), mapOf(2 to 1))
        assertEquals(9, maneuverLandings(0f, 0f, 0f, 2, chart).size)
    }

    @Test
    fun nearest_landing_choisit_le_point_le_plus_proche_du_doigt() {
        val l = listOf(
            com.legionforge.app.domain.ManeuverResult(0f, 0f, 0f),
            com.legionforge.app.domain.ManeuverResult(100f, 0f, 45f),
            com.legionforge.app.domain.ManeuverResult(0f, 100f, 90f),
        )
        val n = nearestLanding(l, 96f, 3f)!!
        assertEquals(100f, n.xMm, 0.001f)
        assertEquals(45f, n.headingDeg, 0.001f)
    }
}
