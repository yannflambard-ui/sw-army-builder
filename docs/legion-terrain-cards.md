# Cartes de terrain Legion — bâtiments vectoriels

 conception (2026-11-19) — expert `expert-dessin-maquette` (9/10), revue `expert-legion-v2` (6/10 → corrections intégrées)

## Principes (non négociables)

- **100 % vectoriel original** (Canvas Compose `drawRect`/`drawPath`/`drawCircle`/`drawLine`). Zéro asset externe embarqué.
- Couvert = **valeur PROPOSÉE à valider en pré-game** entre joueurs (rulebook:2256-2261), jamais une constante de règle. Anneau bleu = léger(1), orange = lourd(2). Crits jamais annulés (2262-2272).
- `size` (LARGE/MEDIUM/SMALL/FILL_IN), `half` (A/B), `rotation` (0/180) : champs à ajouter au modèle AVANT dessin (DOC62 : 2/6/8/16–24 pièces, 2 moitiés assemblées, espacement ≥7" **indicatif · voir Terrain Layout Card**).
- Map Card = seule source des territoires/POI/placement (rulebook:391-395, 421). Aucune bande de déploiement ni objectif codé en dur.
- `blocksLos` indépendant du couvert : area terrain (volume) → true ; simple muret → false.

## Modèle à étendre (LegionTabletopDecor.kt)

```kotlin
enum class TerrainSize { LARGE, MEDIUM, SMALL, FILL_IN }
enum class TerrainKind { /* 9 existants */ + ICE_BASE, GENERATOR, LAVA_RIG, PYLON,
    SKYSCRAPER, TREE_PLATFORM, BUNKER, TOWER_SPIRE, HANGAR, MOISTURE_VAPORIZER }
data class TerrainPiece(..., val size: TerrainSize, val half: Char, val rotation: Int = 0,
                        val surface: Surface = Surface.OBSTACLE /* OPEN|DIFFICULT|IMPASSABLE */)
```

## liste des 8 cartes

| id | planète | palette (3 teintes) | silhouette centrale (kind) | secondaires | couverts proposés | intention de jeu |
|----|---------|---------------------|----------------------------|-------------|-------------------|------------------|
| hoth | Hoth · Base Echo | `#E8EDF2` neige · `#7A9CC4` ombre · `#2F4A66` acier | ICE_BASE : demi-cercle aplati + toit décalé + 2 antennes | GENERATOR, tranchée (2 WALL), IGLOO (ROTUNDA), mur de neige | 2 lourd (ICE_BASE, GENERATOR) · 1 léger (tranchée, igloo) · 0 glace | Longues lignes de vue ; tranchée = choke ; GENERATOR = objectif contesté |
| mustafar | Mustafar · Raffineries | `#1A1210` basalte · `#8C2B12` lave · `#E2611F` braise | LAVA_RIG : rect massif + 3 cheminées + bras en path + bassin cercle | PYLON, rochers (ROTUNDA anguleuse), conduite (WALL+coude), bassin lave (FOUNTAIN, cover 0 décor) | 2 lourd (LAVA_RIG, PYLON, rochers) · 1 léger (conduites) · 0 lave | Conduites canalisent en couloirs ; rigs = points durs ; zones ouvertes punitives |
| coruscant | Coruscant · Cité basse | `#2B3A4A` nuit · `#5B7A99` acier · `#E0B54A` néon | SKYSCRAPER : rect haut + toit trapézoïde + 2 antennes + grille fenêtres | SKYSCRAPER bas, PYLON + plateforme, passerelle (WALL lumineux), STATUE | 2 lourd (SKYSCRAPER, PYLON) · 1 léger (passerelles) · 0 places | Verticalité : tours bloquent LOS et découpent en ruelles ; passerelles = axes contestés |
| kashyyyk | Kashyyyk · Forteresse des arbres | `#1F3D2B` profond · `#4E7A3A` jungle · `#8B6B3F` bois | TREE_PLATFORM : tronc large + plateforme cercle décalée + passerelle + 3 racines | TREE (3 cercles), passerelle (WALL), hutte (TEMPLE conique), palissade | 2 lourd (TREE_PLATFORM, TREE matures, hutte) · 1 léger (passerelles) · 0 clairières | Multi-niveaux : passerelles = goulets ; arbres = couvert lourd ; clairières exposent |
| scarif | Scarif · Complexe plage | `#D9C9A3` sable · `#2E6E73` lagon · `#C65A2C` rouille | BUNKER (LARGE) : grand rect + toit trapézoïde + entrée + 2 embrasures | TOWER_SPIRE (flèche + radome), mur d'enceinte, ROTUNDA radome, CRATE | 2 lourd (BUNKER, TOWER_SPIRE, radome) · 1 léger (murs, caisses) · 0 plage/lagon | Bunker = point dur ; tour radome = objectif ; plages = longues vues brisées par murs |
| jakku | Jakku · Épaves du désert | `#C9A227` ocre · `#8A6B3F` sable ombre · `#55606B` ferraille | HANGAR (LARGE) : grand rect + toit en arc + porte béante + 2 ailes | MOISTURE_VAPORIZER (PYLON + 2 réservoirs), épave (CRATE+aile), TOWER contrôle, muret | 2 lourd (HANGAR, tour) · 1 léger (épaves, vaporisateur) · 0 dunes | Hangar = abri/objectif ; épaves = couverts légers ; dunes = couloirs de tir |
| bespin | Bespin · Nuage City | `#AEBAC6` nuage · `#5D6E82` ombre · `#D98A3C` bronze | SKYSCRAPER : rect fin + base ellipse + plateforme surplomb + 2 antennes | PYLON + disque, ROTUNDA dôme vitré, passerelle (WALL), CRATE fret | 2 lourd (SKYSCRAPER, dôme) · 1 léger (passerelles, pylônes) · 0 vide nuageux | Plateformes flottantes = positions exposées/objectifs ; passerelles = points étroits ; vide non jouable |
| dagobah | Dagobah · Marais | `#2C3A2A` marais · `#4F6B3A` mousse · `#6B5433` tourbe | TEMPLE (existante) : hutte + toit conique penché + 2 poteaux + mousse | TREE tordu, tronc couché (WALL), ROTUNDA butte, FOUNTAIN flaque (cover 0 décor) | 2 lourd (hutte, TREE matures) · 1 léger (troncs, buttes) · 0 flaques | Décor dense, peu de couverts francs ; arbres/hutte structurent ; flaques purement visuelles |

10 nouveaux `TerrainKind` : `ICE_BASE, GENERATOR, LAVA_RIG, PYLON, SKYSCRAPER, TREE_PLATFORM, BUNKER, TOWER_SPIRE, HANGAR, MOISTURE_VAPORIZER`.

## Corrections Legion intégrées

- `rotonde` existante (0,0,150×150, cover 2, blocksLos=false) → **blocksLos=true** (area terrain = Silhouette bloquante, rulebook:787-793).
- NABOO existante : `plaza`(0,0,160×160) et `fontaine`(0,0,55×55) superposées → décaler la fontaine (rulebook:787-794).
- Chaque cover 1/2 affiché avec suffixe « *proposé · à valider en pré-game* » dans l'UI.
- consigne 7" / rotation 180° / quantités DOC62 étiquetées « *indicatif · voir Terrain Layout Card* ».
- Surface par pièce : bâtiment = IMPASSABLE, barricade = OPEN (heavy aux troopers, 749-753), ruines = DIFFICULT.

## Plan d'implémentation

1. Étendre l'enum `TerrainKind` (+10) et le modèle (`size`, `half`, `rotation`, `surface`) — **avant tout dessin**.
2. `terrainColor(kind)` : 3 teintes (ombre/clair/accent) par kind.
3. Renderer `when(piece.kind)` : primitives Canvas, faux 3/4 par toit décalé + ombre portée.
4. 8 objets `TerrainCard` avec pièces aux id uniques, coordonnées mm dans −457..+457.
5. Couvert visible conservé (bleu=1 / orange=2) + marqueur LOS ; aucune règle de jeu ajoutée.
6. i18n : clés `terrain_<id>` dans values/, -fr/, -de/, -es/.
7. Tests JVM : bornes 36", champs size/half/rotation + quantités DOC62, pool par cible, Crits non annulés. Captures Roborazzi 25 % / 300 %.
8. Validation `expert-legion-v2` + `expert-ergonomie-ui`, puis intégration au sélecteur de cartes.

## Sources (inspiration visuelle uniquement, zéro asset embarqué)

- **AMG DOC62 Terrain Layout Cards** — cdn.svc.asmodee.net/.../DOC62_TerrainLayoutCards.pdf — © & TM Lucasfilm / AMG, « personal use only ».
- **AMG DOC56 SWQ Rulebook** — cdn.svc.asmodee.net/.../DOC56_SWQ_Rulebook-2.pdf — © & TM Lucasfilm, personal use only.
- The Fifth Trooper — « Planets! » / « Beginners Guide : Tables and Terrain » — éditorial fan, inspiration thématique/densité.
- printablescenery.com — « Building an Exotic Table for Star Wars Legion » — blog sous copyright, inspiration palettes.
