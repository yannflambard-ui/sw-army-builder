# Legion Forge — Notes de développement

## 🔍 Détection des cartes FR hors-format (2026-10-24)
- **`detect_bad_cards.py` est INVALIDE — supprimé.** Il jugeait une carte par la LUMINOSITÉ
  des bords (RIGHT/LEFT_LIGHT, TOP_WHITE). Faux : un liseré clair >120 est NORMAL sur une carte
  Legion → il flag ~100% des références EN bien cadrées aussi (empire/rebel/republic/separatists
  EN = toutes flaggées). Ne jamais se fier à l'heuristique de luminosité des bords.
- **Fiable : `scripts/detect_bad_dims.py`** — contrôle DIMENSIONNEL.
  Critères (de la recette 0.9.75) : ratio carte std FR ~0.694 (713x1027) / pleine 0.698 (726x1040),
  paysage ~1.431 (1039x726). Suspect si ratio hors [0.68,0.71] et hors [1.40,1.46], OU exactement
  365x525 (= CONTENU FAUX confirmé, basse rés mauvais contenu). NE PAS flag les 713/714x1027.
- Résultat 2026-10-24 : **79 cartes FR réellement hors-format** (78 neutral/upgrades + 1 separatist).
  Réparties en : ratio trop large/étroit (ex 776x1039 r=0.748, 675x1117 r=0.604) + **14×365x525
  contenu FAUX** (linked-targeting-array, b1-security-droid, expanded-databanks, e-5s-b1,
  viper-recon-droid, astromech-droid, e-60r-b1, b1-battle-droid, stormtrooper-captain,
  fragmentation-grenades, the-darksaber-gideon, flametrooper, beskad-duelist, force-pike-warrior).
- État actuel des 143 upgrades neutral/fr : **40 combinaisons de dimensions** (365x525 à 777x1040)
  = chaos de crops obsolètes. Source de vérité pour les corriger : 198 cellules gen_cards
  (`/tmp/gen_cards/`, 668-694x1039-1130) + manifest. À recadrer comme en 0.9.75/0.9.89 : OCR titre
  (psm 6 bandeau bas, upscaled) → match FR→EN → **vérification vision AVANT injection** (+ bump Room).
