package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verrouille la transcription des Map Cards (rulebook 391-401, 417-423, 1181-1185). */
class LegionMapLayoutTest {

    @Test
    fun grille_12x6_couvre_plateau_6x3() {
        val m = LegionMapLayouts.RECOVER_THE_RESEARCH
        assertEquals(12, m.gridCols)
        assertEquals(6, m.gridRows)
        assertEquals(152.4f, m.caseMm, 0.01f) // 6" = 152,4 mm
        // 12 cases × 152,4 = 1828,8 mm ; 6 × 152,4 = 914,4 mm.
        assertEquals(1828.8f, m.gridCols * m.caseMm, 0.01f)
        assertEquals(914.4f, m.gridRows * m.caseMm, 0.01f)
    }

    @Test
    fun coins_grille_centres_dans_plateau() {
        val m = LegionMapLayouts.RECOVER_THE_RESEARCH
        // caseCenter = CENTRE de case : case 0 à -838,2 ; case 11 à +838,2 ; case 12 (bord) à +914,4.
        val c0 = m.caseCenter(0f, 0f)
        val c11 = m.caseCenter(11f, 5f)
        val e0 = m.caseEdge(0f, 0f)
        val e12 = m.caseEdge(12f, 6f)
        assertEquals(-838.2f, c0.first, 0.1f)
        assertEquals(-381.0f, c0.second, 0.1f)
        assertEquals(838.2f, c11.first, 0.1f)
        assertEquals(-914.4f, e0.first, 0.1f)
        assertEquals(-457.2f, e0.second, 0.1f)
        assertEquals(914.4f, e12.first, 0.1f)
        assertEquals(457.2f, e12.second, 0.1f)
    }

    @Test
    fun recover_research_transcrit_avec_zones_et_poi() {
        val m = LegionMapLayouts.RECOVER_THE_RESEARCH
        assertTrue(m.transcribed)
        assertTrue(m.allied.isNotEmpty())
        assertTrue(m.enemy.isNotEmpty())
        assertEquals(6, m.poi.size) // 6 Labs (setup de la carte)
        // Territoires en diagonale : allié bas-droite, ennemi haut-gauche.
        assertTrue(m.allied[0][1] > m.enemy[0][1])
        assertTrue(m.allied[0][0] > m.enemy[0][0])
    }

    @Test
    fun orientation_miroir_selon_bord_blue() {
        val m = LegionMapLayouts.RECOVER_THE_RESEARCH.oriented(blueAtBottom = false)
        // Miroir vertical : l'allié (bas) passe en haut.
        assertTrue(m.allied[0][1] < 3)
        assertTrue(m.enemy[0][1] > 3)
        assertTrue(m.poi.isNotEmpty())
    }

    @Test
    fun poi_sont_aux_intersections_pas_aux_centres() {
        val m = LegionMapLayouts.RECOVER_THE_RESEARCH
        // POI (2,2) : intersection → caseEdge = ((2-6)*152.4, (2-3)*152.4) = (-609.6, -152.4).
        // caseCenter donnerait (-533.4, -76.2), soit +76,2 mm (3") sur chaque axe = BUG.
        val e = m.caseEdge(m.poi[0][0].toFloat(), m.poi[0][1].toFloat())
        assertEquals(-609.6f, e.first, 0.1f)
        assertEquals(-152.4f, e.second, 0.1f)
        val c = m.caseCenter(m.poi[0][0].toFloat(), m.poi[0][1].toFloat())
        assertEquals(76.2f, c.first - e.first, 0.1f) // écart exact = demi-case
        assertEquals(76.2f, c.second - e.second, 0.1f)
    }

    @Test
    fun miroir_poi_utilise_rows_moins_r_pas_rows_moins_1() {
        val base = LegionMapLayouts.RECOVER_THE_RESEARCH
        val flipped = base.oriented(blueAtBottom = false)
        val pt = base.poi[0]
        // Intersection r=2 → miroir rows-r = 6-2 = 4 (pas 6-1-2=3 pour une case).
        assertEquals(6 - pt[1], flipped.poi[0][1])
        // Position mm : y passe de -152,4 à +152,4 (symétrie par rapport au centre).
        val y0 = base.caseEdge(pt[0].toFloat(), pt[1].toFloat()).second
        val y1 = flipped.caseEdge(flipped.poi[0][0].toFloat(), flipped.poi[0][1].toFloat()).second
        assertEquals(-y0, y1, 0.1f)
    }

    @Test
    fun poi_objets_varies_par_carte() {
        // Chaque carte propose plusieurs objets distincts (variation demandée par Yann).
        for (name in LegionMapLayouts.NAMES) {
            val layout = LegionMapLayouts.forName(name)!!
            assertTrue("Pas d'objets décoratifs pour $name", layout.poiObjects.isNotEmpty())
            assertTrue("Moins de 2 objets distincts pour $name", layout.poiObjects.size >= 2)
            assertEquals("Doublons d'objets pour $name", layout.poiObjects.size, layout.poiObjects.toSet().size)
        }
        // Rotation si plus de POI que d'objets : index wrap sans crash.
        val m = LegionMapLayouts.RECOVER_THE_RESEARCH
        for (i in m.poi.indices) assertTrue(m.poiObjectAt(i) != null)
        assertEquals(m.poiObjectAt(0), m.poiObjectAt(m.poiObjects.size))
    }

    @Test
    fun resolution_par_nom_et_repli() {
        assertEquals(LegionMapLayouts.RECOVER_THE_RESEARCH,
            LegionMapLayouts.forName("Recover the Research"))
        assertFalse(LegionMapLayouts.forName("Carte Inconnue")?.transcribed ?: false)
        assertEquals(null, LegionMapLayouts.forName(null))
    }

    @Test
    fun tous_les_noms_de_cartes_sont_uniques_et_resolus() {
        assertEquals(10, LegionMapLayouts.NAMES.size)
        assertEquals(10, LegionMapLayouts.NAMES.toSet().size)
        for (name in LegionMapLayouts.NAMES) {
            val layout = LegionMapLayouts.forName(name)
            assertTrue("Map non resolue: $name", layout != null && layout.transcribed)
            assertTrue("Zones vides: $name", layout!!.allied.isNotEmpty() && layout.enemy.isNotEmpty())
            assertTrue("Aucun POI: $name", layout.poi.isNotEmpty())
        }
    }
}
