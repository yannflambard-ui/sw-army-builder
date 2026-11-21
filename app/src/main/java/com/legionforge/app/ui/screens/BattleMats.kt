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
    outline: List<Pair<Float, Float>>? = null, // contour réel en pixels image (1408x704)
) = TerrainPiece(
    id = id, kind = kind,
    xMm = imgX(px), yMm = imgY(py), wMm = imgW(pw), hMm = imgH(ph),
    cover = cover, blocksLos = blocksLos, surface = surface, outline = outline,
)

object BattleMats {
    /** Plateau vectoriel d'origine (aucune image). */
    val NONE = BattleMat("none", "battlemat_none", "", BattleMatPalette(), emptyList())

    val TATOOINE = BattleMat(
        "battlemat_tatooine", "battlemat_tatooine", "battlemats/tatooine.jpg", BattleMatPalette(),
        listOf(
            p("canyon_hg", TerrainKind.TOWER_SPIRE, 58f, 131f, 115f, 262f, TerrainSurface.IMPASSABLE, 2, true),
            p("canyon_hg_sud", TerrainKind.TOWER_SPIRE, 175f, 243f, 120f, 38f, TerrainSurface.IMPASSABLE, 2, true),
            p("falaise_hc", TerrainKind.TOWER_SPIRE, 505f, 32f, 130f, 63f, TerrainSurface.IMPASSABLE, 2, true),
            p("falaise_hd", TerrainKind.TOWER_SPIRE, 845f, 68f, 370f, 135f, TerrainSurface.IMPASSABLE, 2, true),
            p("rocher_hc", TerrainKind.TOWER_SPIRE, 494f, 76f, 35f, 24f, TerrainSurface.IMPASSABLE, 1, true),
            p("dome_hg", TerrainKind.BUNKER, 182f, 159f, 133f, 128f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(189f to 91f, 220f to 141f, 229f to 161f, 211f to 186f, 161f to 211f, 111f to 189f, 109f to 164f, 120f to 129f, 136f to 99f)),
            p("annexe_hg", TerrainKind.WALL, 263f, 118f, 30f, 85f, TerrainSurface.IMPASSABLE, 2, true),
            p("reservoirs_hg", TerrainKind.PYLON, 303f, 126f, 50f, 88f, TerrainSurface.IMPASSABLE, 2, true),
            p("tour_hg1", TerrainKind.PYLON, 402f, 105f, 36f, 50f, TerrainSurface.OPEN, 1, false),
            p("tour_hg2", TerrainKind.PYLON, 442f, 115f, 36f, 50f, TerrainSurface.OPEN, 1, false),
            p("tour_hg3", TerrainKind.PYLON, 400f, 150f, 36f, 37f, TerrainSurface.OPEN, 1, false),
            p("pilier_couche", TerrainKind.WALL, 532f, 181f, 42f, 58f, TerrainSurface.DIFFICULT, 1, false),
            p("conduite_bg", TerrainKind.WALL, 125f, 290f, 110f, 50f, TerrainSurface.DIFFICULT, 1, false),
            p("rocher_erosion", TerrainKind.TOWER_SPIRE, 324f, 302f, 125f, 75f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(350f to 272f, 370f to 288f, 365f to 303f, 344f to 312f, 320f to 319f, 296f to 321f, 277f to 314f, 272f to 298f)),
            p("rocher_petit", TerrainKind.TOWER_SPIRE, 403f, 274f, 30f, 34f, TerrainSurface.IMPASSABLE, 1, true),
            p("epave_centre", TerrainKind.WALL, 412f, 388f, 221f, 85f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(512f to 316f, 497f to 369f, 467f to 384f, 412f to 409f, 347f to 421f, 295f to 397f, 367f to 337f, 447f to 319f)),
            p("batiment_centre_haut", TerrainKind.BUNKER, 688f, 232f, 165f, 135f, TerrainSurface.IMPASSABLE, 2, true),
            p("auvent_hd", TerrainKind.BUNKER, 792f, 244f, 41f, 56f, TerrainSurface.OPEN, 1, false),
            p("bunker_hd", TerrainKind.BUNKER, 1214f, 166f, 166f, 158f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(1174f to 98f, 1278f to 144f, 1270f to 176f, 1223f to 236f, 1191f to 214f, 1121f to 169f)),
            p("lanceurs_hd", TerrainKind.PYLON, 1044f, 184f, 76f, 84f, TerrainSurface.IMPASSABLE, 2, true),
            p("caisses_hd1", TerrainKind.WALL, 938f, 155f, 60f, 38f, TerrainSurface.OPEN, 1, false),
            p("caisses_hd2", TerrainKind.WALL, 894f, 208f, 78f, 64f, TerrainSurface.OPEN, 1, false),
            p("abri_bas", TerrainKind.BUNKER, 1019f, 331f, 156f, 42f, TerrainSurface.IMPASSABLE, 1, true),
            p("tuyau_hd", TerrainKind.WALL, 1292f, 288f, 83f, 55f, TerrainSurface.DIFFICULT, 1, false),
            p("falaise_bd", TerrainKind.TOWER_SPIRE, 1371f, 185f, 74f, 190f, TerrainSurface.IMPASSABLE, 2, true),
            p("rocher_hd", TerrainKind.TOWER_SPIRE, 1326f, 64f, 56f, 49f, TerrainSurface.IMPASSABLE, 1, true),
            p("dome_bg_g", TerrainKind.BUNKER, 162f, 580f, 142f, 126f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(138f to 514f, 180f to 520f, 210f to 553f, 214f to 586f, 189f to 621f, 143f to 635f, 115f to 616f, 99f to 573f, 101f to 545f)),
            p("dome_bg_d", TerrainKind.BUNKER, 578f, 539f, 184f, 152f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(596f to 461f, 656f to 491f, 658f to 504f, 646f to 551f, 604f to 566f, 596f to 584f, 518f to 604f, 491f to 504f, 558f to 474f)),
            p("conteneur_bg", TerrainKind.WALL, 590f, 630f, 42f, 28f, TerrainSurface.OPEN, 1, false),
            p("chariot_bg", TerrainKind.WALL, 131f, 440f, 80f, 62f, TerrainSurface.DIFFICULT, 1, false),
            p("capteur_bg1", TerrainKind.PYLON, 362f, 556f, 38f, 32f, TerrainSurface.OPEN, 0, false),
            p("capteur_bg2", TerrainKind.PYLON, 400f, 560f, 30f, 32f, TerrainSurface.OPEN, 0, false),
            p("capteur_bg3", TerrainKind.PYLON, 396f, 594f, 31f, 32f, TerrainSurface.OPEN, 0, false),
            p("falaise_g", TerrainKind.TOWER_SPIRE, 82f, 494f, 165f, 46f, TerrainSurface.IMPASSABLE, 2, true),
            p("ravin_bg", TerrainKind.TOWER_SPIRE, 307f, 646f, 146f, 115f, TerrainSurface.IMPASSABLE, 2, true),
            p("falaise_bdg", TerrainKind.TOWER_SPIRE, 664f, 667f, 79f, 74f, TerrainSurface.IMPASSABLE, 2, true),
            p("batiment_gradins", TerrainKind.BUNKER, 1265f, 508f, 92f, 86f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(1201f to 455f, 1303f to 455f, 1303f to 549f, 1201f to 549f, 1206f to 520f, 1206f to 490f)),
            p("batiment_auvent", TerrainKind.BUNKER, 1192f, 600f, 174f, 96f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(1225f to 545f, 1278f to 545f, 1278f to 575f, 1250f to 595f, 1225f to 595f, 1218f to 624f, 1146f to 624f, 1146f to 552f, 1218f to 552f)),
            p("dome_bd", TerrainKind.BUNKER, 1042f, 411f, 224f, 118f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(945f to 335f, 1095f to 335f, 1095f to 440f, 1058f to 440f, 1060f to 468f, 1012f to 468f, 1012f to 413f, 945f to 412f)),
            p("plateau_bd", TerrainKind.TOWER_SPIRE, 876f, 628f, 346f, 152f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(703f to 635f, 744f to 614f, 802f to 589f, 851f to 564f, 900f to 554f, 927f to 601f, 973f to 638f, 1017f to 704f, 703f to 704f)),
            p("rocher_bd", TerrainKind.TOWER_SPIRE, 826f, 422f, 100f, 86f, TerrainSurface.IMPASSABLE, 1, true),
            p("rocher_bd2", TerrainKind.TOWER_SPIRE, 1330f, 630f, 58f, 58f, TerrainSurface.IMPASSABLE, 1, true),
        )
    )

