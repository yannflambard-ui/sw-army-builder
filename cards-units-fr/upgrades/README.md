# Extraction upgrades génériques FR — generic_upgrades_fr.pdf

Source : iron-squadron.fr (drive `1ht63cC5Pm3SWbnPydgU_w4QqHZ4W3b0G`), 22 pages, grille 3×3.

## Géométrie exacte (mesurée 2026-10-24, dpi=300, page 2550×3300 portrait)

- **Gouttières colonnes** (profil colonne, mean<60) : `x[884-942]`, `x[1610-1667]`.
- **Bandeaux-titre / gouttières rangées** (profil rangée) : row0 `y[0-1130]`, row1 `y[1130-2170]`, row2 `y[2170-3209]`.
- **Corps de carte** (contenu clair) : col0 `x[216-884]`, col1 `x[942-1610]`, col2 `x[1667-2335]` (668 de large).
- **Badge de coût** : déborde ~26px dans la gouttière → la carte complète (badge inclus) fait **694 de large** :
  - col0 `x[216-910]`, col1 `x[942-1636]`, col2 `x[1667-2361]`.
- **Hauteur carte** : row0 `y[0-1130]` (1130), row1 `y[1130-2170]` (1040), row2 `y[2170-3209]` (1039). Le bandeau titre est en bas de chaque rangée.

## PIÈGE CRITIQUE — les images FR actuelles sont coupées en haut (badge rogné)
Les images `assets/cards/swl/neutral/fr/*.webp` actuelles (même les "bonnes" 713×1027) sont
**coupées au bord supérieur** : le badge de coût est tronqué, et il y a des marges noires à
gauche/droite/bas (fragments de cartes voisines). C'est le défaut systémique signalé par le user
("haut coupé, carte voisine visible à droite, badge rogné"). La correction = extraire la carte
**complète** (badge→titre, 694 de large) avec `extract_generic_upgrades_fr.py`.

## Script
`extract_generic_upgrades_fr.py` : rend les pages à 300dpi (`pdftoppm -png -r 300`), découpe
chaque cellule avec les bornes ci-dessus, écrit dans `/tmp/gen_cards/` + manifest
`generic_cells_manifest.json` (page/row/col/size).

## État (2026-10-24)
- 198 cellules extraites (22 pages × 9).
- Audit dimensionnel : 96 upgrades FR "suspectes" (dont 20 en basse résolution 365×525 = contenu FAUX confirmé par vision : expanded-databanks montre "Données de Reconnaissance").
- Reste : matcher chaque cellule → slug catalogue, vérifier par vision, remplacer les fausses, bump Room + versionName/versionCode, build CI, upload APK Drive.
