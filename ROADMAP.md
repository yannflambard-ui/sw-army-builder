# SW Army Builder — Roadmap produit

Application compagnon pour **Star Wars: Legion V2** et **Star Wars: Armada**.
Objectif : devenir indispensable **à côté de la table** (trouver une info en 2 secondes, construire une liste honnête, suivre une partie sans PDF ni mémorisation).

> Double filtre appliqué à chaque feature : **utilité réelle pour un joueur** PUIS **intégration propre** dans l'architecture existante (Compose, Room, catalogue JSON v2, mode partie par pager).
> Principe : **faire évoluer SW Army Builder, pas reconstruire.**

---

## Priorités

- 🔴 **P0 — Essentiel** : forte valeur + cohérent avec l'existant
- 🟠 **P1 — Important** : vraie valeur, non essentiel
- 🟡 **P2 — Intéressant** : plus tard
- ⚪ **Backlog** : idées à reconsidérer
- ❌ **Rejeté** : complexité > valeur

---

## 🔴 P0 — Essentiel

### [x] Mode partie compact — tient sur un écran de téléphone (0.9.26)
```
Jeu : Legion + Armada (Commun)
Priorité : P0 | Valeur joueur : Très haute | Complexité : Faible
État actuel : FAIT en 0.9.26 — sections repliables (CollapsibleSection) sur toutes les pages mode partie.
Écrans : CardDetailScreen (LegionUnitPage, ArmadaShipPage, ArmadaSquadronPage)
Pourquoi : le mode partie scrollait sur plusieurs écrans (carte, jetons, roue de commandement, cadrans, effets). Sur téléphone c'était illisible.
Intégration : chaque bloc secondaire (profil, roue de commandement, effets, jetons de défense escadron) est replié par défaut ; le suivi principal (blessures/coque/boucliers) reste déplié. En-tête cliquable (titre + chevron ▲▼) pour masquer/déplier. La roue de commandement (jetons d'ordre) est conservée mais repliée par défaut.
```

### [x] Glossaire / mots-clés cliquables
```
Jeu : Legion + Armada (Commun)
Priorité : P0 | Valeur joueur : Très haute | Complexité : Faible
État actuel : FAIT — les keywords (legionStats.keywords) sont des chips cliquables dans le profil des pages mode partie (Legion + ArmadaSquadron) qui ouvrent le point de règle officiel du wiki (RulePopup). Le dictionnaire keywords_i18n.json reste un stub (1 mot-clé) — à compléter si besoin de traduction FR/DE/ES des noms.
Écrans : LegionUnitPage (profil), ArmadaSquadronPage, fiche carte
```

### [x] Recherche globale dans le texte des cartes
```
Jeu : Commun
Priorité : P0 | Valeur joueur : Haute | Complexité : Moyenne
État actuel : FAIT — SearchScreen + PolymorphicGameDao.searchCards cherchent sur name + rulesText + legionStats + shipStats (LIKE). Interface compacte, clic résultat → bottom sheet des variantes du même nom de base.
Écrans : HomeScreen / barre d'ajout
```

### [x] Filtres de catalogue enrichis
```
Jeu : Commun
Priorité : P0 | Valeur joueur : Haute | Complexité : Faible-moyenne
État actuel : FAIT — ArmyBuilderScreen (bouton ⚙) : chips rang/type (C/O/Co/FS/S/H Legion, V/E/Cmd Armada) + tranche de points (≤25/50/100) + mot-clé (texte libellé, cherche nom+rulesText+legionStats+shipStats). S'ajoutent au filtre faction + scoped upgrades existants.
Écrans : FactionPicker → liste, ArmyBuilder
```

### [ ] Historique + favoris
```
Jeu : Commun
Priorité : P0 | Valeur joueur : Moyenne-haute | Complexité : Faible
État actuel : aucun.
Écrans : HomeScreen (récents + favoris)
Données : consultation (cardId, ts) + favoris (cardId)

Pourquoi : revenir vite vers une carte déjà
Intégration : intercepter l'ouverture de la fiche → journal ; chips Récents/Favoris. Risque quasi nul.
```

---

## 🟠 P1 — Important

### [ ] Collection personnelle (extensions / unités possédées)
```
Jeu : Commun
Priorité : P1 | Valeur joueur : Haute
Données : collection (cardId) + filtre « ma collection » (pattern matchesFaction)
Pourquoi : construire une liste honnête = ce qu'on possède vraiment.
```

