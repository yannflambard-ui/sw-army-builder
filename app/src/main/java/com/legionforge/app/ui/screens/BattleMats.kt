package com.legionforge.app.ui.screens

import androidx.compose.ui.graphics.Color

/**
 * Battle mat planète : VRAIE image (Gemini, 1408x704) comme fond de jeu +
 * couche de LOGIQUE DE JEU vectorielle (surface / couvert / LOS).
 *
 * Décision Yann 2026-11-21 : le plateau reste CARRÉ 36 pouces (mm -457..457).
 * L'image 2:1 est affichée en « contain » dans ce carré, donc elle occupe la
 * bande centrale y ∈ [-228,6 ; +228,6] et toute la largeur x ∈ [-457 ; +457].
 *
 * Les [pieces] ne sont PAS du décor artistique : ce sont les seules zones que
 * le moteur de jeu comprend (où les unités peuvent aller, ce qui bloque la LOS,
 * quel couvert proposer). Tout le reste (dunes, palmiers, neige...) est dans
 * l'image = décor PUR, jamais modélisé ici.
 *
 * Règles (expert Legion V2, rulebook) :
 * - surface OPEN/DIFFICULT/IMPASSABLE (731-842) : seul IMPASSABLE interdit le
 *   drop (846-848, voir LegionTerrainCollision).
 * - cover 1/2 = VALEUR PROPOSÉE à valider en pré-game (2256-2261), appliquée
 *   comme pool de dés blancs de la CIBLE (2262-2272), Crits jamais annulés.
 * - blocksLos booléen INDÉPENDANT du couvert (787-793).
 */
data class BattleMat(
    val id: String,
    val nameKey: String,
    val assetPath: String,
    val palette: BattleMatPalette,
    val pieces: List<TerrainPiece>,
)

/** Palette de l'overlay logique — couleurs FROIDES réservées, jamais ocre/rouge/vert des mats. */
data class BattleMatPalette(
    val open: Color = Color(0x5900E5A8),        // pointillé, zéro remplissage
    val difficult: Color = Color(0x73FFD24A),   // tirets + remplissage léger
    val impassable: Color = Color(0xFF3D6E),    // trait plein + hachures
)

/** Convertit une coordonnée image (1408x704) en mm plateau (contain dans le carré). */
private fun imgX(px: Float): Float = (px / 1408f) * 914.4f - 457.2f
private fun imgY(px: Float): Float = (px / 704f) * 457.2f - 228.6f
private fun imgW(px: Float): Float = (px / 1408f) * 914.4f
private fun imgH(px: Float): Float = (px / 704f) * 457.2f

/** Pièce logique depuis des coordonnées image. */
private fun p(
    id: String, kind: TerrainKind, px: Float, py: Float, pw: Float, ph: Float,
    surface: TerrainSurface, cover: Int, blocksLos: Boolean,
) = TerrainPiece(
    id = id, kind = kind,
    xMm = imgX(px), yMm = imgY(py), wMm = imgW(pw), hMm = imgH(ph),
    cover = cover, blocksLos = blocksLos, surface = surface,
)

object BattleMats {
    /** Plateau vectoriel d'origine (aucune image). */
    val NONE = BattleMat("none", "battlemat_none", "", BattleMatPalette(), emptyList())

