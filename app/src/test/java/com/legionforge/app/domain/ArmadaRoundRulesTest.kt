package com.legionforge.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArmadaRoundRulesTest {
    private fun next(round: Int, phase: ArmadaRoundPhase, ships: Int = 0, squadrons: Int = 0) =
        advanceArmadaPhase(ArmadaRoundState(round, phase), ships, squadrons)

    @Test fun phasesFollowOfficialOrderAndStatusIncrementsRound() {
        assertEquals(ArmadaPhaseAdvance.Advanced(ArmadaRoundState(2, ArmadaRoundPhase.SHIP)), next(2, ArmadaRoundPhase.COMMAND, 3, 4))
        assertEquals(ArmadaPhaseAdvance.Advanced(ArmadaRoundState(2, ArmadaRoundPhase.SQUADRON)), next(2, ArmadaRoundPhase.SHIP, 0, 4))
        assertEquals(ArmadaPhaseAdvance.Advanced(ArmadaRoundState(2, ArmadaRoundPhase.STATUS)), next(2, ArmadaRoundPhase.SQUADRON, 3, 0))
        assertEquals(ArmadaPhaseAdvance.Advanced(ArmadaRoundState(3, ArmadaRoundPhase.COMMAND)), next(2, ArmadaRoundPhase.STATUS))
    }

    @Test fun shipAndSquadronPhasesBlockOnlyTheirOwnPendingActivations() {
        assertEquals(ArmadaPhaseAdvance.Blocked(2), next(1, ArmadaRoundPhase.SHIP, ships = 2))
        assertEquals(ArmadaPhaseAdvance.Blocked(3), next(1, ArmadaRoundPhase.SQUADRON, squadrons = 3))
        // Command and status do not require activations.
        assertEquals(ArmadaPhaseAdvance.Advanced(ArmadaRoundState(1, ArmadaRoundPhase.SHIP)), next(1, ArmadaRoundPhase.COMMAND, 2, 3))
        assertEquals(ArmadaPhaseAdvance.Advanced(ArmadaRoundState(2)), next(1, ArmadaRoundPhase.STATUS, 2, 3))
    }

    @Test fun sixthStatusEndsGameRatherThanCreatingRoundSeven() {
        assertEquals(ArmadaPhaseAdvance.GameFinished, next(6, ArmadaRoundPhase.STATUS))
    }

    @Test(expected = IllegalArgumentException::class) fun invalidRoundIsRejected() { ArmadaRoundState(7) }
    @Test(expected = IllegalArgumentException::class) fun negativePendingIsRejected() { next(1, ArmadaRoundPhase.SHIP, -1) }

    @Test fun scoreIncludesDestroyedEnemiesUpgradesAndObjectivePointsOnly() {
        // Les 10 points d'amélioration sont comptés même si la carte a été défaussée.
        val enemy = listOf(
            ArmadaScoringUnit(50, 10, true),
            ArmadaScoringUnit(15, destroyed = true),
            ArmadaScoringUnit(80, 20, false),
        )
        assertEquals(95L, armadaScore(enemy, objectivePoints = 20))
        assertEquals(0L, armadaScore(emptyList(), objectivePoints = 0))
    }

    @Test fun scoresAreCalculatedAgainstTheOtherFleet() {
        val firstFleet = listOf(ArmadaScoringUnit(30, destroyed = true))
        val secondFleet = listOf(ArmadaScoringUnit(60, destroyed = true))
        assertEquals(60L, armadaScore(secondFleet, 0))
        assertEquals(30L, armadaScore(firstFleet, 0))
    }

    @Test fun scoreWinnerUsesInitiativeOrderForTies() {
        assertEquals(ArmadaPlayer.FIRST, armadaScoreWinner(50, 49))
        assertEquals(ArmadaPlayer.SECOND, armadaScoreWinner(49, 50))
        assertEquals(ArmadaPlayer.SECOND, armadaScoreWinner(50, 50))
    }

    @Test fun eliminationOfOneFleetOverridesScore() {
        assertEquals(ArmadaPlayer.SECOND, armadaEliminationWinner(0, 1, 100, 0))
        assertEquals(ArmadaPlayer.FIRST, armadaEliminationWinner(1, 0, 0, 100))
        assertNull(armadaEliminationWinner(1, 1, 100, 0))
    }

    @Test fun simultaneousEliminationUsesScoreAndThenSecondPlayer() {
        assertEquals(ArmadaPlayer.FIRST, armadaEliminationWinner(0, 0, 100, 80))
        assertEquals(ArmadaPlayer.SECOND, armadaEliminationWinner(0, 0, 80, 100))
        assertEquals(ArmadaPlayer.SECOND, armadaEliminationWinner(0, 0, 80, 80))
    }

    @Test fun sixRoundLocalScenarioExercisesBothFleetsAndPhaseGates() {
        var state = ArmadaRoundState()
        repeat(6) { roundIndex ->
            var shipsA = 1; var shipsB = 1
            var squadronsA = 1; var squadronsB = 1
            fun advance(): ArmadaPhaseAdvance = advanceArmadaPhase(state, shipsA + shipsB, squadronsA + squadronsB)
            fun accept(result: ArmadaPhaseAdvance) { state = (result as ArmadaPhaseAdvance.Advanced).state }
            assertEquals(roundIndex + 1, state.round)
            accept(advance()) // commandement
            assertEquals(ArmadaPhaseAdvance.Blocked(2), advance())
            shipsA--; shipsB--
            accept(advance()) // vaisseaux -> escadrons
            assertEquals(ArmadaPhaseAdvance.Blocked(2), advance())
            squadronsA--; squadronsB--
            accept(advance()) // escadrons -> statut
            val next = advance()
            if (roundIndex == 5) assertEquals(ArmadaPhaseAdvance.GameFinished, next) else accept(next)
        }
    }

    @Test fun scoreUsesLongForLargeTotals() {
        val fleet = listOf(ArmadaScoringUnit(Int.MAX_VALUE, Int.MAX_VALUE, true))
        assertEquals(4294967294L, armadaScore(fleet, 0))
    }

    @Test(expected = IllegalArgumentException::class) fun negativeObjectivePointsAreRejected() { armadaScore(emptyList(), -1) }
    @Test(expected = IllegalArgumentException::class) fun negativeUnitPointsAreRejected() { ArmadaScoringUnit(-1, destroyed = true) }
    @Test(expected = IllegalArgumentException::class) fun negativeShipsRemainingAreRejected() { armadaEliminationWinner(-1, 1, 0, 0) }
}