### [x] Mode partie Legion enrichi (jetons v2 + états auto) — 0.9.27
```
Jeu : Legion
Priorité : P1 | Valeur : haute (tournoi / partie)
État actuel : FAIT en 0.9.27 (coeur de la killer feature) —
- jetons v2 réels : Aim / Dodge / Surge / Standby (remplacent les génériques Dgt/Etat/Bcl/Ordre).
- états auto-calculés depuis les données : Blessé (wounds < santé×effectif), Supprimé (suppression >= courage), Paniqué (suppression >= 2×courage) — depuis legionStats.courage.
- compteur de suppression dédié qui pilote les états + badges (StateChip).
Écrans : LegionUnitPage, TokenSection
Reste à enrichir (suite P1) : jeton Surge auto selon surgeAttack, Vehicle Damage (résilience) / Ion Damage (véhicules + droïdes), Standby lié au round.
```
> Killer feature Legion : ne plus rien mémoriser à la table.

### [ ] Comparateur (unité vs unité / vaisseau vs vaisseau)
```
Jeu : Commun
Priorité : P1 | Complexité : Moyenne
Écrans : écran comparaison
Données : legionStats / shipStats (déjà structurés)
Intégration : réutiliser StatChip.
```

### [ ] Séquence de tour (checklist activable) Legion / Armada
```
Jeu : Commun (déclinaison par jeu)
Priorité : P1
Écrans : overlay dans mode partie
Pourquoi : rappel des phases sans PDF.
```

---

## 🟡 P2 — Intéressant

- **Partage de liste** : export texte + QR code / deep link.
- **Import/export de listes** (JSON versionné, pattern CatalogJsonImporter réutilisable).
- **Changements / errata** : le bundle LegionHQ V2 a un champ `history` (dates + points/màj) → « Quoi de neuf ».
- **Recherche plein-texte FTS5** quand le catalogue grossit.
- **Versions de cartes** via `history`.

---

## ⚪ Backlog

- **Command cards v2** (125 dans le bundle) — pendule de commandement lié au round tracker.
- **Mode partie Armada avancé** (séquence commande, arcs, portée).
- **Scanner de cartes Android** (rapport valeur/complexité à re-évaluer).

---

## ❌ Rejeté (double filtre joueur × concepteur)

- **Scanner de cartes** : CameraX + matcher visuel pour 666 cartes = coût élevé, gain marginal (recherche par nom suffit).
- **Dice roller** : le jeu a ses dés physiques ; sans valeur de référence.
- **Compte cloud / multi-joueurs / profil** : hors périmètre "compagnon de table", ajoute auth+serveur pour rien (app offline-first).
- **KMP/iOS / reconstruction architecture** : casserait l'existant stable.

---

## 🚀 Killer features — copilote de table

1. **Glossaire mots-clés cliquables** (P0) — le geste le plus fréquent en jeu, résolu en 2 s, unifie Legion+Armada.
2. **Recherche globale dans le texte** (P0) — « laquelle a Pierce 2 » instantané, impossible dans un PDF.
3. **Mode partie Legion intelligent** (P1) — états auto-calculés (Supprimé/Paniqué, Aim/Dodge/Standby, Vehicle/Ion).
4. **Collection** (P1) — liste honnête selon ce qu'on possède.

Ensemble ils transforment l'app d'un *catalogue* en *copilote de table*, sur des données (legionStats, shipStats, keywords) **déjà structurées** en V2, sans nouvelle architecture.

---

## Architecture data — décision en cours

⚠️ Pour toute feature qui demande une **base de données dédiée / sync** (collection multi-appareils, favoris cloud, historique partagé), ne pas se lancer sur une migration lourde pour l'instant :
- Solution pressentie : **MongoDB Atlas (niveau gratuit)** — un compte existe. Les favoris/collection/historique iraient naturellement dans des collections Atlas, pas dans des tables Room locales dédiées.
- En attendant, privilégier les implémentations **sans nouvelle brique data** (glossaire seedé, recherche sur catalog_cards existant, états en mémoire du mode partie).
- À re-évaluer quand on touche au multi-appareils / collection.

---

*Roadmap basée sur l'état du projet au 25/09/2026 — catalogue Legion V2 (0.5.x) + Armada enrichie.*
