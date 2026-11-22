#!/usr/bin/env python3
"""Tests de non-régression des seuils Supprimé / Paniqué (rulebook 973-977).

Supprimé : tokens >= Courage        -> 1 action en moins
Paniqué  : tokens >= 2 x Courage    -> 0 action, on retire Courage jetons

Le log sim_logs/game_hotseat_edge_cases.jsonl prouve les deux seuils et le cas
limite (1 token, courage 1 = Supprimé mais PAS Paniqué).
"""
import json
import os
from suppression_rules import resolve, is_panicked, is_suppressed, actions as suppression_actions, tokens_removed_on_panic, after_panic

OUT = "/root/sw-army-builder/sim_logs/game_hotseat_edge_cases.jsonl"


def main():
    cases = [
        ("courage1_sup0", 0, 1),
        ("courage1_sup1_limite", 1, 1),   # Supprimé mais PAS Paniqué
        ("courage1_sup2_panique", 2, 1),  # Paniqué, retire 1 jeton, en reste 1
        ("courage2_sup3_limite", 3, 2),   # Supprimé mais PAS Paniqué
        ("courage2_sup4_panique", 4, 2),  # Paniqué, retire 2, en reste 2
        ("courage0_sup2_garde", 2, 0),    # Courage 0 : jamais Paniqué/Supprimé
    ]
    logs = []
    for name, sup, courage in cases:
        p, s, a, restant, retired = resolve(sup, courage)
        detail = {"cas": name, "sup_init": sup, "courage": courage,
                  "panique": p, "supprime": s, "actions": a,
                  "jetons_retires": retired, "sup_restant": restant}
        logs.append({"t": len(logs) + 1, "player": "SYSTEM",
                     "action": "seuil_panique" if p else "seuil_suppression",
                     "detail": detail, "result": name})

        # Assertions : le test ÉCHOUE si un seuil régresse.
        if courage > 0:
            assert s == (sup >= courage), name
            assert p == (sup >= 2 * courage), name
            assert a == (0 if p else (1 if s else 2)), name
            if p:
                assert retired == courage and restant == sup - courage, name
        else:
            assert not p and not s and a == 2, name

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w") as f:
        for x in logs:
            f.write(json.dumps(x, ensure_ascii=False) + "\n")
    print(f"OK {len(logs)} cas limites -> {OUT}")
    for x in logs:
        print(" ", x["result"], x["detail"]["panique"], x["detail"]["supprime"],
              "actions", x["detail"]["actions"])


if __name__ == "__main__":
    main()
