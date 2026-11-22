package com.legionforge.app.domain
import org.junit.Assert.assertEquals

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArmadaDeploymentTest {
    private val sud = ArmadaDeployment.Side.SOUTH
    private val nord = ArmadaDeployment.Side.NORTH

    @Test
    fun bande_distance_1_a_3_bord_long() {
        // SUD : bord bas y = +457,2 ; bande y ∈ [228,6 ; 381,0]
        assertTrue(ArmadaDeployment.inDeployBand(0f, 300f, sud))
        assertTrue(ArmadaDeployment.inDeployBand(0f, 228.6f, sud))   // distance 3 (limite)
        assertTrue(ArmadaDeployment.inDeployBand(0f, 381f, sud))    // distance 1 (limite)
        assertFalse(ArmadaDeployment.inDeployBand(0f, 200f, sud))   // trop loin du bord
        assertFalse(ArmadaDeployment.inDeployBand(0f, 420f, sud))   // collé au bord (< distance 1)
        assertFalse(ArmadaDeployment.inDeployBand(700f, 300f, sud)) // hors zone 4'
    }

    @Test
    fun nord_est_miroir_du_sud() {
        assertTrue(ArmadaDeployment.inDeployBand(0f, -300f, nord))
        assertFalse(ArmadaDeployment.inDeployBand(0f, 300f, nord))
    }

    @Test
    fun distance_mesuree_au_bord_du_socle_pas_au_centre() {
        // Gros vaisseau large (demi-longueur 64,5 mm). Un escadron à 80 mm du
        // CENTRE est à 15,5 mm du BORD -> trop près (distance 1 = 25,4 mm).
        val gros = Triple(0f, 300f, 64.5f)
        assertFalse(ArmadaDeployment.squadronValid(0f, 380f, sud, listOf(gros)))
        assertTrue(ArmadaDeployment.squadronValid(0f, 395f, sud, listOf(gros)))  // 30,5 mm du bord
        // Demi-dimensions officielles
        assertEquals(21.5f, ArmadaDeployment.baseHalfMm("small").first)
        assertEquals(129f, ArmadaDeployment.baseHalfMm("huge").second)
    }

    @Test
    fun escadron_a_distance_1_2_dun_vaisseau_ami() {
        val vaisseau = Triple(0f, 300f, 0f)
        assertTrue(ArmadaDeployment.squadronValid(0f, 330f, sud, listOf(vaisseau)))  // 30 mm
        assertTrue(ArmadaDeployment.squadronValid(0f, 350f, sud, listOf(vaisseau)))  // 50 mm (limite)
        assertFalse(ArmadaDeployment.squadronValid(0f, 370f, sud, listOf(vaisseau))) // 70 mm trop loin
        assertFalse(ArmadaDeployment.squadronValid(0f, 300f, sud, listOf(vaisseau))) // 0 mm trop près
    }

    @Test
    fun escadron_peut_etre_hors_bande_si_proche_dun_vaisseau() {
        // Vaisseau à y=381 (distance 1). Escadron à y=407 -> hors bande (distance 0,25")
        // mais à 26 mm du vaisseau -> placement légal RRG.
        val vaisseau = Triple(0f, 381f, 0f)
        assertFalse(ArmadaDeployment.inDeployBand(0f, 407f, sud))
        assertTrue(ArmadaDeployment.squadronValid(0f, 407f, sud, listOf(vaisseau)))
    }

    @Test
    fun clamp_ramene_dans_la_bande_en_gardant_x() {
        val (x, y) = ArmadaDeployment.clampToBand(100f, 500f, sud)
        assertTrue(kotlin.math.abs(x - 100f) < 0.01f)
        assertTrue(ArmadaDeployment.inDeployBand(x, y, sud))
        val (x2, y2) = ArmadaDeployment.clampToBand(-900f, 0f, sud)
        assertTrue(kotlin.math.abs(x2 - -609.6f) < 0.01f)
        assertTrue(ArmadaDeployment.inDeployBand(x2, y2, sud))
    }

    @Test
    fun cote_oppose_pour_ladversaire() {
        assertTrue(ArmadaDeployment.sideFor(false) == sud)
        assertTrue(ArmadaDeployment.sideFor(true) == nord)
    }
}
