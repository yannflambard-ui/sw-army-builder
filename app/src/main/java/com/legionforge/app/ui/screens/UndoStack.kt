package com.legionforge.app.ui.screens

/**
 * Pile d'annulation générique (audit gameplay : aucun undo dans l'app).
 *
 * Ring buffer borné : garde les N derniers états. Chaque mutation pousse
 * l'état AVANT ; undo() restaure l'état précédent et dépile.
 *
 * ATTENTION 2 joueurs : l'undo ne doit JAMAIS annuler une action de l'adversaire.
 * Le champ [owner] permet de filtrer : undo() ne dépile que les entrées du
 * même camp, et refuse de traverser une entrée adverse (garde 2J).
 */
class UndoStack<T>(private val capacity: Int = 10) {
    data class Entry<T>(val owner: String, val state: T)

    private val stack = ArrayDeque<Entry<T>>()

    /** Pousse l'état AVANT une mutation. */
    fun push(owner: String, before: T) {
        stack.addLast(Entry(owner, before))
        while (stack.size > capacity) stack.removeFirst()
    }

    val canUndo: Boolean get() = stack.isNotEmpty()

    /**
     * Restaure l'état avant la dernière mutation du camp [owner].
     * Renvoie null si la dernière entrée appartient à l'adversaire (garde 2J)
     * ou si la pile est vide.
     */
    fun undo(owner: String): T? {
        val last = stack.lastOrNull() ?: return null
        if (last.owner != owner) return null
        stack.removeLast()
        return last.state
    }

    fun clear() { stack.clear() }

    val size: Int get() = stack.size
}
