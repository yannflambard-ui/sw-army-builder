"""Seuils Supprimé / Paniqué (rulebook 973-977) — source de vérité partagée
entre le simulateur game_hotseat.py et son test de non-régression.

Supprimé : tokens >= Courage      -> 1 action en moins
Paniqué  : tokens >= 2 x Courage  -> 0 action, retrait de Courage jetons
Courage 0 : jamais Supprimé ni Paniqué.
"""


def is_suppressed(sup, courage):
    return courage > 0 and sup >= courage


def is_panicked(sup, courage):
    return courage > 0 and sup >= 2 * courage


def actions(sup, courage):
    if is_panicked(sup, courage):
        return 0
    return 1 if is_suppressed(sup, courage) else 2


def tokens_removed_on_panic(sup, courage):
    return courage if is_panicked(sup, courage) else 0


def after_panic(sup, courage):
    return max(0, sup - tokens_removed_on_panic(sup, courage))


def resolve(sup, courage):
    """(panicked, suppressed, actions, sup_restant, jetons_retires)."""
    p = is_panicked(sup, courage)
    s = is_suppressed(sup, courage)
    removed = tokens_removed_on_panic(sup, courage)
    restant = after_panic(sup, courage)
    return p, is_suppressed(restant, courage), actions(sup, courage), restant, removed