    val HOTH = BattleMat(
        "battlemat_hoth", "battlemat_hoth", "battlemats/hoth.jpg", BattleMatPalette(),
        listOf(
            p("crevasse_angle_hg", TerrainKind.TOWER_SPIRE, 135f, 78f, 270f, 155f, TerrainSurface.IMPASSABLE, 0, false, outline = listOf(0f to 155f, 40f to 125f, 90f to 90f, 145f to 50f, 215f to 0f, 270f to 0f, 230f to 45f, 160f to 95f, 75f to 145f)),
            p("crevasse_centrale_haut", TerrainKind.TOWER_SPIRE, 743f, 82f, 110f, 165f, TerrainSurface.IMPASSABLE, 0, false),
            p("crete_croissant_cg", TerrainKind.TOWER_SPIRE, 502f, 198f, 205f, 195f, TerrainSurface.IMPASSABLE, 1, true, outline = listOf(590f to 95f, 640f to 120f, 645f to 160f, 635f to 230f, 610f to 290f, 500f to 298f, 395f to 285f, 420f to 215f, 485f to 155f, 540f to 120f)),
            p("crete_glace_hd", TerrainKind.TOWER_SPIRE, 1209f, 90f, 398f, 180f, TerrainSurface.IMPASSABLE, 1, true, outline = listOf(1015f to 0f, 1408f to 0f, 1408f to 300f, 1340f to 290f, 1290f to 240f, 1240f to 195f, 1180f to 145f, 1110f to 95f, 1050f to 45f)),
            p("crete_c_cd", TerrainKind.TOWER_SPIRE, 905f, 515f, 240f, 210f, TerrainSurface.IMPASSABLE, 1, true, outline = listOf(1005f to 412f, 1050f to 435f, 1030f to 490f, 940f to 545f, 890f to 685f, 830f to 700f, 770f to 650f, 760f to 560f, 800f to 470f, 900f to 425f)),
            p("crevasse_bas_centre", TerrainKind.TOWER_SPIRE, 648f, 638f, 116f, 132f, TerrainSurface.IMPASSABLE, 0, false),
            p("crevasse_bas_droite", TerrainKind.TOWER_SPIRE, 1294f, 640f, 228f, 129f, TerrainSurface.IMPASSABLE, 0, false, outline = listOf(1180f to 704f, 1220f to 650f, 1270f to 610f, 1330f to 580f, 1408f to 576f, 1408f to 620f, 1350f to 645f, 1290f to 680f, 1230f to 704f)),
            p("tranchee_acier_hg", TerrainKind.TOWER_SPIRE, 215f, 142f, 430f, 285f, TerrainSurface.OPEN, 1, false),
            p("tranchee_acier_centre_gauche", TerrainKind.TOWER_SPIRE, 367f, 464f, 118f, 117f, TerrainSurface.OPEN, 1, false),
            p("tranchee_acier_bd", TerrainKind.TOWER_SPIRE, 1196f, 567f, 425f, 274f, TerrainSurface.OPEN, 1, false),
            p("bunker_dome_bg", TerrainKind.BUNKER, 220f, 551f, 179f, 154f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(310f to 551f, 284f to 605f, 220f to 628f, 156f to 605f, 130f to 551f, 156f to 497f, 220f to 474f, 284f to 497f)),
            p("hangar_hd", TerrainKind.BUNKER, 1216f, 161f, 155f, 142f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(1140f to 95f, 1175f to 90f, 1290f to 178f, 1294f to 210f, 1270f to 230f, 1210f to 180f, 1150f to 132f)),
            p("epave_centrale", TerrainKind.WALL, 604f, 452f, 139f, 115f, TerrainSurface.IMPASSABLE, 1, true, outline = listOf(545f to 430f, 575f to 400f, 630f to 398f, 670f to 425f, 672f to 470f, 640f to 505f, 585f to 510f, 545f to 485f)),
            p("tente_centre_haut", TerrainKind.BUNKER, 766f, 278f, 77f, 75f, TerrainSurface.IMPASSABLE, 1, true),
            p("barricade_centre_gauche", TerrainKind.WALL, 250f, 331f, 99f, 58f, TerrainSurface.OPEN, 1, false),
            p("reservoir_haut_gauche", TerrainKind.PYLON, 334f, 75f, 38f, 40f, TerrainSurface.OPEN, 1, false),
            p("reservoir_gauche", TerrainKind.PYLON, 126f, 211f, 45f, 46f, TerrainSurface.OPEN, 1, false),
            p("pylone_antennes_hcd", TerrainKind.PYLON, 898f, 132f, 55f, 35f, TerrainSurface.OPEN, 1, false),
            p("capteur_hd", TerrainKind.PYLON, 1093f, 170f, 36f, 50f, TerrainSurface.OPEN, 1, false),
            p("pylone_treillis_bg", TerrainKind.PYLON, 443f, 606f, 48f, 62f, TerrainSurface.OPEN, 1, false),
            p("reservoir_bas_droite", TerrainKind.PYLON, 1281f, 490f, 52f, 61f, TerrainSurface.OPEN, 1, false),
            p("reservoir_bas_droite_2", TerrainKind.PYLON, 1090f, 618f, 50f, 65f, TerrainSurface.OPEN, 1, false),
            p("caisses_haut_gauche", TerrainKind.WALL, 466f, 104f, 43f, 58f, TerrainSurface.OPEN, 1, false),
            p("caisses_centre", TerrainKind.WALL, 720f, 277f, 53f, 54f, TerrainSurface.OPEN, 1, false),
            p("caisses_bas_gauche", TerrainKind.WALL, 92f, 474f, 49f, 47f, TerrainSurface.OPEN, 1, false),
        )
    )

