#!/usr/bin/env python3
"""Vérifie que les formats %1$d des strings localisées sont valides.

Détecte le piège du '%' non échappé (ex. 'Zoom %1$d %' vs 'Zoom %1$d %%')
qui fait crasher String.format à l'exécution sur une locale mais pas en EN.
"""
import glob
import re
import sys

errors = []
for path in sorted(glob.glob("app/src/main/res/values*/strings.xml")):
    content = open(path, encoding="utf-8").read()
    for name, body in re.findall(r'<string name="([^"]+)">(.*?)</string>', content, re.S):
        # Simulation de String.format : seul le format %1$d est utilisé dans l'app.
        # Un '%' isolé (pas '%%', pas '%1$d') est invalide.
        i = 0
        while i < len(body):
            if body[i] == "%":
                if i + 1 < len(body) and body[i + 1] == "%":
                    i += 2
                    continue
                m = re.match(r"%(\d+)\$[dsf]", body[i:])
                if m:
                    i += m.end()
                    continue
                errors.append(f"{path}: {name}: format '%' invalide -> {body!r}")
                break
            i += 1

if errors:
    print("\n".join(errors))
    sys.exit(1)
print("OK : formats de strings valides dans toutes les locales")
