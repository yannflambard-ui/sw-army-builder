package com.legionforge.app.ui.screens

import kotlin.random.Random

/**
 * Dés d'attaque Armada (table « Star Wars: Armada Dice Results Table », vérifiée 18/18).
 * Chaque dé a 8 faces. Un résultat = liste de symboles tirés.
 * Dégâts : (vaisseau, escadron). Le critique seul ne touche que les vaisseaux.
 */
enum class ArmadaDieColor { BLACK, BLUE, RED }

enum class ArmadaFace(val ship: Int, val squadron: Int, val crit: Boolean, val accuracy: Boolean) {
    HIT(1, 1, false, false),
    HIT_HIT(2, 2, false, false),
    CRIT(1, 0, true, false),
    HIT_CRIT(2, 1, true, false),
    ACCURACY(0, 0, false, true),
    BLANK(0, 0, false, false),
}

object ArmadaDiceTable {
    /** Faces par couleur (8 faces chacune), lues sur la table de référence. */
    val faces: Map<ArmadaDieColor, List<ArmadaFace>> = mapOf(
        ArmadaDieColor.BLACK to listOf(
            ArmadaFace.HIT, ArmadaFace.HIT, ArmadaFace.HIT, ArmadaFace.HIT,
            ArmadaFace.HIT_CRIT, ArmadaFace.HIT_CRIT, ArmadaFace.BLANK, ArmadaFace.BLANK),
        ArmadaDieColor.BLUE to listOf(
            ArmadaFace.HIT, ArmadaFace.HIT, ArmadaFace.HIT, ArmadaFace.HIT,
            ArmadaFace.CRIT, ArmadaFace.CRIT, ArmadaFace.ACCURACY, ArmadaFace.ACCURACY),
        ArmadaDieColor.RED to listOf(
            ArmadaFace.HIT, ArmadaFace.HIT, ArmadaFace.HIT_HIT, ArmadaFace.CRIT,
            ArmadaFace.CRIT, ArmadaFace.ACCURACY, ArmadaFace.BLANK, ArmadaFace.BLANK),
    )

    /** Vérifie que la table respecte 8 faces par dé. */
    fun selfCheck(): Boolean = faces.values.all { it.size == 8 }
}

data class ArmadaRoll(val color: ArmadaDieColor, val face: ArmadaFace)

object ArmadaDiceRoller {
    /** Lance N dés de chaque couleur. Random injectable pour les tests. */
    fun roll(black: Int, blue: Int, red: Int, rng: Random = Random.Default): List<ArmadaRoll> {
        val out = mutableListOf<ArmadaRoll>()
        fun add(color: ArmadaDieColor, n: Int) {
            val f = ArmadaDiceTable.faces.getValue(color)
            repeat(n) { out += ArmadaRoll(color, f[rng.nextInt(8)]) }
        }
        add(ArmadaDieColor.BLACK, black)
        add(ArmadaDieColor.BLUE, blue)
        add(ArmadaDieColor.RED, red)
        return out
    }

    /** Dégâts totaux vs vaisseau ou escadron. */
    fun damage(rolls: List<ArmadaRoll>, vsShip: Boolean): Int =
        rolls.sumOf { if (vsShip) it.face.ship else it.face.squadron }

    /** Précisions : nombre de pions de défense que l'attaquant peut choisir de neutraliser. */
    fun accuracies(rolls: List<ArmadaRoll>): Int = rolls.count { it.face.accuracy }
}
