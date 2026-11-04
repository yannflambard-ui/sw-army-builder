package com.legionforge.app.data.model

/**
 * Roues / cadrans de commandement Armada en FILE (FIFO) — logique pure, sans dépendance
 * Android/Compose (testable en JVM). Les cadrans sont empilés face cachée (capacité =
 * valeur de commande du vaisseau), révélés UN PAR MANCHE dans l'ordre (FIFO), créditent des
 * pions d'ordre, puis sont vidés/replacés en fin de manche.
 *
 * Couplé à ArmadaCommandOrder par NOM de commande (pas par instance) pour rester découplé de
 * la couche UI.
 */
object ArmadaCommandStack {

    /** Peut-on pousser un cadran ? (pile < capacité). Capacité 0 = jamais (escadron). */
    fun canPush(stack: List<String>, capacity: Int): Boolean = capacity > 0 && stack.size < capacity

    /** Ajoute un cadran en fin de pile (face cachée) si possible ; renvoie la même pile sinon. */
    fun push(stack: List<String>, name: String, capacity: Int): List<String> =
        if (canPush(stack, capacity)) stack + name else stack

    /**
     * FIFO reveal : retire le cadran en TÊTE de pile et le renvoie avec la pile restante.
     * C'est ce cadran révélé qu'on résout (→ commandOrder) pendant la command phase.
     */
    fun reveal(stack: List<String>): Pair<String?, List<String>> =
        if (stack.isEmpty()) null to stack else stack.first() to stack.drop(1)

    /** Crédite le stock de pions d'ordre du cadran révélé (1 pion par révélation). */
    fun credit(tokens: Map<String, Int>, revealed: String): Map<String, Int> =
        tokens + (revealed to ((tokens[revealed] ?: 0) + 1))

    /** Dépense 1 pion d'ordre (activation) ; ne descend pas sous 0. */
    fun spend(tokens: Map<String, Int>, name: String): Map<String, Int> {
        val cur = tokens[name] ?: return tokens
        if (cur <= 0) return tokens
        return tokens + (name to (cur - 1))
    }

    /**
     * Fin de manche : vide la pile, remet le stock à zéro, retire le cadran révélé.
     * Le joueur re-pose de nouveaux cadrans au round suivant (choix borné par la capacité).
     */
    fun endRound(): Pair<List<String>, Map<String, Int>> = emptyList<String>() to emptyMap()
}
