package com.legionforge.app.domain

/**
 * Géométrie des Map Cards Legion v2 — TRANSCRIPTION de données imprimées, PAS une règle inventée.
 *
 * Règle (rulebook AMG DOC56) :
 * - La Map Card définit les Player Territories (391-395), les POI (400-401, 1287-1310)
 *   et le placement sur une grille où 1 case = 1 range = 6" (417-423, 422).
 * - Chaque Objectif est apparié à UNE Map Card par titre identique (1181-1185).
 * - Le rulebook ne donne AUCUNE position normalisée : les zones/POI sont propres à
 *   chaque carte. Elles sont transcrites depuis les images des cartes, jamais devinées.
 * - Blue (déterminé par 4 dés noirs, 1221-1227) choisit son long bord (1244-1248) :
 *   le territoire adjacent est ALLIÉ, l'autre ENNEMI. L'orientation est donc un
 *   miroir/rotation 0°/180° appliqué à la transcription (voir [oriented]).
 *
 * Repère grille : coin HAUT-GAUCHE, colonne c ∈ [0, gridCols[, ligne r ∈ [0, gridRows[.
 * Plateau : 6'×3' = 1828,8 × 914,4 mm, centré (x ∈ [-914,4 ; 914,4]).
 * 1 case = 6" = 152,4 mm. La grille 12×6 couvre 1828,8 × 914,4 mm = le plateau exact.
 *
 * IMPORTANT : ce repère grille (case → mm) est DIFFÉRENT du mapping image→mm de
 * BattleMats.kt. Ne PAS réutiliser imgX/imgY (décalage ~164 mm, unités posées dans l'eau).
 */
/** Objet décoré posé sur un POI, transcription thématique (asset dans le dossier legion_poi). */
enum class PoiObject(val asset: String) {
    SHIELD_GENERATOR("legion_poi/shield_generator.png"),
    POWER_GENERATOR("legion_poi/power_generator.png"),
    COMMS_TOWER("legion_poi/comms_tower.png"),
    BUNKER_STATION("legion_poi/bunker_station.png"),
    RHYDONIUM_CANISTER("legion_poi/rhydonium_canister.png"),
    SUPPLY_CRATE("legion_poi/supply_crate.png"),
    CARGO_CUBES("legion_poi/cargo_cubes.png"),
    CONTROL_TERMINAL("legion_poi/control_terminal.png"),
    TRANSCEIVER("legion_poi/transceiver.png"),
    FUEL_CANISTER("legion_poi/fuel_canister.png"),
}

data class LegionMapLayout(
    val name: String,
    val gridCols: Int = 12,
    val gridRows: Int = 6,
    /**
     * Objets DÉCORATIFS à disposer sur les POI (un par point, rotation si plus de
     * POI que d'objets). Vide = anneaux génériques. Le type/nombre réel vient de
     * l'Objective Card — ces objets sont un repère visuel, pas une donnée de règle.
     */
    val poiObjects: List<PoiObject> = emptyList(),
    /** Rectangles de territoire allié, en cases [c0,r0,c1,r1] (c1/r1 inclus). */
    val allied: List<IntArray> = emptyList(),
    /** Rectangles de territoire ennemi, en cases [c0,r0,c1,r1]. */
    val enemy: List<IntArray> = emptyList(),
    /**
     * Points d'intérêt, en INTERSECTION de grille [c, r] avec c ∈ [0, gridCols], r ∈ [0, gridRows].
     * Les anneaux de POI des Map Cards sont sur les CROISEMENTS des lignes (vérifié sur l'incart),
     * PAS au centre des cases → convertir avec caseEdge(), jamais caseCenter().
     */
    val poi: List<IntArray> = emptyList(),
    /** Vrai si la carte a pu être transcrite depuis une image lisible. */
    val transcribed: Boolean = false,
) {
    val caseMm: Float get() = 6f * LegionTabletopGeometry.MM_PER_INCH // 152,4 mm

    /** Bord supérieur-gauche d'une case en mm plateau (indice entier, 0..cols). */
    fun caseEdge(c: Float, r: Float): Pair<Float, Float> {
        val x = (c - gridCols / 2f) * caseMm
        val y = (r - gridRows / 2f) * caseMm
        return x to y
    }

    /** Centre d'une case en mm plateau. */
    fun caseCenter(c: Float, r: Float): Pair<Float, Float> {
        val e = caseEdge(c, r)
        return (e.first + caseMm / 2f) to (e.second + caseMm / 2f)
    }

    /** Objet décoratif pour le POI d'index [i] : rotation sur la liste fournie. */
    fun poiObjectAt(i: Int): PoiObject? =
        if (poiObjects.isEmpty()) null else poiObjects[i % poiObjects.size]

    /**
     * Applique le choix du bord de Blue (rulebook 1244-1248).
     * [blueAtBottom] = true : transcription d'origine (allié en bas) ;
     * false : miroir vertical (allié en haut) — Blue a choisi le bord haut.
     * Le joueur ALLIÉ est toujours celui de Blue ; on ne fige pas « haut=ennemi ».
     */
    fun oriented(blueAtBottom: Boolean): LegionMapLayout {
        if (blueAtBottom) return this
        fun flip(rect: IntArray) = intArrayOf(rect[0], gridRows - 1 - rect[3], rect[2], gridRows - 1 - rect[1])
        // POI = intersections (0..rows) → miroir rows-r ; cases (0..rows-1) → rows-1-r.
        fun flipPt(pt: IntArray) = intArrayOf(pt[0], gridRows - pt[1])
        return copy(allied = allied.map(::flip), enemy = enemy.map(::flip), poi = poi.map(::flipPt))
    }
}

