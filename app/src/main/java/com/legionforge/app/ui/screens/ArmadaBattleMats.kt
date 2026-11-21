package com.legionforge.app.ui.screens

/**
 * Battle mats Armada : VRAIES images (Gemini 1408x704, ratio 2:1) comme FOND.
 *
 * DÉCISION Yann 2026-11-21 : fonds COSMÉTIQUES seulement + vraies règles Armada
 * (zones de déploiement 857+, portées de dés 983+). ZÉRO terrain inventé :
 * Armada n'a pas d'astéroïdes/nébuleuses officielles dans le RRG, on ne génère
 * aucune règle de terrain maison. Les images ne changent AUCUNE mécanique.
 *
 * Le plateau Armada est 1800x900 mm (ratio 2:1) : l'image 2:1 tombe exactement,
 * pas de bandes noires (contrairement au carré 36" Legion).
 */
data class ArmadaBattleMat(
    val id: String,
    val nameKey: String,
    val assetPath: String,
)

object ArmadaBattleMats {
    val NONE = ArmadaBattleMat("none", "armada_battlemat_none", "")
    val SCARIF = ArmadaBattleMat("armada_scarif", "armada_battlemat_scarif", "battlemats/armada/scarif.jpg")
    val HOTH = ArmadaBattleMat("armada_hoth", "armada_battlemat_hoth", "battlemats/armada/hoth.jpg")
    val KALIIDA = ArmadaBattleMat("armada_kaliida", "armada_battlemat_kaliida", "battlemats/armada/kaliida.jpg")
    val CORUSCANT = ArmadaBattleMat("armada_coruscant", "armada_battlemat_coruscant", "battlemats/armada/coruscant.jpg")

    val ALL = listOf(SCARIF, HOTH, KALIIDA, CORUSCANT)
}