- **Progression 0.9.92 (2026-10-24)** : 78/79 corrigées. Les 7 dernières via PDFs Upgrades faction
  (barc-twin-laser-gunner, barc-rps-6-gunner, b1-battle-droid, z-6-trooper, e-5s-b1-battle-droid,
  ig-100-magnaguard-pilot, m-45-ion-blaster). Restent 9 NON-corrigeables depuis les PDFs FR
  disponibles (gen_cards, Upgrades faction, Units faction, Mandalorian BattleForce, Nouvelles cartes) :
  - 3 upgrades d'affiliation Mandalorienne : clan-wren, clan-kryze, sabine-wren (absentes des PDFs)
  - 6 upgrades : bowcaster-wookiee (PDF=Wookiee Arme d'Épaule≠Bowcaster), expanded-databanks,
    at-rt-flamethrower (PDF=TL-TT Flamethrower≠AT-RT), jetpack-rockets (PDF=Jetpack de Din≠Jetpack
    Rockets), veteran-clone-pilot, twin-blaster-cannons (PDF republic≠separatist).
  → pipeline textless ou autre source FR.

## Build & CI
- **TOUJOURS build par GitHub Actions** (workflow `.github/workflows/build.yml`), jamais en local.
- Le serveur Debian (8 Go RAM, gradle Xmx512m, workers=1) n'a **pas assez de mémoire** pour compiler : `Not enough memory to run compilation` (ksp/Room/Compose dépassent 512m). Inutile d'essayer.
- Workflow : `assembleDebug` + `testDebugUnitTest` sur ubuntu-latest (16 Go) → artifact `legion-forge-debug/app-debug.apk`.
- Boucle de livraison : push `master` → `gh run watch` → `gh run download -n legion-forge-debug` → upload APK sur Google Drive.
- APK Drive (même lien/fichier, renommé au fil des versions) :
  https://drive.google.com/file/d/1ep1Sr7eDoONqJx6jGfim6dfamurggY0-/view?usp=drivesdk

## Reporting remote des erreurs (diagnostic)
- L'app poste un issue GitHub quand le catalogue est vide **sans exception** (`CrashReporter.reportEvent`), via `POST /api.github.com/repos/yannflambard-ui/legion-forge-ci/issues`.
- **Crucial** : l'API GitHub exige une authentification. Sans token → HTTP 401 → `reportEvent` avale en silence → **rien ne remonte**. Le token `Authorization: Bearer` est obligatoire.
- Token fourni par `BuildConfig.GITHUB_TOKEN` :
  - CI : injecté par le workflow via `permissions: issues: write` + step `Inject GitHub token` (`GITHUB_TOKEN` natif → `app/github_token.properties`). Jamais en clair dans le repo.
  - Local : `app/github_token.properties` (gitignored, `legionforge_github_token=...`) → fichier absent du VCS.
- Le token fin-grained de secours (demo) doit avoir scope `issues: write` sur le repo UNIQUEMENT (pas `repo` global).
- Déduplication : `reportEvent` ignore si un issue de même titre est déjà open (search issues).

## Catalogue (seed)
- Sources : `assets/catalog.json` (1032 cartes : 624 LEGION_V2 + 408 ARMADA_V15). Attributions dans `assets/CATALOG_SOURCES.md`.
- **Images de cartes (miniatures + mode partie)** : les cartes Armada ont des images (vraies cartes) depuis le dataset Ryan Kingston (`armada.ryankingston.com/img/cards/`), converties en WebP dans `assets/cards/` et référencées par `imageAssetPath`. Legion : images partielles depuis LegionHQ (v1, ~43% unités / 46% upgrades). Fallback : nom si pas d'image. `CardArtwork` (colonne gauche builder) et `CardPlayImage` (bas des pages mode partie) les affichent. **Structure** : `assets/cards/<swa|swl>/<faction>/<nom-officiel-anglais>.webp` (variantes de même nom suffixées par les points). Scripts : `enrich_armada_images.py` (génère) + `rename_card_images.py` (structure dossiers) + `fetch_ryk_images.py` (télécharge).
- **Factions Armada complètes** : le catalogue contient désormais les 4 factions (Empire, Rebel, Republic, Separatist) — vaisseaux/escadrons/commandants Republic+Sep ajoutés depuis `armada_bsdata_catalog.json` (stats shipStats incluses). `BuilderRepository.seedCatalog` force un re-seed si `armadaShipCount() < 60` (une vieille base à 46 vaisseaux sans Republic/Sep est re-seedée).
- **Stats Armada (mode partie)** : les vaisseaux/escadrons de `catalog.json` n'ont pas de stats dans rulesText. Elles sont injectées depuis `assets/armada_bsdata_catalog.json` (dataset BSData = « Armada Fleet Builder ») dans un champ JSON `shipStats` (hull, boucliers front/right/left/rear, maxSpeed) sur chaque carte, par `scripts/enrich_armada_stats.py` (match normalisé nom+faction, fuzzy : « Boba Fett (Slave I) », typo BSData « Dreadought »). Appelé en fin de `build_catalog.py` (flag `--skip-stats`). `BuilderRepository.seedCatalog` force un re-seed si des vaisseaux manquent de stats (`armadaUnitsWithoutStats()`), pas seulement si un système est vide.
- `BuilderRepository.seedCatalog` : seed si LEGION<190 OU ARMADA<40 OU des vaisseaux Armada sans stats OU <60 vaisseaux (comptage par gameSystem). Ne coupe plus sur un `cardCount()>=400` global (ça laissait une vieille DB périmée sans les cartes du bon gameSystem → "Catalogue vide" sans erreur).
- `CatalogJsonImporter` : import versionné hors-ligne (schemaVersion==1, IDs uniques, points>=0, nom non vide).
- `observeCards(system, factionId)` : `WHERE gameSystem=:system AND (factionId=:factionId OR factionId='neutral')`.
- **Référence Armada** : Rules Reference Guide 1.6.0 (janv 2025, AMG) + Errata 5.5 — https://atomicmassgames.com/swarmadadocs/. Dégâts critiques réservés aux vaisseaux (squadrons ne peuvent ni résoudre ni subir de critiques). Contraintes flotte : ≤1/3 pts en escadrons (arrondi sup), pas d'upgrade dupliquée sur un même vaisseau, 1 seul commandant.
- **PIÈGE Gson+Kotlin (cause racine du catalogue vide)** : Gson n'applique PAS les valeurs par défaut Kotlin. Un champ absent du JSON reste `null` (défaut JVM), pas `emptyMap()`. `CardDefinition.names` était `= emptyMap()` mais Gson le laissait null → `toJsonNames` faisait `names.isEmpty()` → NPE sur les 974 cartes → 0 mappées → catalogue vide (LEGION + Armada). Fix : `toJsonNames(names: Map<String,String>?)` avec null-check. Toujours null-checker les champs à défaut Kotlin après un `Gson().fromJson`.

## DB
- Room version 6 (`legionforge.db`), `fallbackToDestructiveMigration`, migrations 1→2 (catalog_cards), 2→3 (chosenSlot), 3→4 (names), 5→6 (shipStats) puis destructif au-delà.
- DAO polymorphe `PolymorphicGameDao` gère Legion V2 + Armada V1.5.

## App
- Kotlin/Compose, minSdk 26, targetSdk 34, JDK 17, Room 2.6.1, Gson 2.11.
- GameSystem : `LEGION_V2`, `ARMADA_V15`. CardKind : LEGION_UNIT/UPGRADE, ARMADA_SHIP/SQUADRON/UPGRADE, COMMANDER.
- Mode jeu : swipe cartes, blessures/boucliers directionnels, jetons défense vert/rouge, dégâts critiques, round tracker, activation upgrades, localisation FR/DE/ES.
## 0.9.23 (05/10/2026) — Fiche vaisseau Armada compactee (mode play)
- Icône de la carte (CardArtwork) a gauche du titre + nom en titleMedium pour tenir sur tele.
- Coque et vitesse en courant/max (ex 2/8, 2/3) sur une ligne sous les caracs CMD/SQN/ENG (StatChip avec display=).
- RETIRE le gros compteur coque rouge (BigCounter 0xFFFF6B6B) + la HealthBar rouge sous les cadrans boucliers ("truc rouge a cote du resume des boucliers").
- StatChip: padding vertical 3->6dp, fillMaxWidth, maxLines=1 (largeurs egalisees).
- Roue de commandement: affiche QUE l ordre selectionne (pastille 96x84) + pions d ordre compacts; si aucun ordre, les 4 pions cliquables.
- Ameliorations/effets: spacings reduits (8->6dp, padding effet 12->8dp).
- Version 0.9.23 / code 74 (bump meme commit que la feature).

## 0.9.25 (05/10/2026) — Mode partie: retrait carte bas + bouton crit des cadrans
- RETIRE le bouton "+DGT CRIT" de la ligne cadrans (header ship Armada). Ajout de critiques via le panneau Effets uniquement (bouton +crit existant).
- RETIRE la grosse carte (CardPlayImage) affichee en bas des 4 pages mode partie (Legion, ArmadaShip, Commander, Squadron).
- La carte s affiche desormais en OVERLAY (Dialog plein ecran, CardZoomOverlay) en haut du contenu, au clic sur la miniature du header (CardArtwork 52/64dp cliquable).
- CardBlock : nouveau parametre onCardClick (miniature cliquable) utilise par Legion/Commander/Squadron.
- Supprime les cles string inutilisees : add_dgt_crit, card_label (4 locales).
- Version 0.9.25 / code 76.

## 0.9.27 (06/10/2026) — Mode partie Legion intelligent : etats auto + jetons v2
- P1 killer #3 (roadmap) : les jetons generiques Dgt/Etat/Bcl/Ordre sont remplaces par les vrais jetons d'action Legion v2 — Aim (jaune), Dodge (violet), Surge (bleu), Standby (orange).
- Etat auto de l'unite calcule depuis les donnees de la carte :
  - Blesse (Wounded) si wounds < maxHp (sante x effectif).
  - Supprime (Suppressed) si suppression >= courage (legionStats.courage).
  - Panique (Panicked) si suppression >= 2 x courage.
- Compteur de repetition dedie (pilote l'etat Supprime/Panique) + badges d'etat cliquables (StateChip).
- Seuils conformes a la roadmap P1 ("Suppression: Supprime >= courage / Panique >= 2x courage").
- Nouvelles cles string (4 locales EN/FR/DE/ES) : legion_token_{aim,dodge,surge,standby,suppression}, legion_suppression_label, legion_state_{wounded,suppressed,panicked}.
- Est-ce que la recherche globale + glossaire cliquable + filtres catalogue etaient DEJÀ implémentes (P0 roadmaps non coches) : voir ROADMAP.md — marques FAIT.
- Version 0.9.27 / code 78

### 0.9.72-0.9.73 — Descriptions d'effet des upgrades + images localisées DE
- **effet d'upgrade en mode play** : le bundle LegionHQ n'embarque que les conditions
  d'équipement (requirements), pas le texte d'effet. Ajout champ catalog `effectText`
  sourcé du dataset DoctorDizzee "Legion As Text" (à jour du rulebook 21/04/2026),
  mapping committé scripts/upgrade_effect_text.json (115 génériques). En mode play le
  paneau d'effets affiche l'IMAGE de la carte (texte official localisé) + effectText
  quand dispo, fallback conditions lisibles. ActiveEffect.imageAssetPath ; Room v20→21.
- **images localisées** : architecture imageAssetPaths (map par langue) + imagePath(locale)
  avec fallback EN. Les images DE (34, Asmodee) et FR (98) extraites restaient NON
  référencées (map vide). scripts/inject_localized_image_paths.py scanne le disque
  (assets/cards/swl/<faction>/<locale>/*.webp) et remplit imageAssetPaths — appelé en
  fin de build_legion_v2.py (reproductible). Version 0.9.73 / code 124.