object LegionMapLayouts {
    // Transcriptions des 10 Map Cards v2 (rulebook 1181-1185) depuis les images
    // annotées / upscalées. CHAQUE carte a sa propre grille et ses propres zones :
    // le rulebook ne normalise aucune position (417-423). 1 case = 1 range = 6".

    val RECOVER_THE_RESEARCH = LegionMapLayout(
        name = "Recover the Research",
        gridCols = 12, gridRows = 6,
        allied = listOf(intArrayOf(3,5,11,5)),
        enemy = listOf(intArrayOf(0,0,8,0)),
        poi = listOf(intArrayOf(2,2), intArrayOf(5,2), intArrayOf(9,2), intArrayOf(3,4), intArrayOf(7,4), intArrayOf(10,4)),
        transcribed = true,
        poiObjects = listOf(PoiObject.CONTROL_TERMINAL, PoiObject.COMMS_TOWER, PoiObject.SHIELD_GENERATOR, PoiObject.POWER_GENERATOR, PoiObject.TRANSCEIVER, PoiObject.SUPPLY_CRATE),
    )

    val INTERCEPT_SIGNALS = LegionMapLayout(
        name = "Intercept Signals",
        gridCols = 6, gridRows = 6,
        allied = listOf(intArrayOf(1,4,4,5)),
        enemy = listOf(intArrayOf(1,0,4,1)),
        poi = listOf(intArrayOf(3,1), intArrayOf(1,3), intArrayOf(5,3), intArrayOf(3,5)),
        transcribed = true,
        poiObjects = listOf(PoiObject.COMMS_TOWER, PoiObject.TRANSCEIVER, PoiObject.CONTROL_TERMINAL, PoiObject.POWER_GENERATOR),
    )

    val BREAKTHROUGH = LegionMapLayout(
        name = "Breakthrough",
        gridCols = 12, gridRows = 6,
        allied = listOf(intArrayOf(3,4,11,5), intArrayOf(10,2,11,3)),
        enemy = listOf(intArrayOf(0,0,8,1), intArrayOf(0,2,1,3)),
        poi = listOf(intArrayOf(8,1), intArrayOf(1,3), intArrayOf(11,3), intArrayOf(4,5)),
        transcribed = true,
        poiObjects = listOf(PoiObject.BUNKER_STATION, PoiObject.SHIELD_GENERATOR, PoiObject.SUPPLY_CRATE, PoiObject.CARGO_CUBES, PoiObject.CONTROL_TERMINAL, PoiObject.POWER_GENERATOR),
    )

    val BUNKER_ASSAULT = LegionMapLayout(
        name = "Bunker Assault",
        gridCols = 6, gridRows = 6,
        allied = listOf(intArrayOf(1,4,4,5)),
        enemy = listOf(intArrayOf(1,0,4,1)),
        poi = listOf(intArrayOf(1,2), intArrayOf(5,2), intArrayOf(1,4), intArrayOf(5,4)),
        transcribed = true,
        poiObjects = listOf(PoiObject.BUNKER_STATION, PoiObject.SHIELD_GENERATOR, PoiObject.SUPPLY_CRATE, PoiObject.CARGO_CUBES, PoiObject.CONTROL_TERMINAL, PoiObject.POWER_GENERATOR),
    )

    val CLOSE_THE_POCKET = LegionMapLayout(
        name = "Close the Pocket",
        gridCols = 6, gridRows = 6,
        allied = listOf(intArrayOf(0,4,1,5), intArrayOf(4,4,5,5)),
        enemy = listOf(intArrayOf(0,0,1,1), intArrayOf(4,0,5,1)),
        poi = listOf(intArrayOf(1,3), intArrayOf(3,3), intArrayOf(5,3)),
        transcribed = true,
        poiObjects = listOf(PoiObject.SUPPLY_CRATE, PoiObject.CARGO_CUBES, PoiObject.FUEL_CANISTER, PoiObject.RHYDONIUM_CANISTER),
    )