    val ENDOR = BattleMat(
        "battlemat_endor", "battlemat_endor", "battlemats/endor.jpg", BattleMatPalette(),
        listOf(
            p("bunker", TerrainKind.BUNKER, 833f, 167f, 290f, 266f, TerrainSurface.IMPASSABLE, 2, true),
            p("atst", TerrainKind.WALL, 404f, 475f, 177f, 146f, TerrainSurface.IMPASSABLE, 2, true),
            p("plateforme_c", TerrainKind.ROTUNDA, 1088f, 260f, 177f, 180f, TerrainSurface.OPEN, 0, false),
            p("plateforme_g_haut", TerrainKind.ROTUNDA, 902f, 535f, 162f, 50f, TerrainSurface.OPEN, 0, false),
            p("plateforme_g_gauche", TerrainKind.ROTUNDA, 852f, 624f, 36f, 129f, TerrainSurface.OPEN, 0, false),
            p("plateforme_d_gauche", TerrainKind.ROTUNDA, 1183f, 518f, 38f, 180f, TerrainSurface.OPEN, 0, false),
            p("plateforme_d_haut", TerrainKind.ROTUNDA, 1256f, 450f, 104f, 44f, TerrainSurface.OPEN, 0, false),
            p("plateforme_d_bas", TerrainKind.ROTUNDA, 1231f, 596f, 54f, 24f, TerrainSurface.OPEN, 0, false),
            p("sequoia_01", TerrainKind.TREE_PLATFORM, 140f, 140f, 90f, 80f, TerrainSurface.IMPASSABLE, 2, true),
            p("sequoia_02", TerrainKind.TREE_PLATFORM, 285f, 29f, 74f, 58f, TerrainSurface.IMPASSABLE, 2, true),
            p("sequoia_03", TerrainKind.TREE_PLATFORM, 500f, 68f, 114f, 105f, TerrainSurface.IMPASSABLE, 2, true),
            p("sequoia_04", TerrainKind.TREE_PLATFORM, 1030f, 58f, 101f, 96f, TerrainSurface.IMPASSABLE, 2, true),
            p("sequoia_05", TerrainKind.TREE_PLATFORM, 796f, 673f, 72f, 62f, TerrainSurface.IMPASSABLE, 2, true),
            p("tronc_01", TerrainKind.WALL, 305f, 160f, 234f, 156f, TerrainSurface.DIFFICULT, 1, false),
            p("tronc_02", TerrainKind.WALL, 680f, 474f, 172f, 144f, TerrainSurface.DIFFICULT, 1, false),
            p("tronc_03", TerrainKind.WALL, 238f, 550f, 148f, 215f, TerrainSurface.DIFFICULT, 1, false),
            p("tronc_08", TerrainKind.WALL, 1306f, 528f, 204f, 112f, TerrainSurface.DIFFICULT, 1, false),
            p("tronc_04", TerrainKind.WALL, 67f, 507f, 134f, 94f, TerrainSurface.IMPASSABLE, 2, true),
            p("tronc_05", TerrainKind.WALL, 80f, 632f, 160f, 144f, TerrainSurface.IMPASSABLE, 2, true),
            p("tronc_06", TerrainKind.WALL, 490f, 650f, 130f, 109f, TerrainSurface.IMPASSABLE, 2, true),
            p("tronc_07", TerrainKind.WALL, 964f, 632f, 184f, 144f, TerrainSurface.IMPASSABLE, 2, true),
            p("tronc_09", TerrainKind.WALL, 1334f, 646f, 148f, 116f, TerrainSurface.IMPASSABLE, 2, true),
        )
    )

