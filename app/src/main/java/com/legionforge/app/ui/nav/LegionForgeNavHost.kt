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
                val legionPawns = legionEntries.mapIndexed { i, e ->
                    val st = com.legionforge.app.ui.screens.LegionStatsParser.parse(e.card.legionStats)
                    val isOpp = e.instanceId.startsWith("B:") || e.instanceId.startsWith("opp_")
                    // Taille de socle par TYPE d'unité (rulebook:298-303) : seuls les
                    // véhicules/créatures/emplacements ont des socles crantés ; les
                    // troopers (même commandants) sont sur petits socles ronds.
                    val kw = st?.keywords.orEmpty().map { it.lowercase() }
                    val notched = kw.any { k -> k.contains("vehicle") || k.contains("creature") || k.contains("emplacement") || k.contains("heavy trooper") }
                    val baseSize = if (notched) "large" else "small"
                    com.legionforge.app.ui.screens.LegionPawn(
                        id = e.instanceId,
                        name = e.card.name,
                        xMm = ((legionEntries.size - 1) - i * 1f) * 30f - (legionEntries.size - 1) * 30f / 2f,
                        yMm = if (isOpp) -120f else 120f,
                        headingDeg = if (isOpp) 180f else 0f,
                        baseSize = baseSize,
                        maxWounds = com.legionforge.app.domain.LegionTabletopGeometry.totalWounds(st?.health ?: 1, st?.miniCount ?: 1),
                        wounds = com.legionforge.app.domain.LegionTabletopGeometry.totalWounds(st?.health ?: 1, st?.miniCount ?: 1),
                        speed = st?.speed ?: 1,
                        isOpp = isOpp,
                        canRotate = baseSize != "small",
                        imagePath = legionTokenMap[e.card.id]?.let { "legion_tokens/$it" } ?: e.card.imagePath(),
                    )
                }
                // État de plateau local : les interactions ne sont plus des no-op.
                // (La synchronisation 2J complète via PlaySyncViewModel reste une dette documentée.)
                var legionState by remember(lid) { mutableStateOf(legionPawns) }
                var legionRound by remember(lid) { mutableIntStateOf(1) }
                LaunchedEffect(legionPawns) { legionState = legionPawns }
                com.legionforge.app.ui.screens.LegionTabletopScreen(
                    pawns = legionState,
                    round = legionRound,
                    onBack = { navController.popBackStack() },
                    onOpenPlay = { id -> navController.navigate(Routes.cardDetail(lid, id)) },
                    onMove = { id, x, y, h -> legionState = legionState.map { if (it.id == id) it.copy(xMm = x, yMm = y, headingDeg = h) else it } },
                    onActivate = { id -> legionState = legionState.map { if (it.id == id) it.copy(activated = !it.activated) else it } },
                    onCoverChange = { id, c -> legionState = legionState.map { if (it.id == id) it.copy(cover = c.coerceIn(0, 2)) else it } },
                    onRoundChange = { legionRound = it },
                )
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
            com.legionforge.app.ui.screens.TabletopScreen(
                pawns = pawns,
                sessionId = lid,
                onSelectPlayUnit = { unitId ->
                    val playEntry = navController.previousBackStackEntry?.takeIf { it.destination.route == Routes.CARD_DETAIL }
                    if (playEntry != null) {
                        playEntry.savedStateHandle["tabletop_selected_unit"] = unitId
                        navController.popBackStack()
                    } else {
                        // Déposer la sélection sur la destination créée, même si c'est un pion adverse.
                        navController.navigate(Routes.cardDetail(lid, unitId))
                        navController.currentBackStackEntry?.savedStateHandle?.set("tabletop_selected_unit", unitId)
                    }
                },
                round = tabletopRound,
                phase = tabletopPhase,
                deployment = tabletopSetup,
                onBack = { navController.popBackStack() },
                onOpenPlay = { instanceId -> navController.navigate(Routes.cardDetail(lid, instanceId)) },
            )
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
