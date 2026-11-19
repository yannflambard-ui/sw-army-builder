# Fiche Play Store — SW Army Builder

Version validée : 2.67.0 (code 348), commit 1ca8910.
Lien Drive de test : https://drive.google.com/file/d/1ep1Sr7eDoONqJx6jGfim6dfamurggY0-/view?usp=drivesdk

## Titre

SW Army Builder — Legion & Armada

## Description courte (80 caractères max)

Constructeur de listes Star Wars Legion v2 et Armada 1.6.0, avec mode tabletop.

## Description complète

SW Army Builder est l'outil de constitution et de suivi de listes d'armées pour
Star Wars Legion v2 et Star Wars Armada 1.6.0.

LEGION v2
- Catalogue complet des unités et upgrades, avec validation des points, des rangs
  et des quotas de mercenaires.
- Mode Play : manches, activation des unités, Pass Pool, départage au dé de défense
  rouge (d6), battle deck à 9 cartes et main de départ à 7 cartes.
- Mode Tabletop : plateau tactile avec pions ronds issus des illustrations des
  cartes, portées en bandes de 6 pouces, socles petits/crantés, arc avant, couvert
  par pool de dés blancs, blessures et activations. État local, à côté de la table.
- Dés conformes aux faces FFG : attaque d8, défense d6.

ARMADA 1.6.0
- Flottes, escadrons et points d'escouade.
- Phase de commandement avec cadrans, pile d'ordres et suivi des manches.
- Calculateur de dégâts, objectifs et jetons de victoire, score de flotte sur 6 manches.

GENERAL
- Recherche et fiches détaillées avec règles complètes.
- Partage et synchronisation Nearby entre joueurs.
- Interface sombre, tactile, accessible (cibles 48 dp, texte 11 sp minimum).
- Anglais, français, allemand, espagnol.

Application non officielle. Star Wars est une marque de Lucasfilm Ltd.
Ce produit n'est ni approuvé ni sponsorisé par Lucasfilm ou Atomic Mass Games.

## Captures suggérées (à générer depuis l'app sur appareil)

Ordre recommandé pour le listing :
1. home-411x891.png — écran d'accueil (Legion / Armada)
2. legion-tabletop-411x891.png — plateau tabletop Legion
3. search-411x891.png — recherche et catalogue
4. legion-tabletop-360x640.png — plateau sur petit écran

Les goldens Roborazzi se trouvent dans app/src/test/snapshots/. Ils valident le
rendu en CI mais ne remplacent pas des captures prises sur appareil physique.

## Politique de confidentialité

L'application fonctionne hors ligne. Les listes d'armées et l'état du jeu sont
stockés localement sur l'appareil (Room). Aucune donnée n'est envoyée à un serveur.
La fonction de partage utilise Nearby Devices (Google Play Services) uniquement
entre appareils présents physiquement, sans passage par un serveur tiers.
Aucune information personnelle n'est collectée, aucune publicité n'est affichée,
aucun traceur n'est intégré.

Contact : [à renseigner par le propriétaire avant publication]

## Notes de release 2.67.0

Boucle de validation finale (3 passes, 4 experts) sur les écrans Play et Tabletop.

Legion — tap sur un pion = fiche dépliante du mode Play (camps allié/adverse) ;
plateau tabletop (dés FFG, portées 6", socles, couvert pool blanc, blessures) ;
Round 0 : Map cards appariées, Blue Player par jet de 4 dés noirs avec départage,
alternance des 2 modifications par joueur, main exacte 2x1/2x2/2x3 + Standing Orders.

Armada RRG 1.6.0 — élimination (vaisseaux seuls, départage au score), objectifs et
jetons de victoire intégrés au score, cadran à révélation unique par manche,
avancement de phase et fin de partie en 2 joueurs, 2 tirs depuis 2 zones de coque
distinctes. Ancien écran de déploiement « Zone 1/2 » supprimé au profit du plateau.

Interface — cibles tactiles 48 dp, texte 11 sp minimum, icônes Material avec
contentDescription, 4 langues (EN/FR/DE/ES), captures Roborazzi en CI.

Contact éditeur de la fiche Play : à renseigner par le propriétaire avant publication
(blocant externe — non inventé).