    val SCARIF = BattleMat(
        "battlemat_scarif", "battlemat_scarif", "battlemats/scarif.jpg", BattleMatPalette(),
        listOf(
            p("epave_nord", TerrainKind.WALL, 202f, 145f, 113f, 130f, TerrainSurface.IMPASSABLE, 2, true),
            p("epave_sud", TerrainKind.WALL, 980f, 596f, 141f, 119f, TerrainSurface.IMPASSABLE, 2, true),
            p("bunker_nord", TerrainKind.BUNKER, 934f, 128f, 132f, 133f, TerrainSurface.IMPASSABLE, 2, true),
            p("complexe_radar", TerrainKind.BUNKER, 1198f, 180f, 315f, 230f, TerrainSurface.IMPASSABLE, 2, true),
            p("bunker_sud", TerrainKind.BUNKER, 868f, 484f, 67f, 48f, TerrainSurface.IMPASSABLE, 2, true),
            p("complexe_est", TerrainKind.BUNKER, 1198f, 530f, 153f, 150f, TerrainSurface.IMPASSABLE, 2, true),
            p("plateforme", TerrainKind.ROTUNDA, 256f, 500f, 277f, 270f, TerrainSurface.OPEN, 0, false),
            p("pont_nord", TerrainKind.WALL, 602f, 242f, 137f, 117f, TerrainSurface.OPEN, 0, false),
            p("pont_central", TerrainKind.WALL, 725f, 496f, 144f, 119f, TerrainSurface.OPEN, 0, false),
            p("ponton_diag", TerrainKind.WALL, 592f, 410f, 45f, 120f, TerrainSurface.DIFFICULT, 1, true),
            p("ponton_nord", TerrainKind.WALL, 556f, 90f, 82f, 70f, TerrainSurface.DIFFICULT, 1, false),
            p("ponton_sud", TerrainKind.WALL, 782f, 635f, 43f, 44f, TerrainSurface.DIFFICULT, 1, false),
            p("barricade_01", TerrainKind.WALL, 305f, 118f, 90f, 29f, TerrainSurface.IMPASSABLE, 2, true),
            p("barricade_02", TerrainKind.WALL, 164f, 236f, 88f, 35f, TerrainSurface.OPEN, 1, false),
            p("barricade_03", TerrainKind.WALL, 336f, 215f, 79f, 40f, TerrainSurface.OPEN, 1, false),
            p("barricade_04", TerrainKind.WALL, 786f, 275f, 161f, 184f, TerrainSurface.IMPASSABLE, 2, true),
            p("barricade_05", TerrainKind.WALL, 488f, 352f, 35f, 65f, TerrainSurface.OPEN, 1, false),
            p("barricade_06", TerrainKind.WALL, 804f, 380f, 25f, 24f, TerrainSurface.OPEN, 1, false),
            p("barricade_07", TerrainKind.WALL, 1067f, 482f, 62f, 99f, TerrainSurface.OPEN, 1, false),
            p("barricade_08", TerrainKind.WALL, 1174f, 401f, 72f, 58f, TerrainSurface.OPEN, 1, false),
            p("lagune_ouest", TerrainKind.FOUNTAIN, 400f, 360f, 229f, 210f, TerrainSurface.DIFFICULT, 0, false),
            p("lagune_centrale", TerrainKind.FOUNTAIN, 652f, 365f, 275f, 250f, TerrainSurface.DIFFICULT, 0, false),
        )
    )

