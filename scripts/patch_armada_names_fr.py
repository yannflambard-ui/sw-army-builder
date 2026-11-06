#!/usr/bin/env python3
"""Applique les overrides `names.fr` Armada à catalog.json (idempotent, pérenne).

Lit armada_names_fr_overrides.json (clé = nom EN, valeur = nom FR officiel)
et injecte names['fr'] sur les cartes ARMADA_V15 correspondantes (sans écraser
en/de/es). À relancer après build_catalog.py pour ne pas perdre les noms FR
de la snapshot rkbodenner (qui contient des erreurs de variante).
"""
import json, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CATALOG = ROOT / "app/src/main/assets/catalog.json"
OVERRIDES = ROOT / "armada_names_fr_overrides.json"

def main() -> None:
    overrides = json.load(open(OVERRIDES, encoding="utf8"))
    doc = json.load(open(CATALOG, encoding="utf8"))
    items = doc if isinstance(doc, list) else doc.get("cards") or doc.get("entries") or []
    applied, missing = 0, 0
    for it in items:
        game = it.get("gameSystem")
        if game != "ARMADA_V15":
            continue
        fr = overrides.get(it.get("name"))
        if fr is None:
            continue
        names = it.get("names") or {}
        if names.get("fr") == fr:
            applied += 1
            continue
        names = dict(names)
        names["fr"] = fr
        it["names"] = names
        applied += 1
    # cartes non touchées (reste en fallback EN)
    touched = {n for n in overrides}
    present = {it.get("name") for it in items if it.get("gameSystem") == "ARMADA_V15"}
    missing = len(touched - present)
    target = doc if isinstance(doc, list) else doc
    json.dump(doc, open(CATALOG, "w", encoding="utf8"), ensure_ascii=False, indent=2)
    print(f"names.fr appliqués: {applied} | noms absents du catalogue: {missing}")

if __name__ == "__main__":
    main()
