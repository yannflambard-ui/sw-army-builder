package com.legionforge.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import com.legionforge.app.data.model.ListEntry
import com.legionforge.app.data.model.ArmadaSlot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.legionforge.app.R
import com.legionforge.app.data.model.GameSystem
import com.legionforge.app.data.model.CardKind
import com.legionforge.app.ui.screens.ArmyBuilderScreen
import com.legionforge.app.ui.screens.CardDetailScreen
import com.legionforge.app.ui.screens.CollectionScreen
import com.legionforge.app.ui.screens.FactionPickerScreen
import com.legionforge.app.ui.screens.HomeScreen
import com.legionforge.app.ui.screens.SearchScreen
import com.legionforge.app.ui.screens.SettingsScreen
import com.legionforge.app.ui.screens.ShareScreen
import com.legionforge.app.ui.screens.WikiScreen
import com.legionforge.app.ui.viewmodel.ArmyBuilderViewModel
import com.legionforge.app.ui.viewmodel.ShareListPayload
import com.legionforge.app.ui.viewmodel.PlaySyncViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FACTION_PICKER = "faction_picker/{system}"
    const val ARMY_BUILDER = "army_builder/{listId}"
    const val SETTINGS = "settings"
    const val COLLECTION = "collection"
    const val WIKI = "wiki"
    const val CARD_DETAIL = "card_detail/{listId}/{entryInstanceId}"
        const val SHARE = "share/{listId}"
    const val TABLETOP = "tabletop/{listId}"
        fun factionPicker(system: GameSystem) = "faction_picker/${system.name}"
        fun armyBuilder(listId: String) = "army_builder/$listId"
        fun cardDetail(listId: String, entryInstanceId: String) = "card_detail/$listId/$entryInstanceId"
        fun share(listId: String) = "share/$listId"
        fun tabletop(listId: String) = "tabletop/$listId"
    }