    val NABOO = BattleMat(
        "battlemat_naboo", "battlemat_naboo", "battlemats/naboo.jpg", BattleMatPalette(),
        listOf(
            p("canal_nord_gauche", TerrainKind.FOUNTAIN, 478f, 160f, 100f, 320f, TerrainSurface.IMPASSABLE, 0, false, outline = listOf(524f to 0f, 524f to 90f, 518f to 150f, 514f to 210f, 512f to 260f, 516f to 320f, 446f to 320f, 434f to 260f, 430f to 200f, 436f to 140f, 440f to 70f, 442f to 0f)),
            p("canal_nord_droit_haut", TerrainKind.FOUNTAIN, 925f, 50f, 70f, 100f, TerrainSurface.IMPASSABLE, 0, false),
            p("canal_nord_droit_bas", TerrainKind.FOUNTAIN, 928f, 240f, 75f, 160f, TerrainSurface.IMPASSABLE, 0, false, outline = listOf(962f to 160f, 962f to 220f, 958f to 270f, 960f to 320f, 896f to 320f, 892f to 270f, 894f to 210f, 892f to 160f)),
            p("canal_milieu_gauche", TerrainKind.FOUNTAIN, 480f, 479f, 100f, 138f, TerrainSurface.IMPASSABLE, 0, false),
            p("canal_milieu_droit", TerrainKind.FOUNTAIN, 950f, 483f, 140f, 134f, TerrainSurface.IMPASSABLE, 0, false, outline = listOf(1016f to 420f, 1016f to 480f, 1012f to 548f, 894f to 550f, 884f to 500f, 890f to 450f, 898f to 416f)),
            p("canal_sud_gauche", TerrainKind.FOUNTAIN, 488f, 657f, 85f, 94f, TerrainSurface.IMPASSABLE, 0, false, outline = listOf(528f to 610f, 528f to 660f, 526f to 704f, 448f to 704f, 446f to 660f, 446f to 610f)),
            p("canal_sud_droit", TerrainKind.FOUNTAIN, 990f, 657f, 70f, 94f, TerrainSurface.IMPASSABLE, 0, false),
            p("dome_gauche_haut", TerrainKind.BUNKER, 25f, 236f, 50f, 67f, TerrainSurface.IMPASSABLE, 2, true),
            p("dome_gauche_central", TerrainKind.BUNKER, 45f, 352f, 90f, 171f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(78f to 267f, 90f to 285f, 90f to 420f, 78f to 438f, 10f to 438f, 0f to 420f, 0f to 285f, 10f to 267f)),
            p("dome_gauche_bas", TerrainKind.BUNKER, 26f, 466f, 52f, 65f, TerrainSurface.IMPASSABLE, 2, true),
            p("dome_droit_haut", TerrainKind.BUNKER, 1354f, 236f, 107f, 77f, TerrainSurface.IMPASSABLE, 2, true),
            p("dome_droit_central", TerrainKind.BUNKER, 1362f, 352f, 92f, 168f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(1398f to 268f, 1408f to 286f, 1408f to 418f, 1398f to 436f, 1326f to 436f, 1316f to 418f, 1316f to 286f, 1326f to 268f)),
            p("dome_droit_bas", TerrainKind.BUNKER, 1358f, 468f, 99f, 80f, TerrainSurface.IMPASSABLE, 2, true),
            p("dome_central_haut", TerrainKind.BUNKER, 708f, 18f, 55f, 35f, TerrainSurface.IMPASSABLE, 2, true),
            p("parterre_nord_gauche", TerrainKind.WALL, 622f, 228f, 91f, 125f, TerrainSurface.DIFFICULT, 1, false),
            p("parterre_nord_droit", TerrainKind.WALL, 786f, 228f, 92f, 125f, TerrainSurface.DIFFICULT, 1, false),
            p("parterre_median_gauche", TerrainKind.WALL, 624f, 486f, 93f, 132f, TerrainSurface.DIFFICULT, 1, false),
            p("parterre_median_droit", TerrainKind.WALL, 788f, 486f, 96f, 132f, TerrainSurface.DIFFICULT, 1, false),
            p("parterre_sud_gauche", TerrainKind.WALL, 622f, 652f, 91f, 104f, TerrainSurface.DIFFICULT, 1, false),
            p("parterre_sud_droit", TerrainKind.WALL, 786f, 652f, 92f, 104f, TerrainSurface.DIFFICULT, 1, false),
            p("fontaine_octogonale", TerrainKind.FOUNTAIN, 706f, 173f, 57f, 54f, TerrainSurface.OPEN, 0, false),
            p("fontaine_etoile", TerrainKind.FOUNTAIN, 704f, 576f, 95f, 98f, TerrainSurface.OPEN, 0, false),
            p("batiment_angle_hg", TerrainKind.BUNKER, 48f, 45f, 95f, 90f, TerrainSurface.IMPASSABLE, 2, true),
            p("batiment_haut_gauche", TerrainKind.BUNKER, 190f, 35f, 150f, 70f, TerrainSurface.IMPASSABLE, 2, true),
            p("batiment_haut_330", TerrainKind.BUNKER, 365f, 25f, 70f, 50f, TerrainSurface.IMPASSABLE, 2, true),
            p("batiment_haut_580", TerrainKind.BUNKER, 622f, 50f, 85f, 100f, TerrainSurface.IMPASSABLE, 2, true),
            p("batiment_haut_740", TerrainKind.BUNKER, 784f, 50f, 88f, 100f, TerrainSurface.IMPASSABLE, 2, true),
            p("pavillon_haut_droit", TerrainKind.WALL, 1030f, 36f, 70f, 72f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(1050f to 0f, 1065f to 18f, 1065f to 54f, 1050f to 72f, 1010f to 72f, 995f to 54f, 995f to 18f, 1010f to 0f)),
            p("batiment_haut_droit", TerrainKind.BUNKER, 1198f, 38f, 135f, 75f, TerrainSurface.IMPASSABLE, 2, true),
            p("batiment_angle_hd", TerrainKind.BUNKER, 1339f, 35f, 138f, 70f, TerrainSurface.IMPASSABLE, 2, true),
            p("colonnade_gauche", TerrainKind.WALL, 125f, 352f, 60f, 155f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(150f to 275f, 155f to 300f, 155f to 405f, 150f to 429f, 98f to 429f, 95f to 405f, 95f to 300f, 98f to 275f)),
            p("colonnade_droite", TerrainKind.WALL, 1282f, 352f, 55f, 145f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(1305f to 280f, 1310f to 305f, 1310f to 400f, 1305f to 424f, 1259f to 424f, 1255f to 400f, 1255f to 305f, 1259f to 280f)),
            p("batiment_angle_bg", TerrainKind.BUNKER, 72f, 647f, 145f, 114f, TerrainSurface.IMPASSABLE, 2, true),
            p("batiment_bas_gauche", TerrainKind.BUNKER, 248f, 667f, 165f, 74f, TerrainSurface.IMPASSABLE, 2, true),
            p("pavillon_bas_centre", TerrainKind.WALL, 912f, 650f, 65f, 70f, TerrainSurface.IMPASSABLE, 2, true, outline = listOf(930f to 615f, 944f to 632f, 944f to 668f, 930f to 685f, 894f to 685f, 880f to 668f, 880f to 632f, 894f to 615f)),
            p("batiment_bas_droit", TerrainKind.BUNKER, 1148f, 660f, 195f, 89f, TerrainSurface.IMPASSABLE, 2, true),
            p("batiment_angle_bd", TerrainKind.BUNKER, 1339f, 650f, 138f, 109f, TerrainSurface.IMPASSABLE, 2, true),
        )
    )

