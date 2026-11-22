package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NON-RÉGRESSION bug "zone de déploiement qui change au zoom" :
 * la taille d'un territoire doit être convertie mm -> px, pas laissée en mm.
 */
class LegionMapLayoutZoomTest {

    private val layout = LegionMapLayouts.RECOVER_THE_RESEARCH

    @Test
    fun territoire_suit_le_zoom() {
        val a = layout.caseEdge(2f, 4f)
        val b = layout.caseEdge(8f, 6f)
        val s1 = layout.territorySizePx(a, b, 1f)
        val s2 = layout.territorySizePx(a, b, 2f)
        // Zoom x2 -> taille px x2 (avant le bug, taille identique car en mm brut).
        assertEquals(s1.first * 2f, s2.first, 0.001f)
        assertEquals(s1.second * 2f, s2.second, 0.001f)
        assertTrue(s1.first > 0f && s1.second > 0f)
    }

    @Test
    fun territoire_couvre_la_largeur_de_la_zone_transcrite() {
        // allied = intArrayOf(...) : bords en mm, largeur = (c2+1 - c0)*caseMm.
        val rect = layout.allied.first()
        val a = layout.caseEdge(rect[0].toFloat(), rect[1].toFloat())
        val b = layout.caseEdge(rect[2].toFloat() + 1f, rect[3].toFloat() + 1f)
        val size = layout.territorySizePx(a, b, 2f)
        assertEquals((rect[2] + 1 - rect[0]).toFloat() * layout.caseMm * 2f, size.first, 0.001f)
    }
}
