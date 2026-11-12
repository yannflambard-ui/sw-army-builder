# Mode Tabletop Armada : plateau, déplacement, tir

Statut : plan (aucun code de plateau à ce stade).

## Décisions validées (utilisateur)
- Tirs : l'app affiche les arcs et la distance. C'est le joueur qui décide. Pas de blocage automatique.
- Gabarits de manœuvre officiels dès la première version.
- Bascule depuis le mode Play actuel vers un mode Tabletop (plateau 2D).

## Règles de référence (RRG FR, /root/armada_rrg_fr.txt)
- Déplacement (p.25) : cap = plier les articulations du gabarit, dans la limite de la valeur d'embardée au niveau de vitesse actuel. Le vaisseau se déplace sur le gabarit, départ et arrivée du même côté.
- Un vaisseau traverse vaisseaux, escadrons et obstacles. Seules comptent les positions de départ et d'arrivée.
- Chevauchement à l'arrivée : déplacement annulé (retour à la position de départ, vitesse 0).
- Vitesse (p.27) : table de vitesse sur la carte, embardée par colonne (`-`, `I`, `II`).
- Arcs de tir (p.12, 14) : quatre zones de coque (proue, bâbord, tribord, poupe), arcs de tir infinis, arc = prolongement des lignes imprimées.
- Ligne de vue : depuis le point jaune de la zone de coque. Obstruction par vaisseaux et obstacles. Escadrons : ne bloquent pas.
- Portée : mesurée depuis le point le plus proche de la zone attaquante.

## Données disponibles (catalogue)
- 66 vaisseaux (`ARMADA_SHIP`) avec `speed-chart-rows` (table de vitesse et embardée), `attack` / `shield` par zone, `max-speed`, `hull`.
- `size` : renseigné seulement pour les 4 vaisseaux immenses. 62 vaisseaux n'ont pas de taille.

## Dimensions des socles
Source retenue : wiki Fandom (RRG 1.5.0), cohérent avec la FAQ v1.2 :
- Petit : 43 x 71 mm
- Moyen : 63 x 102 mm
- Grand : 77,5 x 129 mm
- Immense : 77,5 x 365 mm (pion) sur 2 socles grands

Le PDF de référence v1.x donne des valeurs anciennes (41x71, 61x102, 76x192) : à ne pas utiliser.

## Bloquant avant géométrie
- Taille par vaisseau à compléter (62 vaisseaux sans `size`). Source à confirmer (fiche par vaisseau ou liste officielle par classe).

## Découpage
1. Plateau 2D zoomable, pions de vaisseaux déplaçables au doigt, règle de distance. Pas de contrôle de règle.
2. Gabarit de manœuvre : choix de vitesse et de cadran (table du catalogue), trajet affiché, validation. Sans blocage.
3. Arcs de tir et distance affichés entre deux vaisseaux, avec ligne de vue. Décision laissée au joueur.
4. Sync des positions entre deux tablettes, avec journal (même rigueur que le mode Play).

## Références externes
- SWArmada (thetrufflegouda/SWArmada, Unity 3D, fan project non commercial, v0.2.7).
  Inspiration seulement : bandes de portée (courte, moyenne, longue), outil de manœuvre avec planification du trajet, dossiers unitaires. Aucun code, modèle 3D, texture ni donnée repris : pas de licence open source affichée, et les données de flottes viennent de Star Forge.
  Choix retenu pour nous : plateau 2D, gabarit officiel au doigt, bandes de portée calculées depuis les zones de coque.

## Silhouettes de base (vectoriel, à valider)
- Fichiers : docs/armada-silhouettes/{small,medium,large,huge}.svg
- Dessinées à l'échelle des socles (mm), proue vers le haut, quatre zones de coque (avant, bâbord, tribord, poupe), point jaune de mire.
- Forme générique : une silhouette par classe, pas une par vaisseau. Les 66 vaisseaux utiliseront la classe de leur socle.
- Contrôle visuel : 4 zones distinctes, lignes partant du centre vers les coins. Pas encore validé par l'utilisateur.

## Socles et gabarits d'arc (mise à jour)
- Socles aux dimensions RRG 1.5.0 (small 43x71, medium 63x102, large 77,5x129, huge 77,5x365 mm).
- Quatre lignes d'arc partant du centre de la coque jusqu'au bord du socle (géométrie vérifiée : extrémités sur le contour).
- Angles : arcs latéraux 120°, arcs avant et arrière 60° (source : mesures de gabarit, forum Reddit ; à confirmer sur la RRG).
- Pas d'image officielle reprise : le socle et les arcs sont dessinés, pas copiés.
- Point jaune de mire placé dans la moitié avant.

## Table des classes de socle (validée)
- Fichier : docs/data/armada_ship_size_table.json (66 vaisseaux du catalogue).
- Source : pages wiki Category:Small/Medium/Large/Huge_Sized_Ship (RRG 1.5.0).
- Résultat : 20 petits, 12 moyens, 22 grands, 2 immenses classés (Executor I/II), 10 à confirmer.
- Recoupement : 12 vaisseaux comparés au tableau utilisateur, zéro écart.
- Les 10 à confirmer : flottilles (Gozanti, GR-75), prototypes Star Dreadnought, variantes de nommage (Victory I, Consular c70, Hardcell Battle Refit, Gozanti Cruiser(s)). À trancher, pas à deviner.

## Recoupement (archive fan KDYards)
- archive.kdyards.com : cartes fan-made, non officielles. Fiche Acclamator Imperial Refit II : taille « Medium » (cohérent avec la table).
- Ne sert ni aux dimensions de socle ni aux arcs. Pas repris dans le catalogue.

## Décisions sur les 10 cas restants (validées par l'utilisateur)
- Flottilles (Gozanti Assault Carriers / Cruisers / Cruiser, GR-75 Combat Retrofits / Medium Transports) : classe « flotilla », pas de taille de socle tranchée.
- Prototypes Star Dreadnought (Command, Assault) : immense (huge), comme Executor.
- Victory I (variante, republic) : grand. Consular c70 : petit. Hardcell Battle Refit : petit.
- Résultat : 22 petits, 12 moyens, 23 grands, 4 immenses, 5 flottilles, 0 à confirmer.
