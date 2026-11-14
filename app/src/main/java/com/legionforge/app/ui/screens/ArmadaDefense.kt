package com.legionforge.app.ui.screens

/**
 * Résolution de la défense et des dégâts Armada, selon la RRG (pages 6 et « Dégâts »).
 * Pur Kotlin, sans Compose : testable à part.
 */
enum class DefenseKind { EVADE, CONSOLIDATE, SCATTER, CONTAIN, REDIRECT, SALVO }

data class DefenseState(
    /** pions prêts (vert) par type, pour le défenseur. */
    val ready: Map<DefenseKind, Int> = emptyMap(),
)

object ArmadaDefense {

    /**
     * Étape « dépenser les pions de défense ». Un pion par type au maximum (RRG).
     * Retourne le nouvel état et la liste des pions dépensés.
     */
    fun spend(state: DefenseState, kinds: List<DefenseKind>): Pair<DefenseState, List<DefenseKind>> {
        val spent = mutableListOf<DefenseKind>()
        val ready = state.ready.toMutableMap()
        for (k in kinds.distinct()) {
            val n = ready[k] ?: 0
            if (n > 0) {
                ready[k] = n - 1
                spent += k
            }
        }
        return DefenseState(ready) to spent
    }

    /**
     * Dégâts après défense. [dice] = (touches + doubles + critiques) déjà calculés par dé.
     * Ordre RRG : évasion et dispersion sur les dés, puis consolidation sur le total.
     */
    fun damageAfterDefense(rawDamage: Int, spent: List<DefenseKind>): Int {
        var dmg = rawDamage
        if (DefenseKind.SCATTER in spent) dmg = 0
        if (DefenseKind.CONSOLIDATE in spent) dmg = (dmg + 1) / 2   // arrondi au supérieur
        return dmg.coerceAtLeast(0)
    }

    /**
     * Répartition : chaque point retire 1 au bouclier du secteur défendant, puis
     * la coque, puis une carte de dégâts (RRG « Dégâts »).
     * Retourne (boucliers restants, coque restante, cartes de dégâts reçues).
     */
    fun applyDamage(shieldsSector: Int, hull: Int, damage: Int): Triple<Int, Int, Int> {
        var shields = shieldsSector
        var h = hull
        var cards = 0
        repeat(damage) {
            if (shields > 0) shields--
            else { cards++; h-- }   // une carte de dégâts par point non absorbé
        }
        return Triple(shields.coerceAtLeast(0), h.coerceAtLeast(0), cards)
    }

    /**
     * Précisions : chaque précision de l'attaquant neutralise un pion prêt du défenseur
     * (un seul par précision, pion de défense non utilisable pendant cette attaque).
     */
    fun precisionTargets(accuracies: Int, ready: DefenseState): Int {
        val total = ready.ready.values.sum()
        return minOf(accuracies, total)
    }
}
