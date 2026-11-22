package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verrouille le rendu multi-socles des escouades (colonnes de 2, retrait des pertes). */
class LegionSquadLayoutTest {

    @Test
    fun formation_deux_de_large() {
        assertEquals(1, LegionSquadLayout.columns(1))
        assertEquals(2, LegionSquadLayout.columns(2))
        assertEquals(2, LegionSquadLayout.columns(6))
        assertEquals(1, LegionSquadLayout.rows(1))
        assertEquals(1, LegionSquadLayout.rows(2))
        assertEquals(2, LegionSquadLayout.rows(4))
        assertEquals(3, LegionSquadLayout.rows(6)) // 6 = 2 colonnes × 3 lignes
    }

    @Test
    fun six_socles_positions_en_grille_2x3() {
        val d = LegionTabletopGeometry.baseDiameterMm("small") // 27 mm
        // 6 figurines : 2 colonnes (x=-13.5 / +13.5), 3 lignes (y=-27/0/+27)
        assertEquals(-d / 2f, LegionSquadLayout.offsetMm(0, 6, "small").first, 0.01f)
        assertEquals(d / 2f, LegionSquadLayout.offsetMm(1, 6, "small").first, 0.01f)
        assertEquals(-d, LegionSquadLayout.offsetMm(0, 6, "small").second, 0.01f)
        assertEquals(0f, LegionSquadLayout.offsetMm(2, 6, "small").second, 0.01f)
        assertEquals(d, LegionSquadLayout.offsetMm(4, 6, "small").second, 0.01f)
    }

    @Test
    fun retrait_d_un_socle_par_figurine_morte() {
        // 6 figurines, 1 PV/figurine (health=1, maxWounds=6)
        assertEquals(6, LegionSquadLayout.aliveMinis(6, 6, 6, false))
        assertEquals(4, LegionSquadLayout.aliveMinis(6, 4, 6, false))
        assertEquals(1, LegionSquadLayout.aliveMinis(6, 1, 6, false))
        // 0 PV ou vaincu -> tous les socles retirés
        assertEquals(0, LegionSquadLayout.aliveMinis(6, 0, 6, false))
        assertEquals(0, LegionSquadLayout.aliveMinis(6, 2, 6, true))
    }

    @Test
    fun heros_garde_un_socle_tant_qu_il_a_des_pv() {
        // Héros : 1 figurine, 6 PV
        assertEquals(1, LegionSquadLayout.aliveMinis(1, 6, 6, false))
        assertEquals(1, LegionSquadLayout.aliveMinis(1, 1, 6, false))
        assertEquals(0, LegionSquadLayout.aliveMinis(1, 0, 6, false))
    }

    @Test
    fun pv_par_figurine_superieur_a_un() {
        // 3 figurines, 2 PV chacune (health=2, maxWounds=6)
        assertEquals(3, LegionSquadLayout.aliveMinis(3, 6, 6, false))
        assertEquals(2, LegionSquadLayout.aliveMinis(3, 4, 6, false))
        assertEquals(2, LegionSquadLayout.aliveMinis(3, 3, 6, false))
        assertEquals(1, LegionSquadLayout.aliveMinis(3, 2, 6, false))
    }

    @Test
    fun emprise_courante_se_reduit_avec_les_pertes() {
        // 6 figurines -> emprise 2x3 ; 2 survivants -> emprise 2x1 (plus petite).
        val r6 = LegionSquadLayout.footprintRadiusMm(6, "small")
        val r2 = LegionSquadLayout.footprintRadiusMm(2, "small")
        assertTrue(r2 < r6)
        // Les 2 survivants sont recentrés : y = 0 (pas le -d de la grille 2x3).
        assertEquals(0f, LegionSquadLayout.offsetMm(0, 2, "small").second, 0.01f)
        assertEquals(0f, LegionSquadLayout.offsetMm(1, 2, "small").second, 0.01f)
    }

    @Test
    fun emprise_englobe_tous_les_socles() {
        val r6 = LegionSquadLayout.footprintRadiusMm(6, "small")
        val d = LegionTabletopGeometry.baseDiameterMm("small")
        // 2 colonnes × 3 lignes -> hypot(d, 1.5d)
        assertEquals(kotlin.math.hypot(d, 1.5f * d), r6, 0.01f)
        // 1 figurine = rayon d'un socle, pas sa diagonale
        assertEquals(d / 2f, LegionSquadLayout.footprintRadiusMm(1, "small"), 0.01f)
        // Tout centre de socle + rayon socle reste dans l'emprise
        for (i in 0 until 6) {
            val (x, y) = LegionSquadLayout.offsetMm(i, 6, "small")
            assertTrue(kotlin.math.hypot(x, y) + d / 2f <= r6 + 0.01f)
        }
    }
}