    val GEONOSIS = BattleMat(
        "battlemat_geonosis", "battlemat_geonosis", "battlemats/geonosis.jpg", BattleMatPalette(),
        listOf(
            p("ruche_no", TerrainKind.WALL, 110f, 110f, 180f, 170f, TerrainSurface.IMPASSABLE, 2, true),
            p("ruche_ne", TerrainKind.WALL, 1310f, 102f, 170f, 155f, TerrainSurface.IMPASSABLE, 2, true),
            p("ruche_nc", TerrainKind.WALL, 610f, 81f, 65f, 72f, TerrainSurface.IMPASSABLE, 2, true),
            p("ruche_ng", TerrainKind.WALL, 222f, 42f, 33f, 45f, TerrainSurface.IMPASSABLE, 2, true),
            p("ruche_nep", TerrainKind.WALL, 1174f, 70f, 43f, 50f, TerrainSurface.IMPASSABLE, 2, true),
            p("ruche_o", TerrainKind.WALL, 68f, 224f, 47f, 47f, TerrainSurface.IMPASSABLE, 2, true),
            p("ruche_e", TerrainKind.WALL, 1342f, 222f, 47f, 47f, TerrainSurface.IMPASSABLE, 2, true),
            p("ruche_sc", TerrainKind.WALL, 765f, 650f, 52f, 56f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_no", TerrainKind.TOWER_SPIRE, 150f, 165f, 300f, 330f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_nc", TerrainKind.TOWER_SPIRE, 708f, 132f, 455f, 265f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_ne", TerrainKind.TOWER_SPIRE, 1234f, 165f, 348f, 330f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_so", TerrainKind.TOWER_SPIRE, 185f, 572f, 370f, 264f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_sc", TerrainKind.TOWER_SPIRE, 695f, 572f, 410f, 264f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_se", TerrainKind.TOWER_SPIRE, 1249f, 572f, 318f, 264f, TerrainSurface.IMPASSABLE, 2, true),
            p("mesa_cg", TerrainKind.TOWER_SPIRE, 362f, 478f, 117f, 97f, TerrainSurface.IMPASSABLE, 1, true),
            p("mesa_cd", TerrainKind.TOWER_SPIRE, 956f, 356f, 169f, 129f, TerrainSurface.IMPASSABLE, 1, true),
            p("laat_centre", TerrainKind.WALL, 508f, 372f, 135f, 145f, TerrainSurface.IMPASSABLE, 1, true),
            p("laat_se", TerrainKind.WALL, 988f, 580f, 115f, 110f, TerrainSurface.IMPASSABLE, 1, true),
            p("canon_nc", TerrainKind.PYLON, 608f, 180f, 55f, 60f, TerrainSurface.OPEN, 0, false),
            p("canon_ne", TerrainKind.PYLON, 1178f, 230f, 65f, 60f, TerrainSurface.OPEN, 0, false),
            p("canon_sg", TerrainKind.PYLON, 668f, 580f, 55f, 70f, TerrainSurface.OPEN, 0, false),
            p("canon_sd", TerrainKind.PYLON, 818f, 525f, 55f, 70f, TerrainSurface.OPEN, 0, false),
            p("cratere_ne", TerrainKind.FOUNTAIN, 984f, 225f, 68f, 66f, TerrainSurface.DIFFICULT, 0, false),
            p("cratere_so", TerrainKind.FOUNTAIN, 225f, 654f, 70f, 73f, TerrainSurface.DIFFICULT, 0, false),
            p("cratere_c", TerrainKind.FOUNTAIN, 804f, 354f, 35f, 33f, TerrainSurface.DIFFICULT, 0, false),
            p("cratere_se", TerrainKind.FOUNTAIN, 1216f, 662f, 32f, 33f, TerrainSurface.DIFFICULT, 0, false),
        )
    )

    val ALL = listOf(NONE, TATOOINE, HOTH, ENDOR, SCARIF, NABOO, GEONOSIS)
}
