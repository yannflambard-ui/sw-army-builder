package com.legionforge.app.domain

import com.legionforge.app.domain.LegionMissionBuilder.MissionState
import com.legionforge.app.domain.LegionMissionBuilder.ModAction
import com.legionforge.app.domain.LegionMissionBuilder.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Round 0 Build a Mission — chaque règle corrigée est couverte (rulebook:1200-1275). */
class LegionMissionBuilderTest {
    private val B = LegionMissionBuilder

    @Test
    fun `gate blue avant revelation`() {
        val s = MissionState()
        // Sans Blue, la révélation initiale ne fait rien.
        val after = B.initialReveal(s, "objective", { _, _, _ -> "Une carte" })
        assertTrue("revelation bloquee sans Blue", after.revealed.isEmpty())
        assertEquals(Step.BLUE, after.step)
    }

    @Test
    fun `revelation initiale ordonnee blue puis adversaire puis avantages`() {
        var s = B.withBlue(MissionState(), "own")
        assertEquals(Step.REVEAL, s.step)
        assertEquals("own", s.turn)
        s = B.initialReveal(s, "objective", { side, type, _ ->
            "${side}_${type}" // draw deterministe
        })
        assertEquals("Objective partage vient du deck BLUE", "own_objective", s.objective)
        assertEquals("Secondaire partage vient du deck ADVERSE", "opp_secondary", s.secondary)
        assertEquals("Avantage MOI", "own_advantage", s.advOwn)
        assertEquals("Avantage ADV", "opp_advantage", s.advOpp)
        assertEquals(Step.MODIFY, s.step)
        assertEquals("blue commence les modifications", "own", s.turn)
    }

    @Test
    fun `choix du type par blue inverse les decks`() {
        var s = B.withBlue(MissionState(), "opp")
        s = B.initialReveal(s, "secondary", { side, type, _ -> "${side}_${type}" })
        assertEquals("Blue a choisi Secondaire → vient de son deck", "opp_secondary", s.secondary)
        assertEquals("Adversaire → Objectif", "own_objective", s.objective)
    }

    @Test
    fun `alternance B R B R deux modifications chacun`() {
        var s = B.withBlue(MissionState(), "own")
        s = B.initialReveal(s, "objective", { side, type, _ -> "${side}_${type}" })
        val seq = mutableListOf<String>()
        repeat(4) {
            seq += s.turn
            s = B.modify(s, s.turn, ModAction.PASS, { _, _, _ -> "x" })
        }
        assertEquals(listOf("own", "opp", "own", "opp"), seq)
        assertEquals(Step.DONE, s.step)
        assertEquals(0, s.modsLeft.values.sum())
    }

    @Test
    fun `camp inactif ne peut pas modifier`() {
        var s = B.withBlue(MissionState(), "own")
        s = B.initialReveal(s, "objective", { side, type, _ -> "${side}_${type}" })
        val before = s
        s = B.modify(s, "opp", ModAction.OBJECTIVE, { _, _, _ -> "TRICHE" }) // pas son tour
        assertEquals("aucune modification du camp inactif", before, s)
    }

    @Test
    fun `modifier avantage adverse est legal`() {
        var s = B.withBlue(MissionState(), "own")
        s = B.initialReveal(s, "objective", { side, type, _ -> "${side}_${type}" })
        s = B.modify(s, "own", ModAction.OPP_ADVANTAGE, { _, _, _ -> "NOUVEL_AV" })
        assertEquals("avantage adverse remplace", "NOUVEL_AV", s.advOpp)
        assertEquals("avantage propre intact", "own_advantage", s.advOwn)
    }

    @Test
    fun `devenir blue deplace le jeton`() {
        var s = B.withBlue(MissionState(), "own")
        s = B.initialReveal(s, "objective", { side, type, _ -> "${side}_${type}" })
        s = B.modify(s, "opp", ModAction.BECOME_BLUE, { _, _, _ -> "x" })
        // modifier("opp") sans être au tour de opp est bloqué → on avance d'abord
        assertEquals("tour inactif bloque", "own", s.bluePlayer)
        s = B.modify(s, "own", ModAction.PASS, { _, _, _ -> "x" })
        s = B.modify(s, "opp", ModAction.BECOME_BLUE, { _, _, _ -> "x" })
        assertEquals("jeton Blue deplacé vers opp", "opp", s.bluePlayer)
    }

    @Test
    fun `map appariee a l objectif par titre`() {
        val maps = setOf("Recover the Research", "Breakthrough")
        var s = B.withBlue(MissionState(), "own")
        s = B.initialReveal(s, "objective", { side, type, _ ->
            if (type == "objective") "Breakthrough" else "${side}_${type}"
        })
        assertEquals("Breakthrough", s.map(maps))
        s = B.modify(s, "own", ModAction.OBJECTIVE, { _, _, _ -> "Inconnue" })
        assertEquals("map suit l'objectif remplace", null, s.map(maps))
    }

    @Test
    fun `reshuffle quand deck vide inclut la carte ecartée`() {
        // deck vide, défausse = [A, B, C] ; rng renvoie toujours 0 → tire A
        val (card, rest) = B.drawTop(emptyList(), listOf("A", "B", "C"), { 0 })
        assertEquals("A", card)
        assertEquals(listOf("B", "C"), rest)
        // la carte écartée est bien revenue dans la pile (via l'appelant qui l'ajoute à discarded)
        val discardedWithCurrent = listOf("A", "B", "C") + "old"
        val (c2, _) = B.drawTop(emptyList(), discardedWithCurrent, { discardedWithCurrent.size - 1 })
        assertEquals("old", c2) // la carte venant d'être écartée est re-mélangée
    }

    @Test
    fun `garde fou battle deck exactement 3 3 3`() {
        assertTrue(B.battleDeckValid(mapOf("objective" to 3, "secondary" to 3, "advantage" to 3)))
        assertFalse("4 objectifs interdit", B.battleDeckValid(mapOf("objective" to 4, "secondary" to 3, "advantage" to 3)))
        assertFalse("2 secondaires interdit", B.battleDeckValid(mapOf("objective" to 3, "secondary" to 2, "advantage" to 3)))
    }
}
