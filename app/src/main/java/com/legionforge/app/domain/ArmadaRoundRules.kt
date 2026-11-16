package com.legionforge.app.domain

/** RRG 1.5, « Round », p. 22. Modèle autonome : non relié à l'écran Play. */
enum class ArmadaRoundPhase { COMMAND, SHIP, SQUADRON, STATUS }

data class ArmadaRoundState(val round: Int = 1, val phase: ArmadaRoundPhase = ArmadaRoundPhase.COMMAND) {
    init { require(round in 1..6) }
}

sealed class ArmadaPhaseAdvance {
    data class Advanced(val state: ArmadaRoundState) : ArmadaPhaseAdvance()
    data class Blocked(val pendingActivations: Int) : ArmadaPhaseAdvance()
    object GameFinished : ArmadaPhaseAdvance()
}

/**
 * pendingShips et pendingSquadrons sont les unités survivantes non activées
 * des DEUX joueurs. Les effets qui empêchent une activation doivent être
 * résolus par l'appelant : ce modèle ne gère ni les passes ni ces exceptions.
 * STATUS -> GameFinished signifie que la phase de statut a été résolue.
 */
fun advanceArmadaPhase(
    state: ArmadaRoundState,
    pendingShips: Int,
    pendingSquadrons: Int,
): ArmadaPhaseAdvance {
    require(pendingShips >= 0 && pendingSquadrons >= 0)
    val pending = when (state.phase) {
        ArmadaRoundPhase.SHIP -> pendingShips
        ArmadaRoundPhase.SQUADRON -> pendingSquadrons
        else -> 0
    }
    if (pending > 0) return ArmadaPhaseAdvance.Blocked(pending)
    return when (state.phase) {
        ArmadaRoundPhase.COMMAND -> ArmadaPhaseAdvance.Advanced(state.copy(phase = ArmadaRoundPhase.SHIP))
        ArmadaRoundPhase.SHIP -> ArmadaPhaseAdvance.Advanced(state.copy(phase = ArmadaRoundPhase.SQUADRON))
        ArmadaRoundPhase.SQUADRON -> ArmadaPhaseAdvance.Advanced(state.copy(phase = ArmadaRoundPhase.STATUS))
        ArmadaRoundPhase.STATUS -> if (state.round == 6) ArmadaPhaseAdvance.GameFinished
            else ArmadaPhaseAdvance.Advanced(ArmadaRoundState(state.round + 1))
    }
}

/** Coût d'une unité ; les améliorations du vaisseau comptent même défaussées.
 * L'appelant fournit le coût de toutes les améliorations initialement équipées.
 * Pour un escadron, upgradePoints vaut zéro. RRG 1.5, « Scoring », p. 22. */
data class ArmadaScoringUnit(
    val basePoints: Int,
    val upgradePoints: Int = 0,
    val destroyed: Boolean,
) {
    init { require(basePoints >= 0 && upgradePoints >= 0) }
}

/** Points gagnés contre la flotte adverse, plus les points de victoire d'objectif. */
fun armadaScore(enemyFleet: List<ArmadaScoringUnit>, objectivePoints: Int): Long {
    require(objectivePoints >= 0)
    return enemyFleet.filter { it.destroyed }
        .sumOf { it.basePoints.toLong() + it.upgradePoints.toLong() } + objectivePoints.toLong()
}

enum class ArmadaPlayer { FIRST, SECOND }

/** En cas d'égalité le second joueur gagne, indépendamment du camp local. */
fun armadaScoreWinner(firstScore: Long, secondScore: Long): ArmadaPlayer {
    require(firstScore >= 0 && secondScore >= 0)
    return if (firstScore > secondScore) ArmadaPlayer.FIRST else ArmadaPlayer.SECOND
}

/**
 * RRG 1.5, « Winning and Losing », p. 33 : fin immédiate si tous les
 * vaisseaux d'une flotte sont détruits. En cas de destruction simultanée,
 * le score départage ; null signifie que les deux flottes ont des survivants.
 * Les flottes doivent avoir été initialisées avant cet appel.
 */
fun armadaEliminationWinner(
    firstShipsRemaining: Int,
    secondShipsRemaining: Int,
    firstScore: Long,
    secondScore: Long,
): ArmadaPlayer? {
    require(firstShipsRemaining >= 0 && secondShipsRemaining >= 0)
    require(firstScore >= 0 && secondScore >= 0)
    return when {
        firstShipsRemaining == 0 && secondShipsRemaining == 0 -> armadaScoreWinner(firstScore, secondScore)
        firstShipsRemaining == 0 -> ArmadaPlayer.SECOND
        secondShipsRemaining == 0 -> ArmadaPlayer.FIRST
        else -> null
    }
}
