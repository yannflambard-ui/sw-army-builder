package com.legionforge.app.domain

/**
 * Seuils de suppression Legion V2 (rulebook AMG DOC56, 973-977).
 *
 * - Supprimé : tokens >= Courage        -> 1 action en moins par activation.
 * - Paniqué  : tokens >= 2 x Courage    -> 0 action, on retire Courage jetons.
 *
 * Source de vérité unique pour l'UI (CardDetailScreen) et les tests JVM.
 * Courage 0 (Vader) ne peut jamais être Supprimé ni Paniqué.
 */
object LegionSuppression {
    fun isSuppressed(suppressions: Int, courage: Int): Boolean =
        courage > 0 && suppressions >= courage

    fun isPanicked(suppressions: Int, courage: Int): Boolean =
        courage > 0 && suppressions >= 2 * courage

    /** Nombre d'actions autorisées lors de l'activation. */
    fun actions(suppressions: Int, courage: Int): Int = when {
        isPanicked(suppressions, courage) -> 0
        isSuppressed(suppressions, courage) -> 1
        else -> 2
    }

    /** Jetons retirés quand l'unité est Paniquée (0 sinon). */
    fun tokensRemovedOnPanic(suppressions: Int, courage: Int): Int =
        if (isPanicked(suppressions, courage)) courage else 0

    /** État après retrait des jetons dus à la Panique. */
    fun afterPanic(suppressions: Int, courage: Int): Int =
        (suppressions - tokensRemovedOnPanic(suppressions, courage)).coerceAtLeast(0)
}
