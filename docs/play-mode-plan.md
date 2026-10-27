# Mode Play — Plan de livraison (tracker focalisé)

> Arbitrage post-table-ronde. L'app = **cerveau de la partie** (mémoire + état), PAS le moteur
> (résolution). Dés physiques chez le joueur. Dev stoppé → reprise sur ce plan.
> Base code réelle : `SyncedUnitState`, `PlaySyncPayload`, `PlaySyncViewModel`, `BattlefieldView`
> (git v1.5→v1.9.1). Beaucoup de socle existe déjà — le plan exploite l'existant, ne le refait pas.

## Verdict & périmètre
- **Tracker focalisé** : on enregistre l'état, on ne résout rien.
- **Non-négociable** : 1 tap/action · écran principal = état de la partie · zéro clignotement ·
  boutons ⚡ Auto étendus.
- **Hors périmètre** : résolution dés/combat, mouvement, règles embarquées complètes.

---

## PRÉREQUIS — Sync 2 joueurs, armées distinctes (bloquant, à faire en premier)

**Objectif** : chaque tablette est **maître de sa propre armée** (édition locale), l'adversaire est
**lecture seule** ; les états **partagés** (activation/round/VP/tour) sont synchronisés. Sync
**local-first non-bloquante** : une panne réseau ne bloque JAMAIS la partie.

**État actuel (déjà en place)** : `ownStates` (éditable, broadcast) vs `oppStates` (reçue, RO) —
le modèle maître/lecture-seule existe. `PlaySyncPayload` transporte `round`, `units`, `listJson`,
`deploymentDone`, `roundCard`, `turnOwner`. `updateOwnState`/`updateOppState` séparent déjà les 2.

**Écarts à combler** :
1. **Payload d'état partagé incomplet** : `type="state"` ne porte que `units`. Il faut un payload
   `game` portant round + VP + tour + objectifs actifs + phase (voir F5).
2. **Non-bloquant** : `updateOwnState` ne broadcast que si `CONNECTED` — OK, mais il faut un
   **journal local (append-only)** des mutations pour re-sync à la reconnexion (delta + horodatage),
   sinon les états divergents pendant une coupure sont perdus.
3. **Identité d'instance** : `instanceId` doit être **stable et unique par joueur** (préfixe
   `A:`/`B:`) pour que les 2 tablettes ne se marchent pas dessus sur la même clé.
4. **Reconnexion** : bouton « Resync » (re-demande l'état complet adverse) + merge par
   `last-write-wins` sur horodatage.

**Données à ajouter** : `GameStatePayload` (round, vpA, vpB, turnOwner, activeObjectives,
phase, seq), `MutationLog` (append-only local, horodaté), préfixe joueur sur `instanceId`.

**Fichiers** : `data/nearby/PlaySyncManager.kt`, `ui/viewmodel/PlaySyncViewModel.kt`,
`data/local/` (nouveau DAO journal), `data/model/GameData.kt`.

**Effort** : **M** (2-3 j). Bloquant pour F3/F4/F5.

---

## F1 — Blessures par figurine (besoin n°1)

**Objectif** : tap +1/−1 sur les blessures d'une unité, seuil de destruction, état
détachée/éliminée. 1 tap par action.

**Écran/interaction** : sur chaque unité de `BattlefieldView`, badge blessures `X/Y` (Y = seuil
détachée, Z = seuil éliminée). Tap → popover avec `−1` / `+1` (gros boutons) + état
`Détachée`/`Éliminée` (toggle). À `X ≥ seuil détachée` → badge passe orange + état détachée ;
à `X ≥ seuil éliminée` → unité grisée, retirée du compteur d'activations.

**Données** : `SyncedUnitState.wounds` (existe) + ajouter `detachedThreshold`, `eliminatedThreshold`
(issus des stats de la carte), `detached: Boolean`, `eliminated: Boolean`.

**Fichiers** : `data/nearby/PlaySyncManager.kt` (champs), `ui/screens/BattlefieldView.kt`,
`ui/viewmodel/PlaySyncViewModel.kt` (action `applyWound`).

**Effort** : **S** (1 j).

---

## F2 — Jetons d'état par unité (suppression = critique)

**Objectif** : jetons aim/dodge/surge/suppression/standby visibles dans l'écran principal.

**Écran/interaction** : rangée d'icônes de jetons sous chaque unité (symboles daltonisme, déjà
utilisés). Tap sur un jeton → +1 ; tap long → −1. **Suppression** : compteur dédié, badge rouge
dès >0, seuil de panique affiché (voir F4). Zéro clignotement : les jetons sont statiques, seule
la valeur change.

