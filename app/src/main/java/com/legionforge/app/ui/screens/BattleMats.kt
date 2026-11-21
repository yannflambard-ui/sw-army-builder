package com.legionforge.app.ui.screens

import androidx.compose.ui.graphics.Color

/**
 * Battle mat planète : VRAIE image (Gemini, 1408x704) comme fond de jeu +
 * couche de LOGIQUE DE JEU vectorielle (surface / couvert / LOS).
 *
 * Décision Yann 2026-11-21 (révisée) : plateau STANDARD 6'x3' (1828,8 x 914,4 mm,
 * x ∈ [-914,4 ; +914,4], y ∈ [-457,2 ; +457,2]). L'image 2:1 couvre EXACTEMENT
 * le plateau : 1 pixel image = 1828,8/1408 mm en x, 914,4/704 mm en y.
 * ATTENTION : toute conversion encore calée sur l'ancien carré 36" décale
 * les emprises de ~164 mm et laisse poser une unité dans l'eau visible.
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

/** Convertit une coordonnée image (1408x704) en mm sur le plateau 6'x3' (2:1 exact). */
private fun imgX(px: Float): Float = (px / 1408f) * 1828.8f - 914.4f
private fun imgY(px: Float): Float = (px / 704f) * 914.4f - 457.2f
private fun imgW(px: Float): Float = (px / 1408f) * 1828.8f
private fun imgH(px: Float): Float = (px / 704f) * 914.4f

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
            // Épave de barge (centre-gauche) bbox 302,288-526,432 -> centre 414,360 / 224x144.
            p("wreck", TerrainKind.WALL, 414f, 360f, 224f, 144f, TerrainSurface.IMPASSABLE, 2, true),
            // Avant-poste impérial antenne (haut-droite) 1128,88-1296,246 -> 1212,167 / 168x158.
            p("outpost", TerrainKind.TOWER, 1212f, 167f, 168f, 158f, TerrainSurface.IMPASSABLE, 2, true),
            // Grand avant-poste beige (haut-centre) 650,0-1030,130 -> 840,65 / 380x130.
            p("hq", TerrainKind.HANGAR, 840f, 65f, 380f, 130f, TerrainSurface.IMPASSABLE, 2, true),
            // Fermes humidité : HG 115,75-310,222 | CD 920,305-1135,470 | CB 490,460-670,614 | BG 90,515-236,642.
            p("moisture_hg", TerrainKind.MOISTURE_VAPORIZER, 212f, 148f, 195f, 147f, TerrainSurface.IMPASSABLE, 2, true),
            p("moisture_cd", TerrainKind.MOISTURE_VAPORIZER, 1027f, 387f, 215f, 165f, TerrainSurface.IMPASSABLE, 2, true),
            p("moisture_cb", TerrainKind.MOISTURE_VAPORIZER, 580f, 537f, 180f, 154f, TerrainSurface.IMPASSABLE, 2, true),
            p("moisture_bg", TerrainKind.MOISTURE_VAPORIZER, 163f, 578f, 146f, 127f, TerrainSurface.IMPASSABLE, 2, true),
            // Mesa bas-centre 620,545-1050,704 -> 835,624 / 430x159 (bloque).
            p("mesa_bas", TerrainKind.TOWER_SPIRE, 835f, 624f, 430f, 159f, TerrainSurface.IMPASSABLE, 2, true),
            // Canyon haut-gauche 0,0-235,262 -> 117,131 / 235x262 (bloque).
            p("canyon_hg", TerrainKind.TOWER_SPIRE, 117f, 131f, 235f, 262f, TerrainSurface.IMPASSABLE, 2, true),
            // Évaporateurs (tours) : HC-G 383,80-460,170 | HD 1005,140-1083,225 | BG 343,540-416,612.
            p("evap_hcg", TerrainKind.PYLON, 421f, 125f, 77f, 90f, TerrainSurface.OPEN, 1, false),
            p("evap_hd", TerrainKind.PYLON, 1044f, 182f, 78f, 85f, TerrainSurface.OPEN, 1, false),
            p("evap_bg", TerrainKind.PYLON, 379f, 576f, 73f, 72f, TerrainSurface.OPEN, 1, false),
        )
    )

    val HOTH = BattleMat(
        "battlemat_hoth", "battlemat_hoth", "battlemats/hoth.jpg", BattleMatPalette(),
        listOf(
            // Générateur bouclier 132,476-318,630 -> 225,553 / 186x154.
            p("generator", TerrainKind.GENERATOR, 225f, 553f, 186f, 154f, TerrainSurface.IMPASSABLE, 2, true),
            // Hangar Base Echo 1135,88-1293,233 -> 1214,160 / 158x145.
            p("hangar", TerrainKind.ICE_BASE, 1214f, 160f, 158f, 145f, TerrainSurface.IMPASSABLE, 2, true),
            // Crêtes glaciaires massives : HC 378,95-640,308 | CB 762,405-1035,628 | HD falaise 1010,0-1408,320.
            p("ridge_hc", TerrainKind.TOWER_SPIRE, 509f, 201f, 262f, 213f, TerrainSurface.IMPASSABLE, 2, true),
            p("ridge_cb", TerrainKind.TOWER_SPIRE, 898f, 516f, 273f, 223f, TerrainSurface.IMPASSABLE, 2, true),
            p("falaise_hd", TerrainKind.TOWER_SPIRE, 1209f, 160f, 398f, 320f, TerrainSurface.IMPASSABLE, 2, true),
            // Épave Snowspeeder 535,395-675,512 -> 605,453 / 140x117 : DIFFICULT, couvert léger.
            p("wreck", TerrainKind.WALL, 605f, 453f, 140f, 117f, TerrainSurface.DIFFICULT, 1, false),
            // P-Towers : HG 315,50-362,100 | G 104,185-152,236 | BD 1258,475-1307,522 | BCD 1065,588-1112,648.
            p("ptower_hg", TerrainKind.PYLON, 338f, 75f, 47f, 50f, TerrainSurface.OPEN, 1, false),
            p("ptower_g", TerrainKind.PYLON, 128f, 210f, 48f, 51f, TerrainSurface.OPEN, 1, false),
            p("ptower_bd", TerrainKind.PYLON, 1282f, 498f, 49f, 47f, TerrainSurface.OPEN, 1, false),
            p("ptower_bcd", TerrainKind.PYLON, 1088f, 618f, 47f, 60f, TerrainSurface.OPEN, 1, false),
        )
    )

    val ENDOR = BattleMat(
        "battlemat_endor", "battlemat_endor", "battlemats/endor.jpg", BattleMatPalette(),
        listOf(
            // Bunker bouclier 665,35-1020,300 -> 842,167 / 355x265.
            p("bunker", TerrainKind.BUNKER, 842f, 167f, 355f, 265f, TerrainSurface.IMPASSABLE, 2, true),
            // Plateforme atterrissage 990,170-1175,375 -> 1082,272 / 185x205.
            p("platform", TerrainKind.ROTUNDA, 1082f, 272f, 185f, 205f, TerrainSurface.OPEN, 0, false),
            // Séquoias massifs : HG 0,0-210,175 | BC 425,595-550,704 | BD 1260,590-1408,704.
            p("tree_hg", TerrainKind.TREE_PLATFORM, 105f, 87f, 210f, 175f, TerrainSurface.DIFFICULT, 1, true),
            p("tree_bc", TerrainKind.TREE_PLATFORM, 487f, 649f, 125f, 109f, TerrainSurface.DIFFICULT, 1, true),
            p("tree_bd", TerrainKind.TREE_PLATFORM, 1334f, 647f, 148f, 114f, TerrainSurface.DIFFICULT, 1, true),
            // Troncs abattus : HG 185,58-420,235 | C 595,403-765,545 | BG 165,445-375,660.
            p("log_hg", TerrainKind.WALL, 302f, 146f, 235f, 177f, TerrainSurface.DIFFICULT, 2, false),
            p("log_c", TerrainKind.WALL, 680f, 474f, 170f, 142f, TerrainSurface.DIFFICULT, 2, false),
            p("log_bg", TerrainKind.WALL, 270f, 552f, 210f, 215f, TerrainSurface.DIFFICULT, 2, false),
            // Épave AT-ST 315,400-495,550 -> 405,475 / 180x150.
            p("atst", TerrainKind.WALL, 405f, 475f, 180f, 150f, TerrainSurface.IMPASSABLE, 2, true),
        )
    )

    val SCARIF = BattleMat(
        "battlemat_scarif", "battlemat_scarif", "battlemats/scarif.jpg", BattleMatPalette(),
        listOf(
            // Lagunes : DIFFICULT traversable, jamais IMPASSABLE, pas de couvert.
            // Centrale 280,240-790,500 | BD 700,320-1408,704 | HG 0,0-600,240.
            p("lagoon_c", TerrainKind.FOUNTAIN, 535f, 370f, 510f, 260f, TerrainSurface.DIFFICULT, 0, false),
            p("lagoon_bd", TerrainKind.FOUNTAIN, 1054f, 512f, 708f, 384f, TerrainSurface.DIFFICULT, 0, false),
            p("lagoon_hg", TerrainKind.FOUNTAIN, 300f, 120f, 600f, 240f, TerrainSurface.DIFFICULT, 0, false),
            // Bunkers : complexe HD antenne 1040,60-1355,300 | HC 865,60-1000,195 | BD 1120,455-1280,605 | CB 835,440-900,510.
            p("bunker_hd", TerrainKind.BUNKER, 1197f, 180f, 315f, 240f, TerrainSurface.IMPASSABLE, 2, true),
            p("bunker_hc", TerrainKind.BUNKER, 932f, 127f, 135f, 135f, TerrainSurface.IMPASSABLE, 2, true),
            p("bunker_bd", TerrainKind.BUNKER, 1200f, 530f, 160f, 150f, TerrainSurface.IMPASSABLE, 2, true),
            p("bunker_cb", TerrainKind.BUNKER, 867f, 475f, 65f, 70f, TerrainSurface.IMPASSABLE, 2, true),
            // Plateforme atterrissage BG 118,362-395,635 -> 256,498 / 277x273.
            p("platform", TerrainKind.HANGAR, 256f, 498f, 277f, 273f, TerrainSurface.OPEN, 0, false),
            // Ponts au-dessus lagunes : HC 533,185-670,300 | CB 655,435-795,555.
            p("bridge_hc", TerrainKind.WALL, 601f, 242f, 137f, 115f, TerrainSurface.OPEN, 0, false),
            p("bridge_cb", TerrainKind.WALL, 725f, 495f, 140f, 120f, TerrainSurface.OPEN, 0, false),
            // Barricades : C 472,320-505,385 | CD 785,335-815,392.
            p("sandbag_c", TerrainKind.WALL, 488f, 352f, 33f, 65f, TerrainSurface.OPEN, 2, false),
            p("sandbag_cd", TerrainKind.WALL, 800f, 363f, 30f, 57f, TerrainSurface.OPEN, 2, false),
        )
    )

    val NABOO = BattleMat(
        "battlemat_naboo", "battlemat_naboo", "battlemats/naboo.jpg", BattleMatPalette(),
        listOf(
            // Canaux remplis d'eau : IMPASSABLE (on ne pose PAS d'unité dedans,
            // comme un bâtiment). HG 375,0-530,320 | MD-haut 880,165-965,320 |
            // MD-bas 880,385-1025,550 | BG 445,600-530,704.
            p("canal_hg", TerrainKind.FOUNTAIN, 452f, 160f, 155f, 320f, TerrainSurface.IMPASSABLE, 0, false),
            p("canal_md_h", TerrainKind.FOUNTAIN, 922f, 242f, 85f, 155f, TerrainSurface.IMPASSABLE, 0, false),
            p("canal_md_b", TerrainKind.FOUNTAIN, 952f, 467f, 145f, 165f, TerrainSurface.IMPASSABLE, 0, false),
            p("canal_bg", TerrainKind.FOUNTAIN, 487f, 652f, 85f, 104f, TerrainSurface.IMPASSABLE, 0, false),
            // Dômes cuivre : G 0,265-90,430 | D 1300,270-1408,440.
            p("dome_g", TerrainKind.ROTUNDA, 45f, 347f, 90f, 165f, TerrainSurface.IMPASSABLE, 2, true),
            p("dome_d", TerrainKind.ROTUNDA, 1354f, 355f, 108f, 170f, TerrainSurface.IMPASSABLE, 2, true),
            // Parterres : HG 577,165-667,285 | HD 738,165-832,285 | MG 565,420-668,552 | MD 738,420-835,552.
            p("garden_hg", TerrainKind.TREE, 622f, 225f, 90f, 120f, TerrainSurface.DIFFICULT, 1, true),
            p("garden_hd", TerrainKind.TREE, 785f, 225f, 94f, 120f, TerrainSurface.DIFFICULT, 1, true),
            p("garden_mg", TerrainKind.TREE, 616f, 486f, 103f, 132f, TerrainSurface.DIFFICULT, 1, true),
            p("garden_md", TerrainKind.TREE, 786f, 486f, 97f, 132f, TerrainSurface.DIFFICULT, 1, true),
            // Fontaine étoile 655,530-755,625 -> 705,577 / 100x95.
            p("fountain", TerrainKind.FOUNTAIN, 705f, 577f, 100f, 95f, TerrainSurface.OPEN, 0, false),
        )
    )

    val GEONOSIS = BattleMat(
        "battlemat_geonosis", "battlemat_geonosis", "battlemats/geonosis.jpg", BattleMatPalette(),
        listOf(
            // Ruches HG 20,20-200,190 | HD 1225,15-1395,180.
            p("hive_hg", TerrainKind.TOWER_SPIRE, 110f, 105f, 180f, 170f, TerrainSurface.IMPASSABLE, 2, true),
            p("hive_hd", TerrainKind.TOWER_SPIRE, 1310f, 97f, 170f, 165f, TerrainSurface.IMPASSABLE, 2, true),
            // Plateaux : HG 0,0-300,330 | HC 480,0-950,260 | HD 1065,0-1408,320 | BC 480,440-920,704.
            p("mesa_hg", TerrainKind.TOWER_SPIRE, 150f, 165f, 300f, 330f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_hc", TerrainKind.TOWER_SPIRE, 715f, 130f, 470f, 260f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_hd", TerrainKind.TOWER_SPIRE, 1236f, 160f, 343f, 320f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_bc", TerrainKind.TOWER_SPIRE, 700f, 572f, 440f, 264f, TerrainSurface.IMPASSABLE, 2, true),
            // Épave LAAT/i 400,300-620,470 -> 510,385 / 220x170.
            p("laat", TerrainKind.WALL, 510f, 385f, 220f, 170f, TerrainSurface.IMPASSABLE, 2, true),
            // Canons acoustiques : HC 580,150-635,210 | HD 1145,200-1210,260 | BC 793,490-845,560 | BCG 640,545-695,618.
            p("cannon_hc", TerrainKind.PYLON, 607f, 180f, 55f, 60f, TerrainSurface.OPEN, 1, false),
            p("cannon_hd", TerrainKind.PYLON, 1177f, 230f, 65f, 60f, TerrainSurface.OPEN, 1, false),
            p("cannon_bc", TerrainKind.PYLON, 819f, 525f, 52f, 70f, TerrainSurface.OPEN, 1, false),
            p("cannon_bcg", TerrainKind.PYLON, 667f, 581f, 55f, 73f, TerrainSurface.OPEN, 1, false),
            // Cratère HD 935,185-1030,265 -> 982,225 / 95x80 : DIFFICULT.
            p("crater_hd", TerrainKind.FOUNTAIN, 982f, 225f, 95f, 80f, TerrainSurface.DIFFICULT, 0, false),
        )
    )

    val ALL = listOf(NONE, TATOOINE, HOTH, ENDOR, SCARIF, NABOO, GEONOSIS)
}