@Composable
@androidx.compose.material3.ExperimentalMaterial3Api
fun LegionForgeNavHost(navController: NavHostController = rememberNavController()) {
    val vm: ArmyBuilderViewModel = viewModel()
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val shipSizes = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadShipSizes(appContext) }
    val shipImages = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadShipImages(appContext) }
    val shipArcs = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadFiringArcs(appContext) }
    val shipBases = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadShipBases(appContext) }
    val shipFigures = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadArmadaFigures(appContext) }
    val shipTokens = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadArmadaTokens(appContext) }
    val verifiedShipData = androidx.compose.runtime.remember { com.legionforge.app.ui.screens.loadVerifiedArmadaShipData(appContext) }
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            androidx.compose.runtime.LaunchedEffect(Unit) { vm.loadAllCatalog(GameSystem.LEGION_V2) }
            HomeScreen(
                onNewList = { system -> navController.navigate(Routes.factionPicker(system)) },
                onOpenList = { listId -> vm.openList(listId); navController.navigate(Routes.armyBuilder(listId)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onSearch = { navController.navigate(Routes.SEARCH) },
                onWiki = { navController.navigate(Routes.WIKI) },
                vm = vm
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                // La fiche complète est désormais affichée dans la bottom sheet de SearchScreen
                // (image + stats + texte de règle + variantes). Ne pas quitter l'écran ici :
                // l'ancien popBackStack() refermait la recherche sans jamais montrer la carte.
                onCardClick = { /* fiche gérée localement par SearchScreen */ },
                vm = vm
            )
        }
        composable(Routes.WIKI) {
            WikiScreen(
                onBack = { navController.popBackStack() },
                vm = vm
            )
        }
        composable(Routes.FACTION_PICKER, arguments = listOf(navArgument("system") { type = NavType.StringType })) { entry ->
            val system = runCatching { GameSystem.valueOf(entry.arguments?.getString("system") ?: "LEGION_V2") }.getOrDefault(GameSystem.LEGION_V2)
            val defaultName = if (system == GameSystem.LEGION_V2) stringResource(R.string.new_list_title) else stringResource(R.string.new_fleet_title)
            FactionPickerScreen(system, onFactionSelected = { factionId ->
                vm.createList(defaultName, system, factionId, if (system == GameSystem.LEGION_V2) 1000 else 400) { id ->
                    navController.navigate(Routes.armyBuilder(id))
                }
            }, vm = vm)
        }
        composable(Routes.ARMY_BUILDER, arguments = listOf(navArgument("listId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("listId") ?: return@composable
            ArmyBuilderScreen(listId = id, viewModel = vm, onBack = {
                navController.navigate(Routes.HOME) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                    launchSingleTop = true
                }
            }, onPlayCard = { listId, entryId ->
                navController.navigate(Routes.cardDetail(listId, entryId))
            }, onPlayCondensed = { listId ->
                            // Le mode play condensé est désormais fusionné dans la vue plein écran :
                            // le bouton ▶ ouvre CardDetailScreen sur le premier élément jouable.
                            val firstPlayable = vm.entries.value.firstOrNull { it.parentInstanceId == null && (it.card.kind == CardKind.LEGION_UNIT || it.card.kind == CardKind.ARMADA_SHIP) }
                            if (firstPlayable != null) navController.navigate(Routes.cardDetail(listId, firstPlayable.instanceId))
                        }, onShare = {
                            navController.navigate(Routes.share(id))
                        }, onTabletop = {
                            navController.navigate(Routes.tabletop(id))
                        })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onCollection = { navController.navigate(Routes.COLLECTION) }
            )
        }
        composable(Routes.COLLECTION) {
            CollectionScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CARD_DETAIL, arguments = listOf(navArgument("listId") { type = NavType.StringType }, navArgument("entryInstanceId") { type = NavType.StringType })) { entry ->
                    val lid = entry.arguments?.getString("listId") ?: return@composable
                    val eid = entry.arguments?.getString("entryInstanceId") ?: return@composable
                    // Charge la liste dans le VM : sinon vm.currentList est null et la synchro
                    // 2 joueurs ne peut pas envoyer la liste adverse (sendList -> return).
                    androidx.compose.runtime.LaunchedEffect(lid) { vm.openList(lid) }
                    val allEntries = vm.entries.value
                    val idx = allEntries.indexOfFirst { it.instanceId == eid }.coerceAtLeast(0)
                    val selectedFromTabletop by entry.savedStateHandle.getStateFlow<String?>("tabletop_selected_unit", null).collectAsState()
                    CardDetailScreen(entries = allEntries, initialIndex = idx, listId = lid, vm = vm,
                        tabletopSelectedId = selectedFromTabletop,
                        onTabletopSelectionConsumed = { entry.savedStateHandle["tabletop_selected_unit"] = null },
                        onBack = { navController.popBackStack() }, onOpenTabletop = { id -> navController.navigate(Routes.tabletop(id)) })
                }
        composable(Routes.TABLETOP, arguments = listOf(navArgument("listId") { type = NavType.StringType })) { entry ->
            val lid = entry.arguments?.getString("listId") ?: return@composable
            androidx.compose.runtime.LaunchedEffect(lid) { vm.openList(lid) }

            // ── Tabletop Legion : écran dédié, plateau de visualisation au-dessus du Play ──
            if (vm.currentList.value?.gameSystem == com.legionforge.app.data.model.GameSystem.LEGION_V2.name) {
                val legionCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
                val legionTokenMap = remember {
                    try {
                        val txt = legionCtx.assets.open("legion_tokens/token_map.json").bufferedReader().use { it.readText() }
                        val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
                        com.google.gson.Gson().fromJson<Map<String, String>>(txt, type) ?: emptyMap()
                    } catch (_: Exception) { emptyMap<String, String>() }
                }
                val legionEntries = vm.entries.value.filter { it.parentInstanceId == null && it.card.kind == com.legionforge.app.data.model.CardKind.LEGION_UNIT }
                // ── Adversaire 2J (Nearby) ou solo : fusionné dans le plateau ──
                val legionSyncVm: PlaySyncViewModel = viewModel()
                val legionOppJson by legionSyncVm.opponentListJson.collectAsState()
                val legionSoloOpp by vm.soloOppEntries.collectAsState()
                // TOUTES les entrées adverses (unités + upgrades). Le filtre LEGION_UNIT
                // ne s'applique qu'à la construction des pions ; la fiche a besoin des
                // upgrades (children) pour afficher les effets et leurs points.
                val legionOppAll = remember(legionOppJson, vm.cards.value) {
                    if (legionOppJson.isNullOrBlank()) emptyList() else {
                        val opp = try {
                            com.google.gson.Gson().fromJson(legionOppJson, com.legionforge.app.ui.viewmodel.ShareListPayload::class.java)
                        } catch (_: Exception) { null }
                        (opp?.entries ?: emptyList()).mapNotNull { se ->
                            vm.cards.value.firstOrNull { it.id == se.cardId }?.let { card ->
                                ListEntry(
                                    // Ids alignés sur CardDetailScreen:486-488 (B:<uuid> ou opp_<card>_<parent|root>).
                                    instanceId = se.instanceId?.let { "B:$it" } ?: "opp_${se.cardId}_${se.parentInstanceId ?: "root"}",
                                    card = card,
                                    parentInstanceId = se.parentInstanceId?.let { "B:$it" },
                                    quantity = se.quantity,
                                )
                            }
                        }
                    }
                }
                val legionOppEntries = legionOppAll.filter { it.card.kind == com.legionforge.app.data.model.CardKind.LEGION_UNIT }
                val legionSoloEntries = legionSoloOpp.filter { it.card.kind == com.legionforge.app.data.model.CardKind.LEGION_UNIT && it.parentInstanceId == null }
                val allLegionEntries = legionEntries + legionOppEntries.ifEmpty { legionSoloEntries }
                val legionPawns = allLegionEntries.mapIndexed { i, e ->
                    val st = com.legionforge.app.ui.screens.LegionStatsParser.parse(e.card.legionStats)
                    val isOpp = e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_")
                    // Socle : règle officielle rulebook:298-303 via cardSubtype.
                    // Seuls trooper/clone trooper/droid trooper/wookiee trooper sont
                    // sur petits socles ; TOUT le reste est cranté (mandalorien,
                    // emplacement, créature, véhicule...).
                    val baseSize = com.legionforge.app.domain.LegionTabletopGeometry.baseSizeFor(
                        e.card.cardSubtype, e.card.name, st?.miniCount ?: 1)
                    com.legionforge.app.ui.screens.LegionPawn(
                        id = e.instanceId,
                        name = e.card.name,
                        xMm = (i - (allLegionEntries.size - 1) / 2f) * 30f,
                        yMm = if (isOpp) -120f else 120f,
                        headingDeg = if (isOpp) 180f else 0f,
                        baseSize = baseSize,
                        maxWounds = com.legionforge.app.domain.LegionTabletopGeometry.totalWounds(st?.health ?: 1, st?.miniCount ?: 1),
                        wounds = com.legionforge.app.domain.LegionTabletopGeometry.totalWounds(st?.health ?: 1, st?.miniCount ?: 1),
                        speed = st?.speed ?: 1,
                        isOpp = isOpp,
                        canRotate = baseSize != "small",
                        imagePath = legionTokenMap[e.card.id]?.let { "legion_tokens/$it" } ?: e.card.imagePath(),
                        faction = e.card.factionId,
                        miniCount = st?.miniCount ?: 1,
                    )
                }
                // État de plateau local : positions/round/couvert persistés dans le
                // snapshot de session, pour ne pas "sauter" en quittant l'écran.
                val boardDao = com.legionforge.app.data.local.LegionForgeDatabase.getInstance(legionCtx).gameSessionDao()
                var legionState by remember(lid) { mutableStateOf(legionPawns) }
                var legionRound by remember(lid) { mutableIntStateOf(1) }
                var terrainCard by remember(lid) { mutableStateOf<com.legionforge.app.ui.screens.TerrainCard?>(null) }
                var showTerrainPicker by remember(lid) { mutableStateOf(false) }
                // Tapis PAR DÉFAUT : Tatooine (nom visible en haut, changeable).
                var battleMat by remember(lid) { mutableStateOf(com.legionforge.app.ui.screens.BattleMats.TATOOINE) }
                var showBattleMatPicker by remember(lid) { mutableStateOf(false) }
                // Cartes de mission du bandeau (vraies images) + sélection par emplacement.
                val battleCards = remember { com.legionforge.app.ui.screens.loadBattleCards(legionCtx) }
                val battleBitmaps = remember(battleCards) {
                    battleCards.mapNotNull { c -> c.imagePath()?.let { p -> p to com.legionforge.app.ui.screens.loadLegionAssetBitmap(legionCtx, p) } }
                        .filter { it.second != null }.associate { it.first to it.second!! }
                }
                var missionSlot by remember(lid) { mutableStateOf<String?>(null) }
                var missionSel by remember(lid) { mutableStateOf<Map<String, String>>(emptyMap()) }
                // Keyer sur les IDS des pions, pas sur legionPawns (objets recalculés à chaque
                // recomposition) : sinon le chargement écrase les éditions wounds/cover faites
                // depuis la fiche. On ne restaure que x/y/heading ; l'état courant est préservé.
                val legionPawnIds = legionPawns.joinToString(",") { it.id }
                LaunchedEffect(lid, legionPawnIds) {
                    val saved = runCatching { boardDao.getByList(lid)?.snapshot?.let { com.legionforge.app.data.model.PlaySessionSnapshot.fromJson(it) } }.getOrNull()
                    val pos = saved?.tabletopUnits ?: emptyMap()
                    val currentById = legionState.associateBy { it.id }
                    legionState = legionPawns.map { pawn ->
                        val cur = currentById[pawn.id]
                        val savedPos = pos[pawn.id]
                        when {
                            cur != null && savedPos != null -> cur.copy(xMm = savedPos.x, yMm = savedPos.y, headingDeg = savedPos.heading)
                            cur != null -> cur
                            savedPos != null -> pawn.copy(xMm = savedPos.x, yMm = savedPos.y, headingDeg = savedPos.heading)
                            else -> pawn
                        }
                    }
                    legionRound = saved?.round ?: 1
                    missionSel = saved?.tabletopMissions ?: emptyMap()
                }
                // Sauvegarde à chaque changement d'état du plateau.
                LaunchedEffect(legionState, legionRound) {
                    if (legionState.isEmpty()) return@LaunchedEffect
                    val existing = runCatching { boardDao.getByList(lid)?.snapshot?.let { com.legionforge.app.data.model.PlaySessionSnapshot.fromJson(it) } }.getOrNull()
                    val positions = legionState.associate { p ->
                        p.id to com.legionforge.app.data.model.TabletopUnitPos(p.xMm, p.yMm, p.headingDeg)
                    }
                    val snap = if (existing != null) existing.copy(
                        listId = lid, round = legionRound, tabletopUnits = positions, tabletopMissions = missionSel,
                    ) else com.legionforge.app.data.model.PlaySessionSnapshot(
                        listId = lid, gameSystem = "LEGION_V2", round = legionRound, tabletopUnits = positions, tabletopMissions = missionSel,
                    )
                    runCatching {
                        boardDao.upsert(com.legionforge.app.data.model.GameSessionEntity(lid, "LEGION_V2", snap.toJson(), System.currentTimeMillis()))
                    }
                }
                // Résoudre les cartes sélectionnées par nom -> objet, avec défaut par catégorie.
                val missionLabelsForRail = listOf("primary", "map", "secondary")
                fun catFor(slot: String) = when (slot) {
                    "primary" -> "objective"
                    "map" -> "map"
                    "secondary" -> "secondary"
                    else -> "objective"
                }
                val selectedMissionCards = missionLabelsForRail.associateWith { slot ->
                    val name = missionSel[slot]
                    val cat = catFor(slot)
                    battleCards.firstOrNull { it.name == name && it.category == cat }
                        ?: battleCards.firstOrNull { it.category == cat }
                }
                var detailPawnId by remember(lid) { mutableStateOf<String?>(null) }
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                com.legionforge.app.ui.screens.LegionTabletopScreen(
                    pawns = legionState,
                    round = legionRound,
                    onBack = { navController.popBackStack() },
                    onOpenPlay = { id ->
                        // Ouvre la VRAIE fiche dépliante (UnitDetailSheet) en overlay DIRECT
                        // au-dessus du plateau : pas de navigation, le plateau reste visible,
                        // le retour = dismiss de la sheet. Jamais popBackStack ici.
                        detailPawnId = id
                    },
                    onMove = { id, x, y, h -> legionState = legionState.map { if (it.id == id) it.copy(xMm = x, yMm = y, headingDeg = h) else it } },
                    onActivate = { id -> legionState = legionState.map { if (it.id == id) it.copy(activated = !it.activated) else it } },
                    onCoverChange = { id, c -> legionState = legionState.map { if (it.id == id) it.copy(cover = c.coerceIn(0, 2)) else it } },
                    onRoundChange = { legionRound = it },
                    onWoundsChange = { id, w ->
                        legionState = legionState.map { p ->
                            if (p.id == id) {
                                val (nw, dead) = com.legionforge.app.domain.LegionTabletopGeometry.applyWounds(p.wounds, p.maxWounds, w)
                                p.copy(wounds = nw, defeated = dead)
                            } else p
                        }
                    },
                    terrainCard = terrainCard,
                    onPickTerrain = { showTerrainPicker = true },
                    battleMat = battleMat,
                    battleMatName = androidx.compose.ui.res.stringResource(
                        when (battleMat.id) {
                            "battlemat_tatooine" -> com.legionforge.app.R.string.battlemat_tatooine
                            "battlemat_hoth" -> com.legionforge.app.R.string.battlemat_hoth
                            "battlemat_endor" -> com.legionforge.app.R.string.battlemat_endor
                            "battlemat_scarif" -> com.legionforge.app.R.string.battlemat_scarif
                            "battlemat_naboo" -> com.legionforge.app.R.string.battlemat_naboo
                            "battlemat_geonosis" -> com.legionforge.app.R.string.battlemat_geonosis
                            else -> com.legionforge.app.R.string.battlemat_none
                        }
                    ),
                    onPickBattleMat = { showBattleMatPicker = true },
                    missionCards = selectedMissionCards,
                    missionBitmaps = battleBitmaps,
                    onPickMissionCard = { slot -> missionSlot = slot },
                )
                if (showTerrainPicker) {
                    com.legionforge.app.ui.screens.TerrainPickerSheet(
                        current = terrainCard,
                        cards = com.legionforge.app.ui.screens.TerrainCards.ALL.map { card ->
                            card to androidx.compose.ui.res.stringResource(
                                when (card.id) {
                                    "monument" -> com.legionforge.app.R.string.terrain_monument
                                    "ruins" -> com.legionforge.app.R.string.terrain_ruins
                                    "endor" -> com.legionforge.app.R.string.terrain_endor
                                    "tatooine" -> com.legionforge.app.R.string.terrain_tatooine
                                    "naboo" -> com.legionforge.app.R.string.terrain_naboo
                                    "geonosis" -> com.legionforge.app.R.string.terrain_geonosis
                                    else -> com.legionforge.app.R.string.terrain_none
                                }
                            )
                        },
                        onPick = { terrainCard = it; showTerrainPicker = false },
                        onDismiss = { showTerrainPicker = false },
                        nameNone = androidx.compose.ui.res.stringResource(com.legionforge.app.R.string.terrain_none),
                        title = androidx.compose.ui.res.stringResource(com.legionforge.app.R.string.terrain_title),
                        coverHint = androidx.compose.ui.res.stringResource(com.legionforge.app.R.string.terrain_cover),
                    )
                }
                if (showBattleMatPicker) {
                    com.legionforge.app.ui.screens.BattleMatPickerSheet(
                        current = battleMat,
                        onPick = { battleMat = it; showBattleMatPicker = false },
                        onDismiss = { showBattleMatPicker = false },
                    )
                }
                if (missionSlot != null) {
                    val slot = missionSlot!!
                    com.legionforge.app.ui.screens.MissionCardPickerSheet(
                        title = when (slot) {
                            "primary" -> androidx.compose.ui.res.stringResource(com.legionforge.app.R.string.rail_primary)
                            "map" -> androidx.compose.ui.res.stringResource(com.legionforge.app.R.string.rail_map)
                            else -> androidx.compose.ui.res.stringResource(com.legionforge.app.R.string.rail_secondary)
                        },
                        cards = battleCards.filter { it.category == catFor(slot) },
                        current = selectedMissionCards[slot],
                        onPick = { card -> missionSel = missionSel + (slot to card.name); missionSlot = null },
                        onDismiss = { missionSlot = null },
                    )
                }
                // ── VRAIE fiche dépliante du mode Play, en overlay sur le plateau ──
                // Même UnitDetailSheet que CardDetailScreen:1529 : stats, règles, jetons,
                // capacités. Plateau visible derrière, retour = dismiss (jamais de navigation).
                if (detailPawnId != null) {
                    val did = detailPawnId!!
                    // Toutes les cartes (unités ET upgrades) pour que la fiche affiche
                    // les enfants et leurs effets, exactement comme le mode Play.
                    val allForDetail = vm.entries.value + legionOppAll + legionSoloOpp
                    val unit = allForDetail.firstOrNull { it.instanceId == did }
                    val pawn = legionState.firstOrNull { it.id == did }
                    if (unit != null && pawn != null) {
                        val isOppDetail = did.startsWith("B:") || did.startsWith("opp_")
                        val wikiSections by vm.wikiSections.collectAsState()
                        val detailWiki = remember(unit, wikiSections) { wikiSections.filter { it.gameSystem == unit.card.gameSystem.name } }
                        val children = allForDetail.filter { it.parentInstanceId == unit.instanceId }
                        val syncStateMap = if (isOppDetail) legionSyncVm.oppStates.collectAsState().value else legionSyncVm.ownStates.collectAsState().value
                        val base = syncStateMap[did] ?: com.legionforge.app.data.nearby.SyncedUnitState(did)
                        // defeated doit être dérivé des PV (applyWounds) : 0 PV => vaincu,
                        // PV>0 => non vaincu. Sinon la fiche et le plateau divergent.
                        val (shownWounds, shownDead) = com.legionforge.app.domain.LegionTabletopGeometry.applyWounds(
                            pawn.wounds, pawn.maxWounds, pawn.wounds)
                        val detailState = base.copy(
                            wounds = shownWounds, cover = pawn.cover,
                            defeated = shownDead, activated = pawn.activated,
                        )
                        val connected = legionSyncVm.mode.collectAsState().value == com.legionforge.app.data.nearby.ShareMode.CONNECTED
                        val readOnlyDetail = connected && isOppDetail
                        val onChangeDetail: (com.legionforge.app.data.nearby.SyncedUnitState) -> Unit =
                            if (readOnlyDetail) { _ -> } else { next ->
                                if (!isOppDetail) legionSyncVm.setOwnState(did, next)
                                // Mirroring vers le plateau : blessures/couvert/états uniquement,
                                // jamais x/y/heading (gérés par le drag du plateau).
                                legionState = legionState.map { p ->
                                    if (p.id != did) p else {
                                        val (nw, dead) = com.legionforge.app.domain.LegionTabletopGeometry.applyWounds(
                                            p.wounds, p.maxWounds, next.wounds ?: p.wounds)
                                        p.copy(wounds = nw, cover = (next.cover ?: p.cover).coerceIn(0, 2),
                                            defeated = if (next.wounds != null) dead else (next.defeated ?: dead),
                                            activated = next.activated ?: p.activated)
                                    }
                                }
                            }
                        com.legionforge.app.ui.screens.UnitDetailSheet(
                            unit, children, allForDetail, detailWiki, {},
                            detailState, onChangeDetail, readOnlyDetail,
                            onDice = {}, onCompare = {}, onClose = { detailPawnId = null },
                            showDice = false, showCompare = false,
                        )
                    }
                }
                }
                return@composable
            }

            val ships = vm.entries.value.filter { it.parentInstanceId == null && (it.card.kind.name == "ARMADA_SHIP" || it.card.kind.name == "ARMADA_SQUADRON") }
            val playSyncVmTabletop: PlaySyncViewModel = viewModel()
            val appCtxT = androidx.compose.ui.platform.LocalContext.current.applicationContext
            androidx.compose.runtime.LaunchedEffect(lid) {
                if (vm.soloOppList.value == null) {
                    val saved = runCatching {
                        val ent = com.legionforge.app.data.local.LegionForgeDatabase.getInstance(appCtxT).gameSessionDao().getByList(lid)
                        ent?.snapshot?.let { com.legionforge.app.data.model.PlaySessionSnapshot.fromJson(it) }
                    }.getOrNull()
                    if (saved?.twoPlayer == false) saved.soloOppListId?.let(vm::loadOpponentList)
                }
            }
            val tabletopSetup by androidx.compose.runtime.produceState(initialValue = true, lid) {
                value = try {
                    val ent = com.legionforge.app.data.local.LegionForgeDatabase.getInstance(appCtxT).gameSessionDao().getByList(lid)
                    ent == null || com.legionforge.app.data.model.PlaySessionSnapshot.fromJson(ent.snapshot).phase != "GAME"
                } catch (_: Exception) { true }
            }
            val tabletopRound by androidx.compose.runtime.produceState(initialValue = 1, lid) {
                value = try {
                    val ent = com.legionforge.app.data.local.LegionForgeDatabase.getInstance(appCtxT).gameSessionDao().getByList(lid)
                    com.legionforge.app.data.model.PlaySessionSnapshot.fromJson(ent?.snapshot ?: "")?.armadaRound ?: 1
                } catch (_: Exception) { 1 }
            }
            val tabletopPhase by androidx.compose.runtime.produceState(initialValue = "COMMAND", lid) {
                value = try {
                    val ent = com.legionforge.app.data.local.LegionForgeDatabase.getInstance(appCtxT).gameSessionDao().getByList(lid)
                    (com.legionforge.app.data.model.PlaySessionSnapshot.fromJson(ent?.snapshot ?: "")?.armadaPhase ?: "command").uppercase()
                } catch (_: Exception) { "COMMAND" }
            }
            val oppListJson by playSyncVmTabletop.opponentListJson.collectAsState()
            val oppShipsEntries = remember(oppListJson, vm.cards.value) {
                val opp = try { com.google.gson.Gson().fromJson(oppListJson, com.legionforge.app.ui.viewmodel.ShareListPayload::class.java) } catch (_: Exception) { null }
                (opp?.entries ?: emptyList()).mapNotNull { e ->
                    vm.cards.value.firstOrNull { it.id == e.cardId }?.let { card ->
                        ListEntry(
                            instanceId = e.instanceId?.let { "B:$it" } ?: "opp_${e.cardId}_${e.parentInstanceId ?: "root"}",
                            card = card,
                            parentInstanceId = e.parentInstanceId?.let { "B:$it" },
                            quantity = e.quantity,
                            chosenSlot = e.chosenSlot?.let { s -> runCatching { ArmadaSlot.valueOf(s) }.getOrNull() }
                        )
                    }
                }.filter { it.card.kind.name == "ARMADA_SHIP" || it.card.kind.name == "ARMADA_SQUADRON" }
            }
            // En local, la flotte adverse sélectionnée dans Play doit apparaître aussi sur le plateau.
            val soloOppEntries by vm.soloOppEntries.collectAsState()
            val tabletopOpp = if (oppListJson.isNullOrBlank()) soloOppEntries else oppShipsEntries
            val allShips = ships + tabletopOpp.filter { it.parentInstanceId == null && (it.card.kind == CardKind.ARMADA_SHIP || it.card.kind == CardKind.ARMADA_SQUADRON) }
            val sizes = shipSizes
            val pawns = allShips.mapIndexed { i, e ->
                com.legionforge.app.ui.screens.TabletopPawn(
                    id = e.instanceId,
                    name = e.card.name,
                    sizeClass = if (e.card.kind == CardKind.ARMADA_SQUADRON) "squadron" else verifiedShipData[e.card.id]?.sizeClass ?: sizes[e.card.id] ?: "unknown",
                    imagePath = shipImages[e.card.id],
                    tokenPath = shipTokens[e.card.id],
                    figurePath = shipFigures[e.card.id]?.asset,
                    figureLengthMm = shipFigures[e.card.id]?.lengthMm ?: 0f,
                    figureKind = shipFigures[e.card.id]?.figureKind ?: "ship",
                    figureCount = shipFigures[e.card.id]?.figureCount ?: 1,
                    arcs = shipArcs[e.card.id],
                    attack = com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, com.legionforge.app.data.model.CardKind.ARMADA_SHIP)?.let { st ->
                        fun toBag(l: List<Int>): com.legionforge.app.ui.screens.ArmadaDiceBag {
                            fun n(i: Int) = l.getOrElse(i) { 0 }
                            // ArmadaStats.attack* = [rouge, bleu, noir]
                            return com.legionforge.app.ui.screens.ArmadaDiceBag(red = n(0), blue = n(1), black = n(2))
                        }
                        com.legionforge.app.ui.screens.ArmadaAttack(
                            front = toBag(st.attackFront),
                            right = toBag(st.attackStarboard),
                            left = toBag(st.attackPort),
                            rear = toBag(st.attackRear),
                        )
                    },
                    antiSquadron = if (e.card.kind == CardKind.ARMADA_SHIP) verifiedShipData[e.card.id]?.antiSquadron else
                        com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, e.card.kind)?.antiSquadron?.takeIf { it.isNotEmpty() }?.let { d ->
                            val v = com.legionforge.app.domain.ArmadaAttackMapping.squadronDice(d)
                            com.legionforge.app.ui.screens.ArmadaDiceBag(black = v.black, blue = v.blue, red = v.red)
                        },
                    squadronBattery = com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, e.card.kind)?.battery?.takeIf { it.isNotEmpty() }?.let { d ->
                        val v = com.legionforge.app.domain.ArmadaAttackMapping.squadronDice(d)
                        com.legionforge.app.ui.screens.ArmadaDiceBag(black = v.black, blue = v.blue, red = v.red)
                    },
                    snipe = com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, com.legionforge.app.data.model.CardKind.ARMADA_SQUADRON)
                        ?.keywords?.any { it.equals("Snipe", ignoreCase = true) } == true ||
                        (e.card.rulesText ?: "").contains("Snipe", ignoreCase = true),
                    basePath = shipBases[e.card.id],
                    hull = com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, com.legionforge.app.data.model.CardKind.ARMADA_SHIP)?.hull ?: 0,
                    shieldFront = com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, com.legionforge.app.data.model.CardKind.ARMADA_SHIP)?.shieldFront ?: 0,
                    maxSpeed = if (e.card.kind == CardKind.ARMADA_SQUADRON)
                        (com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, CardKind.ARMADA_SQUADRON)?.speed ?: 0)
                    else (com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, CardKind.ARMADA_SHIP)?.maxSpeed ?: 0),
                    speedChart = (com.legionforge.app.ui.screens.ArmadaStatsParser.parse(e.card.shipStats, com.legionforge.app.data.model.CardKind.ARMADA_SHIP)?.speedChart ?: emptyList()).map { pos ->
                        pos.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap()
                    },
                    xMm = (run { val mine = allShips.count { !(it.instanceId.startsWith("B:") || it.instanceId.startsWith("opp_")) }; val sameSide = allShips.take(i).count { (it.instanceId.startsWith("B:") || it.instanceId.startsWith("opp_")) == (e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_")) }; val isOppE = e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_"); val n = if (isOppE) allShips.size - mine else mine; (sameSide - n / 2f) * 160f }),
                    isOpp = e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_"),
                    headingDeg = if (e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_")) 180f else 0f,
                    isSquadron = e.card.kind.name == "ARMADA_SQUADRON",
                    // Placé au centre de la zone de déploiement (distance 1..3 depuis le bord long = bord du joueur).
                    // BOARD_H_MM=914.4, halfH=457.2, DEPLOY_MIN=76.2, DEPLOY_MAX=228.6 → milieu = 304.8 mm.
                    yMm = if (e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_")) -304.8f else 304.8f,
                )
            }
            // Tapis spatial Armada PAR DÉFAUT : Scarif (nom visible, changeable).
            var armadaBattleMat by androidx.compose.runtime.remember(lid) {
                androidx.compose.runtime.mutableStateOf(com.legionforge.app.ui.screens.ArmadaBattleMats.SCARIF)
            }
            var showArmadaMatPicker by androidx.compose.runtime.remember(lid) {
                androidx.compose.runtime.mutableStateOf(false)
            }
            com.legionforge.app.ui.screens.TabletopScreen(
                pawns = pawns,
                sessionId = lid,
                onSelectPlayUnit = { unitId ->
                    // Empile la fiche au-dessus du plateau : le retour ramène au tabletop.
                    navController.navigate(Routes.cardDetail(lid, unitId))
                    navController.currentBackStackEntry?.savedStateHandle?.set("tabletop_selected_unit", unitId)
                },
                round = tabletopRound,
                phase = tabletopPhase,
                deployment = tabletopSetup,
                onBack = { navController.popBackStack() },
                onOpenPlay = { instanceId -> navController.navigate(Routes.cardDetail(lid, instanceId)) },
                battleMat = armadaBattleMat,
                battleMatName = androidx.compose.ui.res.stringResource(
                    when (armadaBattleMat.id) {
                        "armada_scarif" -> com.legionforge.app.R.string.armada_battlemat_scarif
                        "armada_hoth" -> com.legionforge.app.R.string.armada_battlemat_hoth
                        "armada_kaliida" -> com.legionforge.app.R.string.armada_battlemat_kaliida
                        "armada_coruscant" -> com.legionforge.app.R.string.armada_battlemat_coruscant
                        else -> com.legionforge.app.R.string.armada_battlemat_none
                    }
                ),
                onPickBattleMat = { showArmadaMatPicker = true },
            )
            if (showArmadaMatPicker) {
                com.legionforge.app.ui.screens.ArmadaBattleMatPickerSheet(
                    current = armadaBattleMat,
                    onPick = { armadaBattleMat = it; showArmadaMatPicker = false },
                    onDismiss = { showArmadaMatPicker = false },
                )
            }
        }
        composable(Routes.SHARE, arguments = listOf(navArgument("listId") { type = NavType.StringType })) { entry ->
            val lid = entry.arguments?.getString("listId") ?: return@composable
            val list = vm.currentList.value
            ShareScreen(
                list = list,
                entries = vm.entries.value,
                onImport = { payload ->
                    vm.importSharedList(payload) { newId ->
                        navController.navigate(Routes.armyBuilder(newId)) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = false }
                            launchSingleTop = true
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