**Données** : `SyncedUnitState.tokens: List<String>` (existe) + `suppressions: Int` (existe).
Normaliser en `Map<TokenType, Int>` pour éviter les doublons de liste.

**Fichiers** : `data/nearby/PlaySyncManager.kt`, `ui/screens/BattlefieldView.kt`,
`ui/viewmodel/PlaySyncViewModel.kt` (action `addToken`/`removeToken`).

**Effort** : **S** (1 j).

---

## F3 — Pool d'ordres + activation (lié aux jetons)

**Objectif** : savoir qui a reçu un ordre et qui a activé ce round.

**Écran/interaction** : sur chaque unité, 2 indicateurs : `📨 ordre reçu` (toggle) et
`✓ activée` (toggle). Compteur global `X/Y activées` (existe déjà sur non-vaincues). Bouton
**⚡ Auto** étendu : tire au hasard une unité non activée/non vaincue **avec ordre** (sinon sans),
et marque l'activation. Le pool d'ordres = liste des unités avec `ordre reçu` ; l'activation
consomme l'ordre.

**Données** : `SyncedUnitState.orderReceived: Boolean`, `activated: Boolean` (existe pour Armada,
réutiliser pour Legion). Lier à F2 : un ordre reçu peut poser un jeton (ex. standby).

**Fichiers** : `data/nearby/PlaySyncManager.kt`, `ui/screens/BattlefieldView.kt`,
`ui/viewmodel/PlaySyncViewModel.kt` (action `toggleOrder`/`activateUnit`).

**Effort** : **M** (1-2 j). Dépend du prérequis sync (tour partagé).

---

## F4 — Suppression & panique

**Objectif** : compteur de suppression, seuil, jet de panique.

**Écran/interaction** : badge suppression (de F2) avec seuil de panique affiché (`X / seuil`).
Quand `suppressions ≥ seuil` → unité marquée **Paniquée** (bordure rouge, icône). Bouton
**⚡ Auto** : « Jet de panique » → l'app tire un résultat aléatoire (1-6) et affiche le résultat
(le joueur lance le dé physique et compare ; l'app ne résout pas). Bouton « Lever la panique »
(1 tap) quand la condition est levée.

**Données** : `SyncedUnitState.suppressions` (existe) + `panicThreshold` (stat carte),
`panicking: Boolean`.

**Fichiers** : `data/nearby/PlaySyncManager.kt`, `ui/screens/BattlefieldView.kt`,
`ui/viewmodel/PlaySyncViewModel.kt` (action `setSuppression`/`rollPanic`).

**Effort** : **S** (0.5-1 j). Dépend de F2.

---

## F5 — Score + round + objectifs

**Objectif** : VP, round, objectifs actifs.

**Écran/interaction** : bandeau haut de `BattlefieldView` : `Round N` (tap +1), `VP A — VP B`
(tap +1/−1 de chaque côté), liste des objectifs actifs (toggle actif/inactif, tap pour marquer
capturé). Bouton **⚡ Auto** : « Nouveau round » → incrémente round, reset des activations/ordres
de toutes les unités (1 tap).

**Données** : `GameStatePayload` (round, vpA, vpB, activeObjectives, phase) — nouveau, partagé via
sync (prérequis). `activeObjectives: List<String>` (ids de cartes objectif).

**Fichiers** : `data/nearby/PlaySyncManager.kt` (payload `game`), `ui/screens/BattlefieldView.kt`,
`ui/viewmodel/PlaySyncViewModel.kt` (actions `nextRound`/`addVP`/`toggleObjective`).

**Effort** : **M** (1-2 j). Dépend du prérequis sync.

---

## Ordre de livraison & dépendances

```
[PRÉREQUIS sync] ──► F1 ──► F2 ──► F4 ──► F3 ──► F5
                        (indépendant)   (dép. F2)  (dép. sync)
```

- **P0 (jouable en vrai partie, 2 joueurs)** : PRÉREQUIS + F1 + F2 + F3.
- **P1** : F4 (panique) + F5 (score/round/objectifs).
- **P2** : états véhicule (immobilized/weapon), couverture, upgrades utilisés, déploiement,
  journal de partie exportable, mode solo 2 armées étendu.

## Critères de sortie P0 (définition of done)
- 2 tablettes, armées distinctes, maître/lecture-seule vérifié.
- Blessures, jetons, ordres, activation synchronisés en direct.
- Coupure réseau → la partie continue, resync à la reconnexion sans perte.
- 1 tap/action, zéro clignotement, ⚡ Auto fonctionnel sur activation + nouveau round.