    val CONTACT_CONTACT = LegionMapLayout(
        name = "Contact, Contact!",
        gridCols = 12, gridRows = 6,
        allied = listOf(intArrayOf(0,4,5,5), intArrayOf(1,2,2,3)),
        enemy = listOf(intArrayOf(6,0,11,1), intArrayOf(9,2,10,3)),
        poi = listOf(intArrayOf(3,1), intArrayOf(6,1), intArrayOf(6,3), intArrayOf(6,5), intArrayOf(9,5)),
        transcribed = true,
        poiObjects = listOf(PoiObject.TRANSCEIVER, PoiObject.COMMS_TOWER, PoiObject.CONTROL_TERMINAL, PoiObject.SHIELD_GENERATOR, PoiObject.POWER_GENERATOR),
    )

    val PAYLOAD = LegionMapLayout(
        name = "Payload",
        gridCols = 12, gridRows = 6,
        allied = listOf(intArrayOf(0,4,11,5)),
        enemy = listOf(intArrayOf(0,0,11,1)),
        poi = listOf(intArrayOf(9,2), intArrayOf(6,3), intArrayOf(2,4)),
        transcribed = true,
        poiObjects = listOf(PoiObject.CARGO_CUBES, PoiObject.SUPPLY_CRATE, PoiObject.FUEL_CANISTER, PoiObject.BUNKER_STATION),
    )

    val CAULDRON = LegionMapLayout(
        name = "Cauldron",
        gridCols = 12, gridRows = 6,
        allied = listOf(intArrayOf(0,4,11,5)),
        enemy = listOf(intArrayOf(0,0,11,1)),
        poi = listOf(intArrayOf(3,1), intArrayOf(9,1), intArrayOf(4,3), intArrayOf(7,3), intArrayOf(3,4), intArrayOf(9,4)),
        transcribed = true,
        poiObjects = listOf(PoiObject.RHYDONIUM_CANISTER, PoiObject.FUEL_CANISTER, PoiObject.CARGO_CUBES, PoiObject.CONTROL_TERMINAL, PoiObject.POWER_GENERATOR, PoiObject.SUPPLY_CRATE),
    )

    val OUTFLANK = LegionMapLayout(
        name = "Outflank",
        gridCols = 12, gridRows = 6,
        allied = listOf(intArrayOf(0,2,0,5), intArrayOf(3,4,8,5)),
        enemy = listOf(intArrayOf(3,0,8,1), intArrayOf(11,0,11,3)),
        poi = listOf(intArrayOf(2,2), intArrayOf(6,2), intArrayOf(6,4), intArrayOf(10,4)),
        transcribed = true,
        poiObjects = listOf(PoiObject.FUEL_CANISTER, PoiObject.RHYDONIUM_CANISTER, PoiObject.SUPPLY_CRATE, PoiObject.CARGO_CUBES),
    )

    val SHIFTING_PRIORITIES = LegionMapLayout(
        name = "Shifting Priorities",
        gridCols = 12, gridRows = 6,
        allied = listOf(intArrayOf(3,4,11,5)),
        enemy = listOf(intArrayOf(0,0,8,1)),
        poi = listOf(intArrayOf(1,4), intArrayOf(3,3), intArrayOf(6,3), intArrayOf(9,3), intArrayOf(11,2)),
        transcribed = true,
        poiObjects = listOf(PoiObject.POWER_GENERATOR, PoiObject.SHIELD_GENERATOR, PoiObject.CONTROL_TERMINAL, PoiObject.COMMS_TOWER, PoiObject.TRANSCEIVER),
    )

    /** Résolution par nom de Map Card. Repli : null (jamais de bandes 12" inventées). */
    fun forName(name: String?): LegionMapLayout? = when (name) {
        RECOVER_THE_RESEARCH.name -> RECOVER_THE_RESEARCH
        INTERCEPT_SIGNALS.name -> INTERCEPT_SIGNALS
        BREAKTHROUGH.name -> BREAKTHROUGH
        BUNKER_ASSAULT.name -> BUNKER_ASSAULT
        CLOSE_THE_POCKET.name -> CLOSE_THE_POCKET
        CONTACT_CONTACT.name -> CONTACT_CONTACT
        PAYLOAD.name -> PAYLOAD
        CAULDRON.name -> CAULDRON
        OUTFLANK.name -> OUTFLANK
        SHIFTING_PRIORITIES.name -> SHIFTING_PRIORITIES
        else -> null
    }

    val NAMES = listOf(
        "Recover the Research", "Intercept Signals", "Breakthrough", "Bunker Assault",
        "Close the Pocket", "Contact, Contact!", "Payload", "Cauldron", "Outflank",
        "Shifting Priorities",
    )
}