    val TATOOINE = BattleMat(
        "battlemat_tatooine", "battlemat_tatooine", "battlemats/tatooine.jpg", BattleMatPalette(),
        listOf(
            // Épave barge/char des sables (centre-gauche) : obstacle bloquant lourd.
            p("wreck", TerrainKind.WALL, 300f, 380f, 230f, 150f, TerrainSurface.IMPASSABLE, 2, true),
            // Avant-poste impérial + antenne parabolique (droite) : obstacle.
            p("outpost", TerrainKind.TOWER, 1080f, 260f, 170f, 150f, TerrainSurface.IMPASSABLE, 2, true),
            // Fermes humidité à dômes (haut-centre, bas-gauche).
            p("moisture1", TerrainKind.MOISTURE_VAPORIZER, 620f, 150f, 120f, 110f, TerrainSurface.IMPASSABLE, 2, true),
            p("moisture2", TerrainKind.MOISTURE_VAPORIZER, 380f, 580f, 120f, 110f, TerrainSurface.IMPASSABLE, 2, true),
            // Mesas / canyons (bords).
            p("mesa1", TerrainKind.TOWER_SPIRE, 90f, 120f, 150f, 160f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa2", TerrainKind.TOWER_SPIRE, 1300f, 540f, 170f, 150f, TerrainSurface.IMPASSABLE, 2, true),
            // Évaporateurs : couvert léger, ne bloque PAS la LOS.
            p("evap1", TerrainKind.PYLON, 780f, 300f, 70f, 70f, TerrainSurface.OPEN, 1, false),
            p("evap2", TerrainKind.PYLON, 560f, 460f, 70f, 70f, TerrainSurface.OPEN, 1, false),
        )
    )

    val HOTH = BattleMat(
        "battlemat_hoth", "battlemat_hoth", "battlemats/hoth.jpg", BattleMatPalette(),
        listOf(
            // Générateur de bouclier sphérique (bas-gauche) : obstacle area.
            p("generator", TerrainKind.GENERATOR, 300f, 520f, 180f, 170f, TerrainSurface.IMPASSABLE, 2, true),
            // Hangar Base Echo taillé glace (haut-droite).
            p("hangar", TerrainKind.ICE_BASE, 1150f, 160f, 200f, 170f, TerrainSurface.IMPASSABLE, 2, true),
            // Crêtes / blocs glaciaires massifs : obstacles bloquants.
            p("ridge1", TerrainKind.TOWER_SPIRE, 180f, 140f, 170f, 160f, TerrainSurface.IMPASSABLE, 2, true),
            p("ridge2", TerrainKind.TOWER_SPIRE, 850f, 590f, 190f, 150f, TerrainSurface.IMPASSABLE, 2, true),
            // Épave Snowspeeder en cratère (centre) : difficult, couvert léger.
            p("wreck", TerrainKind.WALL, 700f, 350f, 150f, 120f, TerrainSurface.DIFFICULT, 1, false),
            // Tourelles P-Tower : scatter OPEN (emplacement), couvert léger proposé.
            p("ptower1", TerrainKind.PYLON, 480f, 200f, 80f, 80f, TerrainSurface.OPEN, 1, false),
            p("ptower2", TerrainKind.PYLON, 1000f, 480f, 80f, 80f, TerrainSurface.OPEN, 1, false),
            // Tranchées : DIFFICULT traversable, LOS libre.
            p("trench", TerrainKind.WALL, 900f, 300f, 220f, 60f, TerrainSurface.DIFFICULT, 1, false),
        )
    )

    val ENDOR = BattleMat(
        "battlemat_endor", "battlemat_endor", "battlemats/endor.jpg", BattleMatPalette(),
        listOf(
            // Bunker générateur bouclier (haut-droite) : obstacle.
            p("bunker", TerrainKind.BUNKER, 1130f, 180f, 200f, 170f, TerrainSurface.IMPASSABLE, 2, true),
            // Plateforme atterrissage circulaire : dalle OPEN, jalon.
            p("platform", TerrainKind.ROTUNDA, 900f, 470f, 150f, 140f, TerrainSurface.OPEN, 0, false),
            // Séquoias / tronc massif : obstacle area bloquant.
            p("tree1", TerrainKind.TREE_PLATFORM, 140f, 160f, 180f, 170f, TerrainSurface.DIFFICULT, 1, true),
            p("tree2", TerrainKind.TREE_PLATFORM, 280f, 560f, 180f, 170f, TerrainSurface.DIFFICULT, 1, true),
            // Troncs abattus/creux : DIFFICULT, couvert lourd, LOS libre (bas).
            p("log1", TerrainKind.WALL, 560f, 300f, 190f, 70f, TerrainSurface.DIFFICULT, 2, false),
            p("log2", TerrainKind.WALL, 720f, 590f, 200f, 70f, TerrainSurface.DIFFICULT, 2, false),
            // Épave AT-ST (décor/obstacle, pas une unité vivante ici).
            p("atst", TerrainKind.WALL, 520f, 500f, 110f, 100f, TerrainSurface.IMPASSABLE, 2, true),
        )
    )

    val SCARIF = BattleMat(
        "battlemat_scarif", "battlemat_scarif", "battlemats/scarif.jpg", BattleMatPalette(),
        listOf(
            // Lagunes turquoise : DIFFICULT traversable, JAMAIS IMPASSABLE, pas de couvert.
            p("lagoon1", TerrainKind.FOUNTAIN, 420f, 180f, 260f, 150f, TerrainSurface.DIFFICULT, 0, false),
            p("lagoon2", TerrainKind.FOUNTAIN, 980f, 520f, 280f, 160f, TerrainSurface.DIFFICULT, 0, false),
            // Bunkers béton impériaux : obstacles bloquants.
            p("bunker1", TerrainKind.BUNKER, 1240f, 170f, 190f, 160f, TerrainSurface.IMPASSABLE, 2, true),
            p("bunker2", TerrainKind.BUNKER, 1200f, 520f, 170f, 150f, TerrainSurface.IMPASSABLE, 2, true),
            // Plateforme atterrissage surélevée (bas-gauche) : dalle OPEN.
            p("platform", TerrainKind.HANGAR, 260f, 490f, 250f, 200f, TerrainSurface.OPEN, 0, false),
            // Ponts / chaussées au-dessus lagunes : OPEN, aucun couvert.
            p("bridge", TerrainKind.WALL, 620f, 320f, 200f, 55f, TerrainSurface.OPEN, 0, false),
            // Sacs de sable / barricades débarquement : OPEN, couvert lourd proposé.
            p("sandbag1", TerrainKind.WALL, 760f, 200f, 120f, 50f, TerrainSurface.OPEN, 2, false),
            p("sandbag2", TerrainKind.WALL, 880f, 380f, 120f, 50f, TerrainSurface.OPEN, 2, false),
        )
    )

    val NABOO = BattleMat(
        "battlemat_naboo", "battlemat_naboo", "battlemats/naboo.jpg", BattleMatPalette(),
        listOf(
            // Canaux de Theed : DIFFICULT traversable, aucun couvert, LOS libre.
            p("canal1", TerrainKind.FOUNTAIN, 450f, 300f, 130f, 380f, TerrainSurface.DIFFICULT, 0, false),
            p("canal2", TerrainKind.FOUNTAIN, 950f, 300f, 130f, 380f, TerrainSurface.DIFFICULT, 0, false),
            // Dômes cuivre (bords) : obstacles bloquants lourds.
            p("dome1", TerrainKind.ROTUNDA, 80f, 350f, 170f, 170f, TerrainSurface.IMPASSABLE, 2, true),
            p("dome2", TerrainKind.ROTUNDA, 1330f, 350f, 170f, 170f, TerrainSurface.IMPASSABLE, 2, true),
            // Jardins royaux : terrain de zone, DIFFICULT, couvert léger, bloque LOS.
            p("garden1", TerrainKind.TREE, 640f, 200f, 150f, 140f, TerrainSurface.DIFFICULT, 1, true),
            p("garden2", TerrainKind.TREE, 790f, 520f, 150f, 140f, TerrainSurface.DIFFICULT, 1, true),
            // Fontaine centrale : jalon OPEN, pas de couvert.
            p("fountain", TerrainKind.FOUNTAIN, 705f, 350f, 110f, 110f, TerrainSurface.OPEN, 0, false),
        )
    )

    val GEONOSIS = BattleMat(
        "battlemat_geonosis", "battlemat_geonosis", "battlemats/geonosis.jpg", BattleMatPalette(),
        listOf(
            // Ruches géonosiennes (haut-gauche, haut-droite) : obstacles bloquants.
            p("hive1", TerrainKind.TOWER_SPIRE, 180f, 150f, 190f, 180f, TerrainSurface.IMPASSABLE, 2, true),
            p("hive2", TerrainKind.TOWER_SPIRE, 1230f, 150f, 190f, 180f, TerrainSurface.IMPASSABLE, 2, true),
            // Mesas / plateaux érodés : obstacles bloquants.
            p("mesa1", TerrainKind.TOWER_SPIRE, 700f, 110f, 200f, 150f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa2", TerrainKind.TOWER_SPIRE, 300f, 560f, 190f, 150f, TerrainSurface.IMPASSABLE, 2, true),
            // Épave LAAT/i (centre-gauche) : obstacle bloquant lourd.
            p("laat", TerrainKind.WALL, 520f, 360f, 200f, 130f, TerrainSurface.IMPASSABLE, 2, true),
            // Canons acoustiques sur hauteurs : emplacement OPEN, couvert léger, LOS libre.
            p("cannon1", TerrainKind.PYLON, 900f, 250f, 85f, 85f, TerrainSurface.OPEN, 1, false),
            p("cannon2", TerrainKind.PYLON, 1050f, 540f, 85f, 85f, TerrainSurface.OPEN, 1, false),
            // Cratères d'impact : DIFFICULT traversable, pas de couvert.
            p("crater1", TerrainKind.FOUNTAIN, 680f, 540f, 110f, 100f, TerrainSurface.DIFFICULT, 0, false),
        )
    )

    val ALL = listOf(NONE, TATOOINE, HOTH, ENDOR, SCARIF, NABOO, GEONOSIS)
}
