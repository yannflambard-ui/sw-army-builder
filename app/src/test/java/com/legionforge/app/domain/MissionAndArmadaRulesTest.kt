package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Règles Round 0 Legion + scoring/élimination Armada (passe experts 2→3). */
class MissionAndArmadaRulesTest {

    // ── Legion : départage Blue Player (4 dés noirs) ──
    @Test fun `blue player plus de hits gagne`() {
        val own = listOf("Hit", "Hit", "Vide", "Vide")
        val opp = listOf("Hit", "Vide", "Vide", "Vide")
        assertEquals(-1, LegionDiceRules.compareBlueRoll(own, opp))
        assertEquals(1, LegionDiceRules.compareBlueRoll(opp, own))
    }

    @Test fun `blue player crits puis surges departagent`() {
        // À nombre de Hits égal, les Crits puis les Surges départagent.
        assertEquals(-1, LegionDiceRules.compareBlueRoll(
            listOf("Hit", "Crit", "Vide", "Vide"),
            listOf("Hit", "Surge", "Vide", "Vide")))
        assertEquals(-1, LegionDiceRules.compareBlueRoll(
            listOf("Hit", "Surge", "Vide", "Vide"),
            listOf("Hit", "Vide", "Vide", "Vide")))
    }

    @Test fun `blue player egalite renvoie zero`() {
        val a = listOf("Hit", "Crit", "Surge", "Vide")
        assertEquals(0, LegionDiceRules.compareBlueRoll(a, a))
    }

    @Test fun `jet blue produit 4 des noirs`() {
        val roll = LegionDiceRules.rollBluePlayer()
        assertEquals(4, roll.size)
        val facesNoires = LegionDiceRules.faces("attack", "black")
        assert(roll.all { it in facesNoires })
    }

    // ── Armada : scoring avec objectifs ──
    @Test fun `score armada ajoute les points d objectif`() {
        val ship = ArmadaScoringUnit(basePoints = 50, upgradePoints = 0, destroyed = true)
        assertEquals(50L, armadaScore(listOf(ship), 0))
        assertEquals(58L, armadaScore(listOf(ship), 8))
    }

    // ── Armada : élimination (vaisseaux seulement, départage au score) ──
    @Test fun `elimination vaisseau adverse`() {
        assertEquals(ArmadaPlayer.FIRST, armadaEliminationWinner(1, 0, 10, 0))   // adverse éliminé
        assertEquals(ArmadaPlayer.SECOND, armadaEliminationWinner(0, 1, 0, 10))  // local éliminé
    }

    @Test fun `destruction simultanee departagee au score`() {
        assertEquals(ArmadaPlayer.FIRST, armadaEliminationWinner(0, 0, 20, 10))
        assertEquals(ArmadaPlayer.SECOND, armadaEliminationWinner(0, 0, 10, 20))
    }

    @Test fun `pas de fin si vaisseaux des deux cotes survivent`() {
        assertNull(armadaEliminationWinner(2, 3, 10, 10))
    }
}
